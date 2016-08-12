package org.chronopolis.earth.scheduled;

import com.google.common.collect.ImmutableList;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.chronopolis.earth.api.BagAPIs;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.LocalAPI;
import org.chronopolis.earth.api.NodeAPIs;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.models.Response;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import retrofit2.Call;
import retrofit2.Callback;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Test for our synchronizer
 *
 * TODO: Node sync tests
 * TODO: Ingest sync tests
 * TODO: Fixity sync tests
 * TODO: Digest sync tests
 *
 * <p>
 * Created by shake on 5/10/16.
 */
public class SynchronizerTest {

    private static final Logger log = LoggerFactory.getLogger(SynchronizerTest.class);
    private static Sql2o sql2o;

    final String epoch = "1970-01-01T00:00:00Z";
    final String node = "test-node";

    @Mock BalustradeBag localBag;
    @Mock BalustradeBag remoteBag;

    @Mock BalustradeTransfers localTransfer;
    @Mock BalustradeTransfers remoteTransfer;

    @Mock BalustradeNode localNode;
    @Mock BalustradeNode remoteNode;

    Synchronizer synchronizer;

    @BeforeClass
    public static void setupDB() {
        HikariConfig hc = new HikariConfig();
        hc.setMaximumPoolSize(1);
        hc.setDataSourceClassName("org.sqlite.SQLiteDataSource");
        // hc.setJdbcUrl("jdbc:sqlite:/tmp/ed-test-db.sqlite3");
        sql2o = new Sql2o(new HikariDataSource(hc));
        initTables(sql2o);
    }

   private static void initTables(Sql2o sql2o) {
        try (Connection conn = sql2o.open()) {
            log.info("Checking if sync tables exist");
            createIfNotExists(conn, "sync_view", "CREATE TABLE sync_view(sync_id INTEGER PRIMARY KEY ASC, host string, status string, type string)");
            createIfNotExists(conn, "http_detail", "CREATE TABLE http_detail(http_id INTEGER PRIMARY KEY ASC, url string, request_body text, request_method string, response_code SMALLINT, response_body text, " +
                    "sync INTEGER, replication string, " +
                    "FOREIGN KEY(sync) REFERENCES sync_view(sync_id), " +
                    "FOREIGN KEY(replication) REFERENCES replication_flow(replication_id))");
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


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        LocalAPI localAPI = new LocalAPI();
        localAPI.setBagAPI(localBag);
        localAPI.setNodeAPI(localNode);
        localAPI.setTransfersAPI(localTransfer);
        DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC();
        BagAPIs bagAPIs = new BagAPIs();
        bagAPIs.put(node, remoteBag);
        NodeAPIs nodeAPIs = new NodeAPIs();
        nodeAPIs.put(node, remoteNode);
        TransferAPIs transferAPIs = new TransferAPIs();
        transferAPIs.put(node, remoteTransfer);

        synchronizer = new Synchronizer(fmt, sql2o, bagAPIs, transferAPIs, nodeAPIs, localAPI);
    }

    static <T> Response<T> responseWrapper(T t) {
        Response<T> remoteResponse = new Response<>();
        remoteResponse.setCount(1);
        remoteResponse.setNext(null);
        remoteResponse.setPrevious(null);
        remoteResponse.setResults(ImmutableList.of(t));
        return remoteResponse;
    }

    void blockUnitShutdown() throws InterruptedException {
        synchronizer.service.shutdown();
        synchronizer.service.awaitTermination(5, TimeUnit.MINUTES);
    }

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
            return new okhttp3.Request.Builder()
                    .url("http://localhost:1234")
                    .method("GET", null)
                    .build();
        }
    }

    class ExceptedCall<T> extends SuccessfulCall<T> {
        ExceptedCall(T t) {
            super(t);
        }

        @Override
        public retrofit2.Response<T> execute() throws IOException {
            throw new IOException("test exception");
        }

        @Override
        public void enqueue(Callback<T> callback) {
            callback.onFailure(this, new IOException("test exception"));
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


}