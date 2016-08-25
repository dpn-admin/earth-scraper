package org.chronopolis.earth.scheduled;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.rest.api.IngestAPI;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import retrofit2.Call;
import retrofit2.Callback;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;

import static org.mockito.Mockito.mock;

/**
 * Super class for our download tests
 *
 * Sets up most of what we need (staging areas/apis/db)
 * Provides create replication helpers
 *
 * Created by shake on 6/24/16.
 */
@SuppressWarnings("WeakerAccess")
public class DownloaderTest {

    private static final Logger log = LoggerFactory.getLogger(DownloaderTest.class);
    static Sql2o sql2o;

    final String invalid = "9049d506-ab07-4275-b7b9-288d3fcadcd7";

    Path bagLink;
    Path bagExtracted;

    TransferAPIs apis;
    Downloader downloader;

    IngestAPI chronopolis;
    BalustradeTransfers transfer;

    @BeforeClass
    public static void setupDB() {
        HikariConfig hc = new HikariConfig();
        hc.setMaximumPoolSize(1);
        hc.setDataSourceClassName("org.sqlite.SQLiteDataSource");
        // hc.setJdbcUrl("jdbc:sqlite:/tmp/ed-test-db.sqlite3");
        sql2o = new Sql2o(new HikariDataSource(hc));
        initTables(sql2o);
    }

    @Before
    public void setup() throws URISyntaxException {
        log.info("Setting up temp database");

        chronopolis = mock(IngestAPI.class);
        transfer = mock(BalustradeTransfers.class);
        apis = new TransferAPIs();
        apis.put("mock-node", transfer);

        URL resources = ClassLoader.getSystemClassLoader().getResource("");
        bagLink = Paths.get(resources.toURI()).resolve("tar");
        bagExtracted = Paths.get(resources.toURI()).resolve("stage");
    }

    // TODO: Common home for these

    private static void initTables(Sql2o sql2o) {
        try (Connection conn = sql2o.open()) {
            log.info("Checking if replication_flow exists");
            createIfNotExists(conn, "replication_flow", "CREATE TABLE replication_flow(replication_id string PRIMARY KEY, node string, pushed TINYINT, received TINYINT, extracted TINYINT, validated TINYINT)");
        }
    }

    private static void createIfNotExists(Connection conn, String table, String create) {
        String select = "SELECT name FROM sqlite_master WHERE type='table' AND name = :name";
        String name = conn.createQuery(select).addParameter("name", table).executeAndFetchFirst(String.class);
        if (name == null) {
            log.info("Creating table {}", table);
            conn.createQuery(create).executeUpdate();
        }
    }

    Replication createReplication(String id, boolean storeRequested, boolean stored) {
        return createReplication(id, bagLink.toString(), storeRequested, stored);
    }

    Replication createReplication(String id, String link, boolean storeRequested, boolean stored) {
        Replication r = new Replication();
        r.setReplicationId(id);
        r.setBag("599b663d-b5e8-4f56-b332-4418eac0f8f2");
        r.setCreatedAt(ZonedDateTime.now());
        r.setUpdatedAt(ZonedDateTime.now());
        r.setLink(link);
        r.setStoreRequested(storeRequested);
        r.setStored(stored);
        r.setCancelled(false);
        r.setFixityAlgorithm("sha256");
        r.setFromNode("mock-node");
        r.setProtocol("rsync");
        r.setToNode("chron-test");
        return r;
    }

    // TODO: These need a home

    class SuccessfulCall<T> implements Call<T> {
        T t;

        SuccessfulCall(T t) {
            this.t = t;
        }

        @Override
        public retrofit2.Response<T> execute() throws IOException {
            return retrofit2.Response.success(t);
        }

        @Override
        public void enqueue(Callback<T> callback) {
            callback.onResponse(this, retrofit2.Response.success(t));
        }

        @Override
        public boolean isExecuted() {
            return false;
        }

        @Override
        public void cancel() {

        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public Call<T> clone() {
            return null;
        }

        @Override
        public Request request() {
            return null;
        }
    }

    class FailedCall<T> extends SuccessfulCall<T> {
        private final retrofit2.Response response;

        FailedCall(T t) {
            super(t);
            this.response = retrofit2.Response.error(404, ResponseBody.create(MediaType.parse("text/plain"), "not found"));
        }

        @Override
        public retrofit2.Response<T> execute() throws IOException {
            return response;
        }

        @Override
        public void enqueue(Callback<T> callback) {
            callback.onResponse(this, response);
        }
    }

    class ExceptedCall<T> extends SuccessfulCall<T> {
        private final retrofit2.Response response;

        ExceptedCall() {
            super(null);
            this.response = retrofit2.Response.error(404, ResponseBody.create(MediaType.parse("text/plain"), "not found"));
        }

        @Override
        public retrofit2.Response<T> execute() throws IOException {
            throw new IOException("Expected test IOException");
        }

        @Override
        public void enqueue(Callback<T> callback) {
            callback.onResponse(this, response);
        }
    }
}