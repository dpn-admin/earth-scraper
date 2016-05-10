package org.chronopolis.earth.scheduled;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.chronopolis.earth.api.BagAPIs;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.LocalAPI;
import org.chronopolis.earth.api.NodeAPIs;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.models.Bag;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;
import retrofit2.Callback;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private final String epoch = "1970-01-01T00:00:00Z";
    private final String node = "test-node";
    private Bag bag;
    private Replication replication;

    @Mock
    BalustradeBag remoteBag;
    @Mock
    BalustradeTransfers remoteTransfer;
    @Mock
    BalustradeNode remoteNode;
    @Mock
    BalustradeBag localBag;
    @Mock
    BalustradeTransfers localTransfer;
    @Mock
    BalustradeNode localNode;

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

        // Setup our bag
        bag = setupBag();
        replication = setupReplication();
    }

    private Replication setupReplication() {
        String uuid = UUID.randomUUID().toString();
        Replication r = new Replication();
        r.setUuid(uuid);
        r.setFromNode(node);
        r.setToNode(node);
        r.setCreatedAt(DateTime.now());
        r.setUpdatedAt(DateTime.now());
        r.setBagValid(true);
        r.setFixityNonce("");
        r.setFixityAccept(true);
        r.setFixityAlgorithm("uuid");
        r.setFixityValue(uuid);
        r.setLink("link");
        r.setProtocol("rsync");
        r.setStatus(Replication.Status.STORED);
        return r;
    }

    private Bag setupBag() {
        String uuid = UUID.randomUUID().toString();
        Bag b = new Bag();
        b.setAdminNode(node);
        b.setIngestNode(node);
        b.setBagType('D');
        b.setCreatedAt(DateTime.now());
        b.setUpdatedAt(DateTime.now());
        b.setUuid(uuid);
        b.setFirstVersionUuid(uuid);
        b.setInterpretive(ImmutableList.of());
        b.setReplicatingNodes(ImmutableList.of());
        b.setRights(ImmutableList.of());
        b.setLocalId(uuid);
        b.setFixities(ImmutableMap.of());
        b.setSize(0L);
        b.setVersion(1L);
        b.setMember(uuid);
        return b;
    }

    public static <T> Response<T> responseWrapper(T t) {
        Response<T> remoteResponse = new Response<>();
        remoteResponse.setCount(1);
        remoteResponse.setNext(null);
        remoteResponse.setPrevious(null);
        remoteResponse.setResults(ImmutableList.of(t));
        return remoteResponse;
    }

    private void blockUnitShutdown() throws InterruptedException {
        synchronizer.service.shutdown();
        synchronizer.service.awaitTermination(5, TimeUnit.MINUTES);
    }

    /**
     * Test that a bag was sync'd with no errors
     * At the end of the test LastSync should be today
     */
    @Test
    public void testBagSuccessfulSync() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        ImmutableMap<String, String> params = ImmutableMap.of(
                "admin_node", node,
                "after", epoch);
        when(remoteBag.getBags(params)).thenReturn(new SuccessfulCall<>(responseWrapper(bag)));
        when(localBag.getBag(bag.getUuid()))
                .thenReturn(new SuccessfulCall<>(bag));
        when(localBag.updateBag(bag.getUuid(), bag))
                .thenReturn(new SuccessfulCall<>(bag));
        synchronizer.readLastSync();
        synchronizer.syncBags();

        blockUnitShutdown();

        verify(remoteBag, times(1)).getBags(params);
        verify(localBag, times(1)).getBag(bag.getUuid());
        verify(localBag, times(1)).updateBag(bag.getUuid(), bag);
        Assert.assertNotEquals(epoch, synchronizer.lastSync.lastBagSync(node));
    }

    /**
     * Test that a communication error occurred with the remote server
     * At the end of the test LastSync should be the epoch
     */
    @Test
    public void testBagRemoteIOE() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        ImmutableMap<String, String> params = ImmutableMap.of(
                "admin_node", node,
                "after", epoch);
        when(remoteBag.getBags(params)).thenReturn(new ExceptedCall<>(responseWrapper(bag)));
        synchronizer.readLastSync();
        synchronizer.syncBags();

        blockUnitShutdown();

        verify(remoteBag, times(1)).getBags(params);
        verify(localBag, times(0)).getBag(bag.getUuid());
        verify(localBag, times(0)).updateBag(bag.getUuid(), bag);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastBagSync(node));
    }

    /**
     * Test that a non successful response was returned from the remote server
     * At the end of the test LastSync should be the epoch
     */
    @Test
    public void testBagRemoteFailure() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        ImmutableMap<String, String> params = ImmutableMap.of(
                "admin_node", node,
                "after", epoch);
        when(remoteBag.getBags(params)).thenReturn(new FailedCall<>(responseWrapper(bag)));
        synchronizer.readLastSync();
        synchronizer.syncBags();

        blockUnitShutdown();

        verify(remoteBag, times(1)).getBags(params);
        verify(localBag, times(0)).getBag(bag.getUuid());
        verify(localBag, times(0)).updateBag(bag.getUuid(), bag);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastBagSync(node));
    }

    /**
     * Test that a communication error happened with the local server
     * At the end of the test LastSync should be the epoch
     */
    @Test
    public void testBagLocalIOE() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        ImmutableMap<String, String> params = ImmutableMap.of(
                "admin_node", node,
                "after", epoch);
        when(remoteBag.getBags(params)).thenReturn(new SuccessfulCall<>(responseWrapper(bag)));
        when(localBag.getBag(bag.getUuid()))
                .thenReturn(new ExceptedCall<>(bag));
        when(localBag.createBag(bag))
                .thenReturn(new ExceptedCall<>(bag));
        synchronizer.readLastSync();
        synchronizer.syncBags();

        blockUnitShutdown();

        verify(remoteBag, times(1)).getBags(params);
        verify(localBag, times(1)).getBag(bag.getUuid());
        verify(localBag, times(1)).createBag(bag);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastBagSync(node));
    }

    /**
     * Test that a non successful response was returned from the local server
     * At the end of the test LastSync should be the epoch
     */
    @Test
    public void testBagLocalFailure() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        ImmutableMap<String, String> params = ImmutableMap.of(
                "admin_node", node,
                "after", epoch);
        when(remoteBag.getBags(params)).thenReturn(new SuccessfulCall<>(responseWrapper(bag)));
        when(localBag.getBag(bag.getUuid()))
                .thenReturn(new SuccessfulCall<>(bag));
        when(localBag.updateBag(bag.getUuid(), bag))
                .thenReturn(new FailedCall<>(bag));
        synchronizer.readLastSync();
        synchronizer.syncBags();

        blockUnitShutdown();

        verify(remoteBag, times(1)).getBags(params);
        verify(localBag, times(1)).getBag(bag.getUuid());
        verify(localBag, times(1)).updateBag(bag.getUuid(), bag);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastBagSync(node));
    }

    @Test
    public void testReplicationSuccess() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        ImmutableMap<String, String> params = ImmutableMap.of(
                "from_node", node,
                "after", epoch);
        when(remoteTransfer.getReplications(params))
                .thenReturn(new SuccessfulCall<>(responseWrapper(replication)));
        when(localTransfer.getReplication(replication.getReplicationId()))
                .thenReturn(new SuccessfulCall<>(replication));
        when(localTransfer.updateReplication(replication.getReplicationId(), replication))
                .thenReturn(new SuccessfulCall<>(replication));
        synchronizer.readLastSync();
        synchronizer.syncTransfers();

        blockUnitShutdown();

        verify(remoteTransfer, times(1)).getReplications(params);
        verify(localTransfer, times(1)).getReplication(replication.getReplicationId());
        verify(localTransfer, times(1)).updateReplication(replication.getReplicationId(), replication);
        Assert.assertNotEquals(epoch, synchronizer.lastSync.lastReplicationSync(node));
    }

    @Test
    public void testReplicationRemoteException() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        ImmutableMap<String, String> params = ImmutableMap.of(
                "from_node", node,
                "after", epoch);
        when(remoteTransfer.getReplications(params))
                .thenReturn(new ExceptedCall<>(responseWrapper(replication)));
        synchronizer.readLastSync();
        synchronizer.syncTransfers();

        blockUnitShutdown();

        verify(remoteTransfer, times(1)).getReplications(params);
        verify(localTransfer, times(0)).getReplication(replication.getReplicationId());
        verify(localTransfer, times(0)).updateReplication(replication.getReplicationId(), replication);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastReplicationSync(node));
    }

    @Test
    public void testReplicationRemoteFailure() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        ImmutableMap<String, String> params = ImmutableMap.of(
                "from_node", node,
                "after", epoch);
        when(remoteTransfer.getReplications(params))
                .thenReturn(new FailedCall<>(responseWrapper(replication)));
        synchronizer.readLastSync();
        synchronizer.syncTransfers();

        blockUnitShutdown();

        verify(remoteTransfer, times(1)).getReplications(params);
        verify(localTransfer, times(0)).getReplication(replication.getReplicationId());
        verify(localTransfer, times(0)).updateReplication(replication.getReplicationId(), replication);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastReplicationSync(node));
    }

    @Test
    public void testReplicationLocalException() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        ImmutableMap<String, String> params = ImmutableMap.of(
                "from_node", node,
                "after", epoch);
        when(remoteTransfer.getReplications(params))
                .thenReturn(new SuccessfulCall<>(responseWrapper(replication)));
        when(localTransfer.getReplication(replication.getReplicationId()))
                .thenReturn(new ExceptedCall<>(replication));
        when(localTransfer.createReplication(replication))
                .thenReturn(new ExceptedCall<>(replication));
        synchronizer.readLastSync();
        synchronizer.syncTransfers();

        blockUnitShutdown();

        verify(remoteTransfer, times(1)).getReplications(params);
        verify(localTransfer, times(1)).getReplication(replication.getReplicationId());
        verify(localTransfer, times(1)).createReplication(replication);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastReplicationSync(node));
    }

    @Test
    public void testReplicationLocalFailure() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        ImmutableMap<String, String> params = ImmutableMap.of(
                "from_node", node,
                "after", epoch);
        when(remoteTransfer.getReplications(params))
                .thenReturn(new SuccessfulCall<>(responseWrapper(replication)));
        when(localTransfer.getReplication(replication.getReplicationId()))
                .thenReturn(new SuccessfulCall<>(replication));
        when(localTransfer.updateReplication(replication.getReplicationId(), replication))
                .thenReturn(new FailedCall<>(replication));
        synchronizer.readLastSync();
        synchronizer.syncTransfers();

        blockUnitShutdown();

        verify(remoteTransfer, times(1)).getReplications(params);
        verify(localTransfer, times(1)).getReplication(replication.getReplicationId());
        verify(localTransfer, times(1)).updateReplication(replication.getReplicationId(), replication);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastReplicationSync(node));
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

    private class ExceptedCall<T> extends SuccessfulCall<T> {
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

    private class FailedCall<T> extends SuccessfulCall<T> {
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