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
 *
 * Created by shake on 2/22/16.
 */
public class LastSync {

    private Map<String, String> syncs;

    private static transient final String file = "last-syncs";
    private static transient final Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "sync");;
    private final DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC();

    public LastSync() {
        syncs = new HashMap<>();
    }

    public void addLastSync(String node, DateTime date) {
        syncs.put(node, formatter.print(date));
    }

    public void addLastSync(String node, String date) {
        syncs.put(node, date);
    }


    public String getLastSync(String node) {
        return syncs.getOrDefault(node, formatter.print(new DateTime(0)));
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
            sync.syncs = g.fromJson(builder.toString(), new TypeToken<Map<String, String>>() {}.getType());
            // log.info("size: {}", sync.syncs.size());
            // sync.syncs.entrySet().forEach(e -> log.info("{} :: {}", e.getKey(), e.getValue()));
        }

        return sync;
    }

}
