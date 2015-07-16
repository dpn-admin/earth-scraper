package org.chronopolis.earth.scheduled;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.chronopolis.earth.EarthSettings;
import org.chronopolis.earth.SimpleCallback;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.IngestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
import java.util.List;
import java.util.Map;

/**
 * TODO: Find a better way to query each api/grab replication transfers
 * The current has mucho code duplication
 *
 *
 * Created by shake on 3/31/15.
 */
@Component
@EnableScheduling
public class Downloader {
    private final Logger log = LoggerFactory.getLogger(Downloader.class);
    private static final String TAG_MANIFEST = "tagmanifest-sha256.txt";
    private static final String MANIFEST = "manifest-sha256.txt";

    @Autowired
    TransferAPIs apis;

    @Autowired
    IngestAPI chronopolis;

    @Autowired
    EarthSettings settings;

    private Response<Replication> getTransfers(BalustradeTransfers balustrade,
                                               Map<String, String> params) {
        SimpleCallback<Response<Replication>> callback = new SimpleCallback<>();
        balustrade.getReplications(params, callback);
        Optional<Response<Replication>> response = callback.getResponse();

        // get the actual response OR an empty response (in the event of failure)
        Response<Replication> transfers = response.or(emptyResponse());
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
        response.setResults(Lists.<Replication>newArrayList());
        return response;
    }

    // Scheduled tasks. Delegate to functions based on what state the replication is in

    @Scheduled(cron = "${earth.cron.replicate:0 * * * * *}")
    private void requested() {
        int page;
        int pageSize = 10;
        Replication.Status status = Replication.Status.REQUESTED;

        Response<Replication> transfers;
        Map<String, String> params = Maps.newHashMap();
        params.put("status", status.getName());
        params.put("page_size", String.valueOf(pageSize));

        for (Map.Entry<String, BalustradeTransfers> entry : apis.getApiMap().entrySet()) {
            page = 1;
            String node = entry.getKey();
            BalustradeTransfers api = entry.getValue();
            log.info("Getting {} replications from {}", status.getName(), node);
            do {
                params.put("page", String.valueOf(page));
                transfers = getTransfers(api, params);
                for (Replication transfer : transfers.getResults()) {
                    String from = transfer.getFromNode();
                    String uuid = transfer.getUuid();
                    log.info("Downloading");

                    try {
                        download(api, transfer);
                    } catch (InterruptedException | IOException e) {
                        log.error("Error downloading {}::{}, skipping", from, uuid, e);
                    }
                }

                ++page;
            } while (transfers.getNext() != null);

        }

    }

    @Scheduled(cron = "${earth.cron.replicate:0 * * * * *}")
    private void received() {
        int page;
        int pageSize = 10;
        Replication.Status status = Replication.Status.RECEIVED;

        Response<Replication> transfers;
        Map<String, String> params = Maps.newHashMap();
        params.put("status", status.getName());
        params.put("page_size", String.valueOf(pageSize));

        for (Map.Entry<String, BalustradeTransfers> entry : apis.getApiMap().entrySet()) {
            page = 1;
            String node = entry.getKey();
            BalustradeTransfers api = entry.getValue();
            log.info("Getting {} replications from {}", status.getName(), node);
            do {
                params.put("page", String.valueOf(page));
                transfers = getTransfers(api, params);
                for (Replication transfer : transfers.getResults()) {
                    String from = transfer.getFromNode();
                    String uuid = transfer.getUuid();

                    try {
                        untar(transfer);
                        update(api, transfer);
                    } catch (IOException e) {
                        log.error("Error untarring {}::{}, skipping", from, uuid, e);
                    }
                }

                ++page;
            } while (transfers.getNext() != null);

        }
    }

    @Scheduled(cron = "${earth.cron.replicate:0 * * * * *}")
    private void confirmed() {
        int page;
        int pageSize = 10;
        Replication.Status status = Replication.Status.CONFIRMED;

        Response<Replication> transfers;
        Map<String, String> params = Maps.newHashMap();
        params.put("status", status.getName());
        params.put("page_size", String.valueOf(pageSize));
        params.put("order_by", "updated_on");

        for (Map.Entry<String, BalustradeTransfers> entry : apis.getApiMap().entrySet()) {
            page = 1;
            String node = entry.getKey();
            BalustradeTransfers api = entry.getValue();
            log.info("Getting {} replications from {}", status.getName(), node);

            do {
                params.put("page", String.valueOf(page));
                transfers = getTransfers(api, params);
                for (Replication transfer : transfers.getResults()) {
                    // TODO: Exit when we get past a certain update time
                    if (transfer.status() == Replication.Status.STORED) {
                        continue;
                    }

                    // String from = transfer.getFromNode();
                    String uuid = transfer.getUuid();

                    Map<String, Object> chronParams = Maps.newHashMap();
                    chronParams.put("name", uuid);

                    // TODO: Query parameters for bag
                    // Since bags are named by uuids, this should be unique
                    // but it's still a list so we get that and check if it's empty
                    List<Bag> bags = chronopolis.getBags(chronParams)
                            .getContent();

                    if (!bags.isEmpty()) {
                        Bag b = bags.get(0);
                        log.info("Bag found in chronopolis, status is {}", b.getStatus());
                        if (b.getStatus() == BagStatus.REPLICATED) {
                            store(api, transfer);
                        }
                    } else {
                        log.info("Bag not found in chronopolis, validating and pushing");
                        validate(api, transfer);
                        push(transfer);
                    }
                }

                ++page;
            } while (transfers.getNext() != null);
        }
    }

