package org.chronopolis.earth.scheduled;

import com.google.common.collect.ImmutableList;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.chronopolis.earth.api.BagAPIs;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.LocalAPI;
import org.chronopolis.earth.api.NodeAPIs;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.models.Response;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;
import retrofit2.Callback;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Test for our synchronizer
 *
 * TODO: Node sync tests
 *
 * <p>
 * Created by shake on 5/10/16.
 */
// @RunWith(SpringJUnit4ClassRunner.class)
public class SynchronizerTest {

    final String epoch = "1970-01-01T00:00:00Z";
    final String node = "test-node";

    @Mock BalustradeBag remoteBag;
    @Mock BalustradeTransfers remoteTransfer;
    @Mock BalustradeNode remoteNode;
    @Mock BalustradeBag localBag;
    @Mock BalustradeTransfers localTransfer;
    @Mock BalustradeNode localNode;

    Synchronizer synchronizer;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        LocalAPI localAPI = new LocalAPI();
        localAPI.setBagAPI(localBag);
        localAPI.setNodeAPI(localNode);
        localAPI.setTransfersAPI(localTransfer);

        synchronizer = new Synchronizer();
        synchronizer.formatter = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC();
        // Init our fields
        synchronizer.local = localAPI;
        synchronizer.bagAPIs = new BagAPIs();
        synchronizer.nodeAPIs = new NodeAPIs();
        synchronizer.transferAPIs = new TransferAPIs();

        synchronizer.bagAPIs.put(node, remoteBag);
        synchronizer.nodeAPIs.put(node, remoteNode);
        synchronizer.transferAPIs.put(node, remoteTransfer);

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
            callback.onResponse(retrofit2.Response.success(t));
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
            callback.onFailure(new IOException("test exception"));
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
            callback.onResponse(response);
        }
    }


}