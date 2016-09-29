package org.chronopolis.earth.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated Being replaced in favor of the @entity version
 *
 * Created by shake on 2/22/16.
 */
@Deprecated
public class LastSync {

    // We might want to consider changing this to a List[NodeSync]
    // where NodeSync also has a name field
    private Map<String, NodeSync> syncs;

    private static transient final String epoch = "1970-01-01T00:00:00Z";
    private static transient final String file = "last-syncs";
    private static transient final Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "sync");;
    private final DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC();

    public LastSync() {
        syncs = new HashMap<>();
    }

    // Getters and setters for our types of syncs
    public void addLastBagSync(String node, DateTime date) {
        NodeSync sync = syncs.getOrDefault(node, new NodeSync());
        sync.bag = formatter.print(date);
        syncs.put(node, sync);
    }

    public void addLastReplication(String node, DateTime date) {
        NodeSync sync = syncs.getOrDefault(node, new NodeSync());
        sync.replication = formatter.print(date);
        syncs.put(node, sync);
    }

    public void addLastNode(String node, DateTime date) {
        NodeSync sync = syncs.getOrDefault(node, new NodeSync());
        sync.node = formatter.print(date);
        syncs.put(node, sync);
    }

    public void addLastDigest(String node, DateTime date) {
        NodeSync sync = syncs.getOrDefault(node, new NodeSync());
        sync.digest = formatter.print(date);
        syncs.put(node, sync);
    }

    public void addLastFixity(String node, DateTime date) {
        NodeSync sync = syncs.getOrDefault(node, new NodeSync());
        sync.fixity = formatter.print(date);
        syncs.put(node, sync);
    }

    public void addLastIngest(String node, DateTime date) {
        NodeSync sync = syncs.getOrDefault(node, new NodeSync());
        sync.ingest = formatter.print(date);
        syncs.put(node, sync);
    }


    public String lastBagSync(String node) {
        NodeSync sync = syncs.getOrDefault(node, new NodeSync());
        return sync.bag;
    }

    public String lastReplicationSync(String node) {
        NodeSync sync = syncs.getOrDefault(node, new NodeSync());
        return sync.replication;
    }

    public String lastNodeSync(String node) {
        NodeSync sync = syncs.getOrDefault(node, new NodeSync());
        return sync.node;
    }

    public String lastDigestSync(String node) {
        NodeSync sync = syncs.getOrDefault(node, new NodeSync());
        return sync.digest;
    }

    public String lastFixitySync(String node) {
        NodeSync sync = syncs.getOrDefault(node, new NodeSync());
        return sync.fixity;
    }

    public String lastIngestSync(String node) {
        NodeSync sync = syncs.getOrDefault(node, new NodeSync());
        return sync.ingest;
    }

    public void write() throws IOException {
        Path dir = LastSync.dir;
        Files.createDirectories(dir);
        Path lastSyncPath = dir.resolve(file);

        BufferedWriter writer = Files.newBufferedWriter(lastSyncPath, Charset.defaultCharset());

        Gson g = new Gson();
        writer.write(g.toJson(syncs));
        writer.close();
    }

    public static LastSync read() throws IOException {
        Path dir = LastSync.dir;
        Files.createDirectories(dir);
        Path lastSyncPath = dir.resolve(file);
        LastSync sync;
        sync = new LastSync();

        if (Files.exists(lastSyncPath)) {
            String line;
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = Files.newBufferedReader(lastSyncPath, Charset.defaultCharset());
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();

            Gson g = new Gson();
            sync.syncs = g.fromJson(builder.toString(), new TypeToken<Map<String, NodeSync>>() {}.getType());
            // log.info("size: {}", sync.syncs.size());
            // sync.syncs.entrySet().forEach(e -> log.info("{} :: {}", e.getKey(), e.getValue()));
        }

        return sync;
    }


    protected class NodeSync {
        String bag = epoch;
        String node = epoch;
        String digest = epoch;
        String fixity = epoch;
        String ingest = epoch;
        String replication = epoch;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NodeSync nodeSync = (NodeSync) o;

            if (bag != null ? !bag.equals(nodeSync.bag) : nodeSync.bag != null) return false;
            if (replication != null ? !replication.equals(nodeSync.replication) : nodeSync.replication != null)
                return false;
            return node != null ? node.equals(nodeSync.node) : nodeSync.node == null;

        }

        @Override
        public int hashCode() {
            int result = bag != null ? bag.hashCode() : 0;
            result = 31 * result + (replication != null ? replication.hashCode() : 0);
            result = 31 * result + (node != null ? node.hashCode() : 0);
            return result;
        }

        // Getters for gson

        public String getBag() {
            return bag;
        }

        public String getReplication() {
            return replication;
        }

        public String getNode() {
            return node;
        }

        public String getDigest() {
            return digest; 
        }

        public String getFixity() {
            return fixity; 
        }

        public String getIngest() {
            return ingest;
        }
    }

}
