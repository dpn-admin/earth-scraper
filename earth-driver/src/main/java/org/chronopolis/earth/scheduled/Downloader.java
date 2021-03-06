package org.chronopolis.earth.scheduled;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.chronopolis.earth.EarthSettings;
import org.chronopolis.earth.SimpleCallback;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.Remote;
import org.chronopolis.earth.config.Endpoint;
import org.chronopolis.earth.config.Ingest;
import org.chronopolis.earth.domain.ReplicationFlow;
import org.chronopolis.earth.domain.RsyncDetail;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.chronopolis.earth.util.DetailEmitter;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.IngestRequest;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrofit2.Call;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TODO: Find a better way to query each api/grab replication transfers
 * The current has mucho code duplication
 *
 * TODO: We can add some bookkeeping/stats via ReplicationFlow (maybe RsyncStats/TarStats/HttpStats)
 * Created by shake on 3/31/15.
 */
@Component
@EnableScheduling
public class Downloader {
    private final Logger log = LoggerFactory.getLogger(Downloader.class);
    private static final String TAG_MANIFEST = "tagmanifest-sha256.txt";
    private static final String MANIFEST = "manifest-sha256.txt";

    private IngestAPI chronopolis;
    private EarthSettings settings;
    private final List<Remote> remotes;
    private SessionFactory sessionFactory;

    @Autowired
    public Downloader(EarthSettings settings, IngestAPI chronopolis, List<Remote> remotes, SessionFactory factory) {
        this.chronopolis = chronopolis;
        this.settings = settings;
        this.remotes = remotes;
        this.sessionFactory = factory;
    }

    private Response<Replication> getTransfers(BalustradeTransfers balustrade,
                                               Map<String, String> params) {
        SimpleCallback<Response<Replication>> callback = new SimpleCallback<>();
        params.put("to_node", settings.getName());
        params.put("cancelled", String.valueOf(false));
        Call<Response<Replication>> call = balustrade.getReplications(params);
        call.enqueue(callback);
        Optional<Response<Replication>> response = callback.getResponse();

        // get the actual response OR an empty response (in the event of failure)
        Response<Replication> transfers = response.orElse(emptyResponse());
        log.trace("Count: {}\nNext: {}\nPrevious: {}",
                transfers.getCount(),
                transfers.getNext(),
                transfers.getPrevious());
        return transfers;
    }

    /**
     * Create an empty response object
     *
     * @return a response with no result list
     */
    private Response<Replication> emptyResponse() {
        Response<Replication> response = new Response<>();
        response.setResults(Lists.newArrayList());
        return response;
    }

    // Scheduled tasks. Delegate to functions based on what state the replication is in

    /**
     * The beginning of a replication sequence. Here we have store_requested = false and
     * need to do one of two tasks: rsync or untar. When we extract the tarball, we
     * calculate the fixity value and update the replication.
     */
    @Scheduled(cron = "${earth.cron.replicate:0 * * * * *}")
    protected void requested() {
        int page;
        int pageSize = 10;

        Response<Replication> transfers;
        Map<String, String> params = Maps.newHashMap();
        params.put("store_requested", String.valueOf(false));
        params.put("page_size", String.valueOf(pageSize));

        for (Remote remote : remotes) {
            Endpoint endpoint = remote.getEndpoint();
            page = 1;
            String node = endpoint.getName();
            BalustradeTransfers api = remote.getTransfers();

            // Open for all?
            // TODO: Put flows in a list and flush accordingly?
            Session session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            // log.info("[{}] Getting {} replications", node, status.getName());
            do {
                params.put("from_node", node);
                params.put("page", String.valueOf(page));
                transfers = getTransfers(api, params);
                for (Replication transfer : transfers.getResults()) {
                    ReplicationFlow flow = init(session, transfer);
                    String from = transfer.getFromNode();
                    String uuid = transfer.getBag();
                    try {
                        if (flow.isReceived()) {
                            log.info("Updating replication {}", transfer.getReplicationId());
                            update(api, transfer, flow);
                        } else {
                            download(transfer, flow);
                        }
                    } catch (IOException e) {
                        log.error("[{}] Error downloading {}, skipping", from, uuid, e);
                    } finally {
                        session.saveOrUpdate(flow);
                    }
                }

                ++page;
                if (tx.isActive()) {
                    tx.commit();
                }
            } while (transfers.getNext() != null);
            session.close();
        }

    }

    private ReplicationFlow init(Session session, Replication transfer) {
        ReplicationFlow flow = session.get(ReplicationFlow.class, transfer.getReplicationId());
        if (flow == null) {
            log.info("Creating new replication flow for {}", transfer.getReplicationId());
            flow = new ReplicationFlow();
            flow.setId(transfer.getReplicationId());
            flow.setNode(transfer.getFromNode());
        }

        return flow;
    }

