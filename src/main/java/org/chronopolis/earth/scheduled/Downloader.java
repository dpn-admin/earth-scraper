package org.chronopolis.earth.scheduled;

import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by shake on 3/31/15.
 */
@Component
@EnableScheduling
public class Downloader {
    private final Logger log = LoggerFactory.getLogger(Downloader.class);

    @Autowired
    BalustradeTransfers balustrade;

    /**
     * Replication ongoing and new transfers from a dpn node
     *
     * @throws InterruptedException
     */
    @Scheduled(cron = "0 0 * * * *")
    public void replicate() throws InterruptedException {
        Map<String, String> ongoing = Maps.newHashMap();
        ongoing.put("status", "A");
        ongoing.put("fixity", "False");
        System.out.println("Getting ongoing transfers");
        get(ongoing);


        System.out.println("Getting new transfers");
        get(Maps.<String, String>newHashMap());

        System.out.println("Done");
    }

    /**
     * GET our active replications from a dpn node
     *
     * @param query
     * @throws InterruptedException
     */
    private void get(Map<String, String> query) throws InterruptedException {
        int page = 1;
        String next;
        do {
            Response<Replication> transfers = balustrade.getReplications(query);
            next = transfers.getNext();
            System.out.printf("Count: %d\nNext: %s\nPrevious: %s\n",
                    transfers.getCount(),
                    transfers.getNext(),
                    transfers.getPrevious());
            for (Replication transfer : transfers.getResults()) {
                download(transfer);
                update(transfer);
            }

            ++page;
            query.put("page", String.valueOf(page));
        } while (next != null);
    }

    /**
     * Download a bag from a dpn node with rsync
     *
     * @param transfer
     * @throws InterruptedException
     */
    private void download(Replication transfer) throws InterruptedException {
        System.out.printf("Getting %s from %s\n", transfer.getUuid(), transfer.getLink());
        String[] cmd = new String[]{"rsync", "-aL", "--stats", transfer.getLink(), "/tmp/dpn/"};
        String stats;

        ProcessBuilder pb = new ProcessBuilder(cmd);
        try {
            Process p = pb.start();
            int exit = p.waitFor();

            stats = stringFromStream(p.getInputStream());


            if (exit != 0) {
                System.out.println("There was an error rsyncing!");
            }

            System.out.printf("Rsync stats:\n %s", stats);
        } catch (IOException e) {
            System.out.println("Error executing rsync");
        }

        TimeUnit.SECONDS.sleep(1);
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
    private void update(Replication transfer) {
        // Get the files digest
        HashFunction func = Hashing.sha256();
        Path file = Paths.get("/tmp/dpn/", transfer.getUuid() + ".tar");
        HashCode hash;
        try {
            hash = Files.hash(file.toFile(), func);
        } catch (IOException e) {
            System.out.println("Error hashing file");
            return;
        }

        // Set the receipt
        String receipt = hash.toString();
        transfer.setFixityValue(receipt);
        Replication updated = balustrade.updateReplication(transfer.getReplicationId(), transfer);

        if (updated.isFixityAccept()) {
            // push to chronopolis
            // chron.putBag(baggyBag);
        }
    }

    /**
     * Explode a tarball for a given transfer
     *
     * @param transfer
     */
    private void untar(Replication transfer) throws IOException {
        Path tarball = Paths.get("/tmp/dpn/", transfer.getUuid() + ".tar");
        String bags = "/tmp/dpn/";
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
