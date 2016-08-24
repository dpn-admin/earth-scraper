package org.chronopolis.earth.scheduled;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import org.chronopolis.earth.models.Node;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * Created by shake on 5/11/16.
 */
public class SynchronizeNodeTest extends SynchronizerTest {

    private Node n;

    @Before
    public void setup() {
        super.setup();

        n = new Node();
        n.setName(node);
        n.setNamespace(node);
        n.setApiRoot("some-api-root");
        n.setCreatedAt(ZonedDateTime.now());
        n.setUpdatedAt(ZonedDateTime.now());
        n.setFixityAlgorithms(ImmutableList.of("sha256"));
        n.setProtocols(ImmutableList.of("rsync"));
        n.setReplicateFrom(ImmutableList.of());
        n.setReplicateTo(ImmutableList.of());
        n.setRestoreFrom(ImmutableList.of());
        n.setRestoreTo(ImmutableList.of());
        n.setSshPubkey("pubkey");
        n.setStorage("test-region", "test-type");
    }

    @Test
    public void testNodeSuccess() throws InterruptedException {
        synchronizer.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));

        when(remoteNode.getNode(n.getNamespace()))
                .thenReturn(new SuccessfulCall<>(n));

        synchronizer.readLastSync();
        synchronizer.syncNode();
        blockUnitShutdown();

        verify(remoteNode, times(1)).getNode(n.getNamespace());
        Assert.assertNotEquals(epoch, synchronizer.lastSync.lastNodeSync(n.getNamespace()));
    }

}