    /**
     * The middle/end of the replication. At this point we have a valid fixity, but still
     * need to validate the bag. If a bag is not valid, we cancel it, else we push it in
     * to chronopolis. Once a bag has been replicated in chronopolis we set stored to true
     * and update the replication.
     * <p>
     */
    @Scheduled(cron = "${earth.cron.replicate:0 * * * * *}")
    protected void received() {
        int page;
        int pageSize = 10;

        Response<Replication> transfers;
        Map<String, String> params = Maps.newHashMap();
        params.put("store_requested", String.valueOf(true));
        params.put("stored", String.valueOf(false));
        params.put("page_size", String.valueOf(pageSize));

        for (Remote remote : remotes) {
            Endpoint endpoint = remote.getEndpoint();
            page = 1;
            String node = endpoint.getName();
            BalustradeTransfers api = remote.getTransfers();
            // log.info("[{}] Getting {} replications", node, status.getName());

            Session session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            do {
                params.put("from_node", node);
                params.put("page", String.valueOf(page));
                transfers = getTransfers(api, params);

                for (Replication transfer : transfers.getResults()) {
                    String from = transfer.getFromNode();
                    String uuid = transfer.getBag();
                    ReplicationFlow flow = init(session, transfer);

                    try {

                        // If we haven't yet extracted, do so
                        // else validate
                        // else push to chronopolis
                        // else check if we should store
                        // todo: see if there's a way to cut down on this
                        if (flow.isPushed() && flow.isValidated() && flow.isExtracted()) {
                            store(api, transfer, flow);
                        } else if (flow.isValidated() && flow.isExtracted()) {
                            push(transfer, flow);
                        } else if (flow.isExtracted()) {
                            validate(api, transfer, flow);
                        } else {
                            untar(transfer, flow);
                        }
                    } catch (IOException e) {
                        log.error("[{}] Error untarring {}, skipping", from, uuid, e);
                    } finally {
                        session.saveOrUpdate(flow);
                    }
                }

                ++page;
                if (tx.isActive()) {
                    tx.commit();
                }
            } while (transfers.getNext() != null);

            session.close();
        }
    }

    // Operations

    /**
     * Update a replication to note completion of ingestion into chronopolis
     * @param api      The transfer API to use
     * @param transfer The replication transfer to update
     * @param flow     The replication flow to update
     */
    @VisibleForTesting
    @SuppressWarnings("WeakerAccess")
    public void store(BalustradeTransfers api, Replication transfer, ReplicationFlow flow) {
        // First check if we have the bag stored in chronopolis
        String uuid = transfer.getBag();

        Map<String, Object> chronParams = Maps.newHashMap();
        chronParams.put("name", uuid);

        // TODO: Query parameters for bag
        // Since bags are named by uuids, this should be unique
        // but it's still a list so we get that and check if it's empty
        List<Bag> bags;
        Call<PageImpl<Bag>> bagCall = chronopolis.getBags(chronParams);
        try {
            bags = bagCall.execute() // execute the http request
                    .body()          // get the response body
                    .getContent();   // get the content of the response
        } catch (IOException e) {
            log.error("Error getting list of bags from a DPN Registry", e);
            bags = new ArrayList<>();
        }

        // Then update  dpn if we're ready
        // TODO: Rework this a little bit so it's a little cleaner
        if (!bags.isEmpty()) {
            Bag b = bags.get(0);
            BagStatus status = b.getStatus();
            log.info("Bag found in chronopolis, status is {}", status);
            if (status == BagStatus.PRESERVED) {
                transfer.setStored(true);
                transfer.setUpdatedAt(ZonedDateTime.now());
                Call<Replication> call = api.updateReplication(transfer.getReplicationId(), transfer);
                handleCall(call, flow);
            }
        }
    }



    /**
     * Notify our Chronopolis ingest server that a bag can be replicated
     * in to Chronopolis
     *
     * @param transfer The replication transfer being ingested into Chronopolis
     * @param flow     The flow object corresponding to the replication
     */
    public void push(Replication transfer, ReplicationFlow flow) {
        log.info("Bag is valid, pushing to chronopolis");
        Ingest ingest = settings.getIngest();

        // push to chronopolis
        IngestRequest request = new IngestRequest();
        request.setDepositor(transfer.getFromNode());
        request.setName(transfer.getBag());
        request.setLocation(transfer.getFromNode() + "/" + transfer.getBag());
        request.setRequiredReplications(1);
        request.setReplicatingNodes(ingest.getReplicateTo());

        // We only need to check for the presence of the response, which should
        // indicate a successful http call
        DetailEmitter<Bag> cb;
        Call<Bag> call = chronopolis.stageBag(request);
        cb = handleCall(call, flow);

        if (cb.getResponse().isPresent()) {
            flow.setPushed(true);
        }
    }

