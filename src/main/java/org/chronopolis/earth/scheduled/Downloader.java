package org.chronopolis.earth.scheduled;

import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

}
