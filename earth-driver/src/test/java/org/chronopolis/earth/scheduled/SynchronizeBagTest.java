package org.chronopolis.earth.scheduled;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import org.chronopolis.earth.models.Bag;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.Executors;

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
                "after", epoch);

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

    private void verifyBagMocks(int remoteGetTimes, int localGetTimes, int localCreateTimes, int localUpdateTimes) {
        verify(remoteBag, times(remoteGetTimes)).getBags(params);
        verify(localBag, times(localGetTimes)).getBag(bag.getUuid());
        verify(localBag, times(localCreateTimes)).createBag(bag);
        verify(localBag, times(localUpdateTimes)).updateBag(bag.getUuid(), bag);
    }

    /**
     * Test that a bag was sync'd with no errors
     * At the end of the test LastSync should be today
     */
    @Test
    public void testBagSuccessfulSync() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        when(remoteBag.getBags(params))
                .thenReturn(new SuccessfulCall<>(responseWrapper(bag)));
        when(localBag.getBag(bag.getUuid()))
                .thenReturn(new SuccessfulCall<>(bag));
        when(localBag.updateBag(bag.getUuid(), bag))
                .thenReturn(new SuccessfulCall<>(bag));
        synchronizer.readLastSync();
        synchronizer.syncBags();

        blockUnitShutdown();
        verifyBagMocks(1, 1, 0, 1);
        Assert.assertNotEquals(epoch, synchronizer.lastSync.lastBagSync(node));
    }

    /**
     * Test that a communication error occurred with the remote server
     * At the end of the test LastSync should be the epoch
     */
    @Test
    public void testBagRemoteException() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        when(remoteBag.getBags(params)).thenReturn(new ExceptedCall<>(responseWrapper(bag)));
        synchronizer.readLastSync();
        synchronizer.syncBags();

        blockUnitShutdown();
        verifyBagMocks(1, 0, 0, 0);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastBagSync(node));
    }

    /**
     * Test that a non successful response was returned from the remote server
     * At the end of the test LastSync should be the epoch
     */
    @Test
    public void testBagRemoteFailure() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        when(remoteBag.getBags(params)).thenReturn(new FailedCall<>(responseWrapper(bag)));
        synchronizer.readLastSync();
        synchronizer.syncBags();

        blockUnitShutdown();

        verifyBagMocks(1, 0, 0, 0);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastBagSync(node));
    }

    /**
     * Test that a communication error happened with the local server
     * At the end of the test LastSync should be the epoch
     */
    @Test
    public void testBagLocalException() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        when(remoteBag.getBags(params)).thenReturn(new SuccessfulCall<>(responseWrapper(bag)));
        when(localBag.getBag(bag.getUuid()))
                .thenReturn(new ExceptedCall<>(bag));
        when(localBag.createBag(bag))
                .thenReturn(new ExceptedCall<>(bag));
        synchronizer.readLastSync();
        synchronizer.syncBags();

        blockUnitShutdown();

        verifyBagMocks(1, 1, 1, 0);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastBagSync(node));
    }

    /**
     * Test that a non successful response was returned from the local server
     * At the end of the test LastSync should be the epoch
     */
    @Test
    public void testBagLocalFailure() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        when(remoteBag.getBags(params)).thenReturn(new SuccessfulCall<>(responseWrapper(bag)));
        when(localBag.getBag(bag.getUuid()))
                .thenReturn(new SuccessfulCall<>(bag));
        when(localBag.updateBag(bag.getUuid(), bag))
                .thenReturn(new FailedCall<>(bag));
        synchronizer.readLastSync();
        synchronizer.syncBags();

        blockUnitShutdown();

        verifyBagMocks(1, 1, 0, 1);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastBagSync(node));
    }
}