    /**
     * Validate the manifests for a bag
     * <p/>
     * Our paths look like:
     * /staging/area/from_node/bag_uuid/manifest-sha256.txt
     * /staging/area/from_node/bag_uuid/tagmanifest-sha256.txt
     *
     * @param api      dpn api to update the replication request
     * @param transfer the replication request from the dpn api
     * @param flow     the flow object corresponding to the replication
     */
    public void validate(BalustradeTransfers api, Replication transfer, ReplicationFlow flow) {
        boolean valid;
        String uuid = transfer.getBag();
        String stage = settings.getStage();
        String depositor = transfer.getFromNode();

        // Read the manifests
        Path bag = Paths.get(stage, depositor, uuid);
        // TODO: Resolve named based off of transfer fixity
        Path manifest = bag.resolve(MANIFEST);
        Path tagmanifest = bag.resolve(TAG_MANIFEST);

        valid = validateManifest(uuid, tagmanifest, bag);

        // Just so we don't waste time
        if (valid) {
            valid = validateManifest(uuid, manifest, bag);
        }

        log.info("Bag {} is valid: {}", uuid, valid);
        if (valid) {
            flow.setValidated(true);
        } else {
            transfer.setCancelled(true);
            transfer.setCancelReason("Bag is invalid");
            transfer.setUpdatedAt(ZonedDateTime.now());
            Call<Replication> call = api.updateReplication(transfer.getReplicationId(), transfer);
            handleCall(call, flow);
        }
    }

    /**
     * Read a manifest and validate that the entries in it are correct
     *
     * @param uuid     the uuid of the bag
     * @param manifest the manifest to validate
     * @param bag      the path to the bag
     * @return true if valid; false otherwise
     */
    private boolean validateManifest(String uuid, Path manifest, Path bag) {
        String line;
        boolean valid = true;
        HashFunction func = Hashing.sha256();
        Charset cs = Charset.defaultCharset();

        try(BufferedReader br = java.nio.file.Files.newBufferedReader(manifest, cs)) {
            while ((line = br.readLine()) != null) {
                String[] split = line.split("\\s+", 2);
                if (split.length != 2) {
                    valid = false;
                    continue;
                }

                String digest = split[0];
                String path = split[1];
                Path file = bag.resolve(path);
                log.trace("Processing {}", file);

                HashCode hash = Files.hash(file.toFile(), func);
                if (!hash.toString().equalsIgnoreCase(digest)) {
                    log.error("[{}] Bad hash found for file {}", uuid, path);
                    valid = false;
                }
            }
        } catch (IOException e) {
            valid = false;
            log.error("IOException while validating manifest {}", manifest.toString(), e);
        }

        return valid;
    }

    /**
     * Download a bag from a dpn node with rsync
     * <p/>
     * Configured so that our final download location looks like:
     * /staging/area/from_node/bag_uuid
     *
     * @param transfer The replication transfer to download
     * @param flow     The flow object corresponding to the replication
     * @throws IOException an io related... exception
     */
    public void download(Replication transfer, ReplicationFlow flow) throws IOException {
        String stage = settings.getStage();
        String uuid = transfer.getBag();
        String from = transfer.getFromNode();
        String link = transfer.getLink();

        RsyncDetail detail = new RsyncDetail();
        detail.setLink(link);
        log.debug("[{}] Downloading {} from {}\n", from, uuid, link);

        // Create the dir for the node if it doesn't exist
        Path nodeDir = Paths.get(stage, transfer.getFromNode());
        if (!nodeDir.toFile().exists()) {
            java.nio.file.Files.createDirectories(nodeDir);
        }

        Path local = nodeDir.resolve(uuid + ".tar");
        String[] cmd = new String[]{"rsync",
                "-aL",                                   // archive, follow links
                "-e ssh -o 'PasswordAuthentication no'", // disable password auth
                "--stats",                               // print out statistics
                link,
                local.toString()};
        String stats;

        ProcessBuilder pb = new ProcessBuilder(cmd);
        try {
            Process p = pb.start();
            int exit = 0;
            try {
                exit = p.waitFor();
            } catch (InterruptedException e) {
                log.error("Interrupted waiting for rsync to complete", e);
                Thread.currentThread().interrupt();
            }

            stats = stringFromStream(p.getInputStream());

            if (exit != 0) {
                log.error("[{}] There was an error rsyncing {}! exit value {}", from, uuid, exit);
                String error = stringFromStream(p.getErrorStream());
                detail.setOutput(error);
                log.error(error);
            } else {
                log.info("rsync successful, updating replication transfer");
                detail.setOutput(stats);
                flow.setReceived(true);
            }

            log.debug("Rsync stats:\n {}", stats);
        } catch (IOException e) {
            log.error("Error executing rsync", e);
            detail.setOutput(e.getMessage());
        }

        flow.addRsync(detail);
    }

