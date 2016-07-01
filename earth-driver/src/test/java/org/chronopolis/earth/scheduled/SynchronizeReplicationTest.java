package org.chronopolis.earth.scheduled;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import org.chronopolis.earth.models.Replication;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.Executors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for replication synchronization
 *
 * Created by shake on 5/11/16.
 */
public class SynchronizeReplicationTest extends SynchronizerTest {

    private Replication replication;
    private ImmutableMap<String, String> params = ImmutableMap.of(
            "from_node", node,
            "after", epoch,
            "page", String.valueOf(1));

    @Before
    public void setup() {
        super.setup();

        replication = setupReplication();
    }

    private Replication setupReplication() {
        String uuid = UUID.randomUUID().toString();
        Replication r = new Replication();
        r.setReplicationId(uuid);
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

    private void verifyMocks(int remoteGet, int localGet, int localCreate, int localUpdate) {
        verify(remoteTransfer, times(remoteGet)).getReplications(any());
        verify(localTransfer, times(localGet)).getReplication(replication.getReplicationId());
        verify(localTransfer, times(localCreate)).createReplication(replication);
        verify(localTransfer, times(localUpdate)).updateReplication(replication.getReplicationId(), replication);
    }

    @Test
    public void testReplicationSuccess() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));

        when(remoteTransfer.getReplications(params))
                .thenReturn(new SuccessfulCall<>(responseWrapper(replication)));
        when(localTransfer.getReplication(replication.getReplicationId()))
                .thenReturn(new SuccessfulCall<>(replication));
        when(localTransfer.updateReplication(replication.getReplicationId(), replication))
                .thenReturn(new SuccessfulCall<>(replication));

        synchronizer.readLastSync();
        synchronizer.syncTransfers();
        blockUnitShutdown();

        verifyMocks(1, 1, 0, 1);
        Assert.assertNotEquals(epoch, synchronizer.lastSync.lastReplicationSync(node));
    }

    @Test
    public void testReplicationRemoteException() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));

        when(remoteTransfer.getReplications(params))
                .thenReturn(new ExceptedCall<>(responseWrapper(replication)));

        synchronizer.readLastSync();
        synchronizer.syncTransfers();
        blockUnitShutdown();

        verifyMocks(1, 0, 0, 0);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastReplicationSync(node));
    }

    @Test
    public void testReplicationRemoteFailure() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));

        when(remoteTransfer.getReplications(params))
                .thenReturn(new FailedCall<>(responseWrapper(replication)));

        synchronizer.readLastSync();
        synchronizer.syncTransfers();
        blockUnitShutdown();

        verifyMocks(1, 0, 0, 0);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastReplicationSync(node));
    }

    @Test
    public void testReplicationLocalException() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));

        when(remoteTransfer.getReplications(params))
                .thenReturn(new SuccessfulCall<>(responseWrapper(replication)));
        when(localTransfer.getReplication(replication.getReplicationId()))
                .thenReturn(new ExceptedCall<>(replication));
        when(localTransfer.createReplication(replication))
                .thenReturn(new ExceptedCall<>(replication));

        synchronizer.readLastSync();
        synchronizer.syncTransfers();
        blockUnitShutdown();

        verifyMocks(1, 1, 1, 0);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastReplicationSync(node));
    }

    @Test
    public void testReplicationLocalFailure() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));

        when(remoteTransfer.getReplications(params))
                .thenReturn(new SuccessfulCall<>(responseWrapper(replication)));
        when(localTransfer.getReplication(replication.getReplicationId()))
                .thenReturn(new SuccessfulCall<>(replication));
        when(localTransfer.updateReplication(replication.getReplicationId(), replication))
                .thenReturn(new FailedCall<>(replication));

        synchronizer.readLastSync();
        synchronizer.syncTransfers();
        blockUnitShutdown();

        verifyMocks(1, 1, 0, 1);
        Assert.assertEquals(epoch, synchronizer.lastSync.lastReplicationSync(node));
    }

}
