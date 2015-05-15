package org.chronopolis.earth.scheduled;

import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.chronopolis.earth.EarthSettings;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.chronopolis.rest.api.IngestAPI;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by shake on 3/31/15.
 */
@Component
@EnableScheduling
public class Downloader {
    private final Logger log = LoggerFactory.getLogger(Downloader.class);
    private static final String TAG_MANIFEST = "tagmanifest-sha256.txt";

    @Autowired
    TransferAPIs transfers;

    @Autowired
    IngestAPI chronopolis;

    @Autowired
    EarthSettings settings;


    /**
     * Replication ongoing and new transfers from a dpn node
     *
     * @throws InterruptedException
     */
    @Scheduled(cron = "${cron.replicate:0 0 * * * *}")
    public void replicate() throws InterruptedException, IOException {
        Map<String, String> ongoing = Maps.newHashMap();
        ongoing.put("status", "A");
        ongoing.put("fixity", "False");

        for (BalustradeTransfers api : transfers.getApiMap().values()) {
            log.debug("Getting ongoing transfers");
            get(api, ongoing);

            log.debug("Getting new transfers");
            get(api, Maps.<String, String>newHashMap());
        }
    }

    /**
     * GET our active replications from a dpn node
     *
     * @param balustrade dpn api to use for getting replication requests
     * @param query QueryParameters to use with the dpn api
     * @throws InterruptedException
     * @throws IOException
     */
    private void get(BalustradeTransfers balustrade,
                     Map<String, String> query) throws InterruptedException, IOException {
        int page = 1;
        String next;
        do {
            Response<Replication> transfers = balustrade.getReplications(query);
            next = transfers.getNext();
            log.debug("Count: {}\nNext: {}\nPrevious: {}\n",
                    transfers.getCount(),
                    transfers.getNext(),
                    transfers.getPrevious());
            for (Replication transfer : transfers.getResults()) {
                log.info("Replicating {}", transfer.getReplicationId());
                download(balustrade, transfer);
                untar(transfer);
                update(balustrade, transfer);
                validate(balustrade, transfer);
                push(transfer);
            }

            ++page;
            query.put("page", String.valueOf(page));
        } while (next != null);
    }

    private void push(Replication transfer) {
        if (transfer.isFixityAccept() && transfer.isBagValid()) {
            log.info("Bag is valid, pushing to chronopolis");
            // push to chronopolis
            /*
            IngestRequest request = new IngestRequest();
            request.setDepositor(updated.getFromNode());
            request.setName(updated.getUuid());
            request.setLocation(updated.getFromNode() + "/" + updated.getUuid());
            chronopolis.putBag(request);
            */
        }

    }

    /**
     * Validate the manifests for a bag
     *
     * Our paths look like:
     * /staging/area/from_node/bag_uuid/manifest-sha256.txt
     * /staging/area/from_node/bag_uuid/tagmanifest-sha256.txt
     *
     * @param api dpn api to update the replication request
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
        // TODO: Create named based off of transfer fixity
        String manifestName = "manifest-sha256.txt";
        String tagmanifestName = "tagmanifest-sha256.txt";

        Path bag = Paths.get(stage, depositor, uuid);
        Path manifest = bag.resolve(manifestName);
        Path tagmanifest = bag.resolve(tagmanifestName);

        valid = validateManifest(uuid, tagmanifest, bag);

        // Just so we don't waste time
        if (valid) {
            valid = validateManifest(uuid, manifest, bag);
        }

        log.info("Bag {} is valid: {}", uuid, valid);
        transfer.setBagValid(valid);
        api.updateReplication(transfer.getReplicationId(), transfer);

    }

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
     *
     * Configured so that our final download location looks like:
     * /staging/area/from_node/bag_uuid
     *
     * @param transfer
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
                transfer.setStatus(Replication.Status.RECEIVED);
                api.updateReplication(transfer.getReplicationId(), transfer);
            }

            log.debug("Rsync stats:\n {}", stats);
        } catch (IOException e) {
            log.error("Error executing rsync");
        }

    }

    /**
     * Create a string from the input stream
     *
     * @param is
     * @return
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
     * Digest a file and update the api
     *
     * @param transfer
     */
    private void update(BalustradeTransfers balustrade, Replication transfer) {
        // Get the files digest
        HashFunction func = Hashing.sha256();
        String stage = settings.getStage();
        Path file = Paths.get(stage,
                transfer.getFromNode(),
                transfer.getUuid(),
                TAG_MANIFEST);
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
        Replication update = balustrade.updateReplication(transfer.getReplicationId(), transfer);
        transfer.setFixityAccept(update.isFixityAccept());
    }

    /**
     * Explode a tarball for a given transfer
     *
     * @param transfer
     */
    private void untar(Replication transfer) throws IOException {
        String stage = settings.getStage();
        Path tarball = Paths.get(stage, transfer.getFromNode(), transfer.getUuid() + ".tar");

        String bags = stage;
        String depositor = transfer.getFromNode();

        // Set up our tar stream and channel
        TarArchiveInputStream tais = new TarArchiveInputStream(java.nio.file.Files.newInputStream(tarball));
        TarArchiveEntry entry = tais.getNextTarEntry();
        ReadableByteChannel inChannel = Channels.newChannel(tais);

        // Get our root path (just the staging area), and create an updated bag path
        Path root = Paths.get(bags, depositor);
        Path bag = root.resolve(entry.getName());

        while (entry != null) {
            Path entryPath = root.resolve(entry.getName());

            if (entry.isDirectory()) {
                log.trace("Creating directory {}", entry.getName());
                java.nio.file.Files.createDirectories(entryPath);
            } else {
                log.trace("Creating file {}", entry.getName());

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
