package org.chronopolis.earth.scheduled;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.domain.ReplicationFlow;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.rest.api.IngestAPI;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    final String invalid = "9049d506-ab07-4275-b7b9-288d3fcadcd7";

    Path bagLink;
    Path bagExtracted;

    TransferAPIs apis;
    Downloader downloader;

    IngestAPI chronopolis;
    BalustradeTransfers transfer;
    static SessionFactory factory;

    @BeforeClass
    public static void setupDB() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure()
                .applySetting("hibernate.hikari.dataSource.url", "jdbc:h2:mem:TEST")
                .build();

        factory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
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

    void saveNewFlow(Replication r, boolean extracted, boolean received, boolean validated, boolean pushed) {
        Session session = factory.openSession();
        Transaction transaction = session.beginTransaction();
        ReplicationFlow flow = new ReplicationFlow();
        flow.setId(r.getReplicationId());
        flow.setNode(r.getFromNode());
        flow.setExtracted(extracted);
        flow.setReceived(received);
        flow.setValidated(validated);
        flow.setPushed(pushed);
        session.persist(flow);
        transaction.commit();
        session.close();
    }

    ReplicationFlow getFlow(String id) {
        try (Session s = factory.openSession()) {
            return s.get(ReplicationFlow.class, id);
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
        public Response<T> execute() throws IOException {
            return Response.success(t);
        }

        @Override
        public void enqueue(Callback<T> callback) {
            callback.onResponse(this, Response.success(t));
        }

        @Override
        public boolean isExecuted() {
            return false;
        }

        @Override
        public void cancel() {
            // We don't actually cancel these so there's nothing to do
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
        private final Response<T> response;

        FailedCall(T t) {
            super(t);
            this.response = Response.error(404, ResponseBody.create(MediaType.parse("text/plain"), "not found"));
        }

        @Override
        public Response<T> execute() throws IOException {
            return response;
        }

        @Override
        public void enqueue(Callback<T> callback) {
            callback.onResponse(this, response);
        }
    }

    class ExceptedCall<T> extends SuccessfulCall<T> {
        private final Response<T> response;

        ExceptedCall() {
            super(null);
            this.response = Response.error(404, ResponseBody.create(MediaType.parse("text/plain"), "not found"));
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