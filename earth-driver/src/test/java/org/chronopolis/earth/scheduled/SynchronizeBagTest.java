package org.chronopolis.earth.scheduled;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.chronopolis.earth.domain.LastSync;
import org.chronopolis.earth.domain.Sync;
import org.chronopolis.earth.domain.SyncType;
import org.chronopolis.earth.models.Bag;
import org.chronopolis.earth.models.SumResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test synchronization tasks on bags
 *
 * Created by shake on 5/11/16.
 */
public class SynchronizeBagTest extends SynchronizerTest {

    private Bag bag;
    private ImmutableMap<String, String> params = ImmutableMap.of(
                "admin_node", node,
                "after", epoch.format(DateTimeFormatter.ISO_INSTANT),
                "page", "1");

    @Before
    public void setup() {
        super.setup();

        // Setup our bag
        bag = setupBag();
    }

    private Bag setupBag() {
        String uuid = UUID.randomUUID().toString();
        Bag b = new Bag();
        b.setAdminNode(node);
        b.setIngestNode(node);
        b.setBagType('D');
        b.setCreatedAt(ZonedDateTime.now());
        b.setUpdatedAt(ZonedDateTime.now());
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

    private void verifyBagMocks(int remoteGetTimes, int localGetTimes, int localCreateTimes, int localUpdateTimes) {
        verify(remoteBag, times(remoteGetTimes)).getBags(any());
        verify(localBag, times(localGetTimes)).getBag(bag.getUuid());
        verify(localBag, times(localCreateTimes)).createBag(bag);
        verify(localBag, times(localUpdateTimes)).updateBag(bag.getUuid(), bag);
    }

    // I'm not a wrapper
    static <T> SumResponse<T> responseRapper(T t) {
        SumResponse<T> remoteResponse = new SumResponse<>();
        remoteResponse.setCount(1);
        remoteResponse.setTotalSize(0L);
        remoteResponse.setNext(null);
        remoteResponse.setPrevious(null);
        remoteResponse.setResults(ImmutableList.of(t));
        return remoteResponse;
    }

    @Test
    public void testSyncMultiple() throws InterruptedException {
        Bag b1 = setupBag();
        Bag b2 = setupBag();
        Bag b3 = setupBag();
        SumResponse<Bag> multi = new SumResponse<>();
        multi.setCount(3);
        multi.setNext(null);
        multi.setPrevious(null);
        multi.setResults(ImmutableList.of(b1, b2, b3));

        when(remoteBag.getBags(any()))
                .thenReturn(new SuccessfulCall<>(multi));
        when(localBag.getBag(b1.getUuid()))
                .thenReturn(new SuccessfulCall<>(b1));
        when(localBag.getBag(b2.getUuid()))
                .thenReturn(new SuccessfulCall<>(b2));
        when(localBag.getBag(b3.getUuid()))
                .thenReturn(new SuccessfulCall<>(b3));
        when(localBag.updateBag(b1.getUuid(), b1))
                .thenReturn(new SuccessfulCall<>(b1));
        when(localBag.updateBag(b2.getUuid(), b2))
                .thenReturn(new SuccessfulCall<>(b2));
        when(localBag.updateBag(b3.getUuid(), b3))
                .thenReturn(new SuccessfulCall<>(b3));

        synchronizer.syncBags(remoteBag, node, new Sync());

        verify(localBag, times(3)).getBag(any());
        // TODO: Verify our last sync
    }

    /**
     * Test that a bag was sync'd with no errors
     * At the end of the test LastSync should be today
     */
    @Test
    public void testBagSuccessfulSync() throws InterruptedException {
        when(remoteBag.getBags(params))
                .thenReturn(new SuccessfulCall<>(responseRapper(bag)));
        when(localBag.getBag(bag.getUuid()))
                .thenReturn(new SuccessfulCall<>(bag));
        when(localBag.updateBag(bag.getUuid(), bag))
                .thenReturn(new SuccessfulCall<>(bag));
        synchronizer.syncBags(remoteBag, node, new Sync());

        verifyBagMocks(1, 1, 0, 1);
        LastSync lastSync = getLastSync(node, SyncType.BAG);
        Assert.assertNotNull(lastSync);
        Assert.assertNotEquals(epoch, lastSync.getTime());
    }

    /**
     * Test that a communication error occurred with the remote server
     * At the end of the test LastSync should be the epoch
     */
    @Test
    public void testBagRemoteException() throws InterruptedException {
        when(remoteBag.getBags(params)).thenReturn(new ExceptedCall<>(responseRapper(bag)));
        synchronizer.syncBags(remoteBag, node, new Sync());

        verifyBagMocks(1, 0, 0, 0);
        LastSync lastSync = getLastSync(node, SyncType.BAG);
        Assert.assertNotNull(lastSync);
        Assert.assertEquals(epoch, lastSync.getTime());
    }

    /**
     * Test that a non successful response was returned from the remote server
     * At the end of the test LastSync should be the epoch
     */
    @Test
    public void testBagRemoteFailure() throws InterruptedException {
        when(remoteBag.getBags(params)).thenReturn(new FailedCall<>(responseRapper(bag)));
        synchronizer.syncBags(remoteBag, node, new Sync());


        verifyBagMocks(1, 0, 0, 0);
        LastSync lastSync = getLastSync(node, SyncType.BAG);
        Assert.assertNotNull(lastSync);
        Assert.assertEquals(epoch, lastSync.getTime());
    }

    /**
     * Test that a communication error happened with the local server
     * At the end of the test LastSync should be the epoch
     */
    @Test
    public void testBagLocalException() throws InterruptedException {
        when(remoteBag.getBags(params)).thenReturn(new SuccessfulCall<>(responseRapper(bag)));
        when(localBag.getBag(bag.getUuid()))
                .thenReturn(new ExceptedCall<>(bag));
        when(localBag.createBag(bag))
                .thenReturn(new ExceptedCall<>(bag));
        synchronizer.syncBags(remoteBag, node, new Sync());


        verifyBagMocks(1, 1, 1, 0);
        LastSync lastSync = getLastSync(node, SyncType.BAG);
        Assert.assertNotNull(lastSync);
        Assert.assertEquals(epoch, lastSync.getTime());
    }

    /**
     * Test that a non successful response was returned from the local server
     * At the end of the test LastSync should be the epoch
     */
    @Test
    public void testBagLocalFailure() throws InterruptedException {
        when(remoteBag.getBags(params)).thenReturn(new SuccessfulCall<>(responseRapper(bag)));
        when(localBag.getBag(bag.getUuid()))
                .thenReturn(new SuccessfulCall<>(bag));
        when(localBag.updateBag(bag.getUuid(), bag))
                .thenReturn(new FailedCall<>(bag));
        synchronizer.syncBags(remoteBag, node, new Sync());


        verifyBagMocks(1, 1, 0, 1);
        LastSync lastSync = getLastSync(node, SyncType.BAG);
        Assert.assertNotNull(lastSync);
        Assert.assertEquals(epoch, lastSync.getTime());
    }
}
