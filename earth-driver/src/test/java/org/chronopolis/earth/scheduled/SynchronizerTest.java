package org.chronopolis.earth.scheduled;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.Events;
import org.chronopolis.earth.api.LocalAPI;
import org.chronopolis.earth.api.Remote;
import org.chronopolis.earth.config.Endpoint;
import org.chronopolis.earth.domain.LastSync;
import org.chronopolis.earth.domain.SyncType;
import org.chronopolis.earth.models.Response;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;
import retrofit2.Callback;

import javax.persistence.NoResultException;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Test for our synchronizer
 *
 * Created by shake on 5/10/16.
 */
public class SynchronizerTest {

    final ZonedDateTime epoch = ZonedDateTime.from(java.time.Instant.EPOCH.atZone(ZoneOffset.UTC));
    final String node = "test-node";

    @Mock BalustradeBag localBag;
    @Mock BalustradeBag remoteBag;

    @Mock BalustradeTransfers localTransfer;
    @Mock BalustradeTransfers remoteTransfer;

    @Mock BalustradeNode localNode;
    @Mock BalustradeNode remoteNode;

    @Mock Events localEvents;
    @Mock Events remoteEvents;

    Synchronizer synchronizer;
    private SessionFactory factory;
    private static StandardServiceRegistry registry;

    @BeforeClass
    public static void setupDB() {
        registry = new StandardServiceRegistryBuilder()
                .configure()
                .applySetting("hibernate.hikari.dataSource.url", "jdbc:h2:mem:TEST")
                .build();
    }


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        LocalAPI localAPI = new LocalAPI();
        localAPI.setBagAPI(localBag);
        localAPI.setNodeAPI(localNode);
        localAPI.setTransfersAPI(localTransfer);
        localAPI.setEventsAPI(localEvents);

        Endpoint remotePoint = new Endpoint()
                .setApiRoot("test-api-root")
                .setAuthKey("Token token=test-api-auth-key")
                .setName(node);
        Gson g = new GsonBuilder().create();
        Remote r = new MockRemote(remotePoint, g)
                .setBags(remoteBag)
                .setEvents(remoteEvents)
                .setTransfers(remoteTransfer)
                .setNodes(remoteNode);
        List<Remote> remotes = ImmutableList.of(r);

        // Probably not the best thing, but this works for now. We want the transactions
        // to roll back between tests, and this is the easiest way to do it.
        factory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
        synchronizer = new Synchronizer(localAPI, remotes, factory);
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

    LastSync getLastSync(String node, SyncType type) {
        try (Session session = factory.openSession()) {
            return (LastSync) session.createQuery("select l from LastSync l where l.node = :node and l.type = :type")
                    .setParameter("node", node)
                    .setParameter("type", type)
                    .getSingleResult();
        } catch (NoResultException ne) {
            return new LastSync()
                    .setNode(node)
                    .setType(type);
        }
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
            return new Request.Builder()
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