    // Operations

    /**
     * Update a replication to note completion of ingestion into chronopolis
     *
     * @param api The transfer API to use
     * @param transfer The replication transfer to update
     */
    private void store(BalustradeTransfers api, Replication transfer) {
        SimpleCallback<Replication> callback = new SimpleCallback<>();
        transfer.setStatus(Replication.Status.STORED);
        api.updateReplication(transfer.getReplicationId(), transfer, callback);
    }


    /**
     * Notify our Chronopolis ingest server that a bag can be replicated
     * in to Chronopolis
     *
     * @param transfer The replication transfer being ingested into Chronopolis
     */
    private void push(Replication transfer) {
        if (transfer.isFixityAccept() && transfer.isBagValid()) {
            log.info("Bag is valid, pushing to chronopolis");
            // push to chronopolis
            IngestRequest request = new IngestRequest();
            request.setDepositor(transfer.getFromNode());
            request.setName(transfer.getUuid());
            request.setLocation(transfer.getFromNode() + "/" + transfer.getUuid());
            chronopolis.stageBag(request);
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
     */
    private void validate(BalustradeTransfers api, Replication transfer) {
        if (!transfer.isFixityAccept()) {
            log.info("Fixity not accepted, setting bag as false");
            transfer.setBagValid(false);
            return;
        }

        boolean valid;
        String uuid = transfer.getUuid();
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
        transfer.setBagValid(valid);

        SimpleCallback<Replication> callback = new SimpleCallback<>();
        api.updateReplication(transfer.getReplicationId(), transfer, callback);
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

        try {
            BufferedReader br = java.nio.file.Files.newBufferedReader(manifest, cs);
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
     * @throws InterruptedException
     */
    private void download(BalustradeTransfers api, Replication transfer) throws InterruptedException, IOException {
        log.debug("Getting {} from {}\n", transfer.getUuid(), transfer.getLink());
        String stage = settings.getStage();

        // Create the dir for the node if it doesn't exist
        Path nodeDir = Paths.get(stage, transfer.getFromNode());
        if (!nodeDir.toFile().exists()) {
            java.nio.file.Files.createDirectories(nodeDir);
        }

        Path local = nodeDir.resolve(transfer.getUuid() + ".tar");
        String[] cmd = new String[]{"rsync",
                "-aL",                                   // archive, follow links
                "-e ssh -o 'PasswordAuthentication no'", // disable password auth
                "--stats",                               // print out statistics
                transfer.getLink(),
                local.toString()};
        String stats;

        ProcessBuilder pb = new ProcessBuilder(cmd);
        try {
            Process p = pb.start();
            int exit = p.waitFor();

            stats = stringFromStream(p.getInputStream());

            if (exit != 0) {
                log.error("There was an error rsyncing! exit value {}", exit);
                String error = stringFromStream(p.getErrorStream());
                log.error(error);
            } else {
                log.info("rsync successful, updating replication transfer");

                SimpleCallback<Replication> callback = new SimpleCallback<>();
                transfer.setStatus(Replication.Status.RECEIVED);
                api.updateReplication(transfer.getReplicationId(), transfer, callback);
            }

            log.debug("Rsync stats:\n {}", stats);
        } catch (IOException e) {
            log.error("Error executing rsync");
        }

    }

    /**
     * Create a string from the input stream
     *
     * @param is The input stream to string...ify
     * @return string blob of the input stream
     * @throws IOException
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
     * Digest the tarball and update the api
     *
     * @param transfer The replication transfer to update
     */
    private void update(BalustradeTransfers balustrade, Replication transfer) {
        // Get the files digest
        HashFunction func = Hashing.sha256();
        String stage = settings.getStage();
        Path file = Paths.get(stage,
                transfer.getFromNode(),
                transfer.getUuid() + ".tar");
        HashCode hash;
        try {
            hash = Files.hash(file.toFile(), func);
        } catch (IOException e) {
            log.error("Error hashing file", e);
            return;
        }

        // Set the receipt
        String receipt = hash.toString();
        transfer.setFixityValue(receipt);

        // Do the update
        SimpleCallback<Replication> callback = new SimpleCallback<>();
        balustrade.updateReplication(transfer.getReplicationId(), transfer, callback);
        Optional<Replication> response = callback.getResponse();

        if (response.isPresent()) {
            Replication update = response.get();
            transfer.setFixityAccept(update.isFixityAccept());
        }
    }

    /**
     * Explode a tarball for a given transfer
     *
     * @param transfer The replication transfer to untar
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void untar(Replication transfer) throws IOException {
        String stage = settings.getStage();
        Path tarball = Paths.get(stage, transfer.getFromNode(), transfer.getUuid() + ".tar");

        String depositor = transfer.getFromNode();

        // Set up our tar stream and channel
        TarArchiveInputStream tais = new TarArchiveInputStream(java.nio.file.Files.newInputStream(tarball));
        TarArchiveEntry entry = tais.getNextTarEntry();
        ReadableByteChannel inChannel = Channels.newChannel(tais);

        // Get our root path (just the staging area), and create an updated bag path
        Path root = Paths.get(stage, depositor);
        // Path bag = root.resolve(entry.getName());

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
            }

            entry = tais.getNextTarEntry();
        }
    }

}