    /**
     * Create a string from the input stream
     *
     * @param is The input stream to string...ify
     * @return string blob of the input stream
     * @throws IOException if there's an error reading the InputStream
     */
    private String stringFromStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line).append("\n");
        }
        return out.toString();
    }

    /**
     * Digest the tagmanifest and update the api
     *
     * @param transfer The replication transfer to update
     * @param flow The replication flow
     */
    public void update(BalustradeTransfers balustrade, Replication transfer, ReplicationFlow flow) {
        // Get the files digest
        String stage = settings.getStage();
        Path tarball = Paths.get(stage,
                transfer.getFromNode(),
                transfer.getBag() + ".tar");
        log.trace("{}", tarball);
        HashCode hash = null;
        try (TarArchiveInputStream tais = new TarArchiveInputStream(java.nio.file.Files.newInputStream(tarball))) {
            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                if (entry.getName().equals(transfer.getBag() + "/" + TAG_MANIFEST)) {
                    // TODO: size = 1MB, in the event of absurdly large tag manifests
                    int size = (int) entry.getSize();
                    byte[] buf = new byte[size];
                    Hasher hasher = Hashing.sha256().newHasher();
                    tais.read(buf, 0, size);
                    hasher.putBytes(buf);
                    hash = hasher.hash();
                    break;
                }
            }
        } catch (IOException e) {
            log.error("Error trying to get receipt for bag {}", transfer.getBag(), e);
            return;
        }

        if (hash == null) { /* Cancel */
            log.info("Cancelling transfer");
            transfer.setCancelled(true);
            transfer.setCancelReason("Unable to create fixity");
        } else {            /* Update fixity */
            String receipt = hash.toString();
            log.info("Captured receipt {}", receipt);
            transfer.setFixityValue(receipt);
        }

        // Do the update. We don't need the response as the transfer
        // will be continued the next time we query
        transfer.setUpdatedAt(ZonedDateTime.now());
        Call<Replication> call = balustrade.updateReplication(transfer.getReplicationId(), transfer);
        handleCall(call, flow);
    }

    /**
     * Explode a tarball for a given transfer
     *
     * @param transfer The replication transfer to untar
     * @param flow     The flow object corresponding to the replication
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void untar(Replication transfer, ReplicationFlow flow) throws IOException {
        String stage = settings.getStage();
        Path tarball = Paths.get(stage, transfer.getFromNode(), transfer.getBag() + ".tar");

        String depositor = transfer.getFromNode();

        // Set up our tar stream and channel
        TarArchiveInputStream tais = new TarArchiveInputStream(java.nio.file.Files.newInputStream(tarball));
        TarArchiveEntry entry = tais.getNextTarEntry();
        ReadableByteChannel inChannel = Channels.newChannel(tais);

        // Get our root path (just the staging area), and create an updated bag path
        Path root = Paths.get(stage, depositor);

        while (entry != null) {
            Path entryPath = root.resolve(entry.getName());

            if (entry.isDirectory()) {
                log.trace("Creating directory {}", entry.getName());
                java.nio.file.Files.createDirectories(entryPath);
            } else {
                log.trace("Creating file {}", entry.getName());

                // Create the parent directories just in case
                entryPath.getParent().toFile().mkdirs();

                // In case files are greater than 2^32 bytes, we need to use a
                // RandomAccessFile and FileChannel to write them
                RandomAccessFile file = new RandomAccessFile(entryPath.toFile(), "rw");
                FileChannel out = file.getChannel();

                // The TarArchiveInputStream automatically updates its offset as
                // it is read, so we don't need to worry about it
                out.transferFrom(inChannel, 0, entry.getSize());
                out.close();
                file.close();
            }

            entry = tais.getNextTarEntry();
        }

        inChannel.close();
        flow.setExtracted(true);
    }

    private <T> DetailEmitter<T> handleCall(Call<T> call, ReplicationFlow flow) {
        DetailEmitter<T> emitter = new DetailEmitter<T>();
        call.enqueue(emitter);
        flow.addHttpDetail(emitter.emit());
        return emitter;
    }

}