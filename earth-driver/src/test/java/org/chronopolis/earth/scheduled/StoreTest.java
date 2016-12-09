package org.chronopolis.earth.scheduled;

import com.google.common.collect.ImmutableList;
import org.chronopolis.earth.EarthSettings;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.PageImpl;

import static org.chronopolis.earth.scheduled.SynchronizerTest.responseWrapper;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the 'store' operation of the Downloader class
 *
 * Created by shake on 8/11/16.
 */
public class StoreTest extends DownloaderTest {

    @Test
    public void store() throws Exception {
        // Setup our downloader object
        EarthSettings settings = new EarthSettings();
        settings.setLogChron(true);

        downloader = new Downloader(settings, chronopolis, apis, factory);

        Bag b = new Bag("5ayadda", "mock-node");
        b.setStatus(BagStatus.PRESERVED);
        String replicationId = "store-success";
        Replication r = createReplication(replicationId, true, false);
        saveNewFlow(r, true, true, true, true);
        when(transfer.getReplications(anyMap())).thenReturn(new SuccessfulCall<>(responseWrapper(r)));
        when(chronopolis.getBags(anyMap())).thenReturn(new SuccessfulCall(new PageImpl<>(ImmutableList.of(b))));
        when(transfer.updateReplication(replicationId, r)).thenReturn(new SuccessfulCall<>(r));
        downloader.received();

        verify(transfer, times(1)).updateReplication(replicationId, r);
        // Assert.assertTrue("ReplicationFlow isStored", flow.isPushed());
        Assert.assertTrue("Replication isStored", r.isStored());
    }

    @Test
    public void failChron() throws Exception {
        // Setup our downloader object
        EarthSettings settings = new EarthSettings();
        settings.setLogChron(true);

        downloader = new Downloader(settings, chronopolis, apis, factory);

        String replicationId = "store-fail-chron";
        Replication r = createReplication(replicationId, true, false);
        saveNewFlow(r, true, true, true, true);
        when(transfer.getReplications(anyMap())).thenReturn(new SuccessfulCall<>(responseWrapper(r)));
        when(chronopolis.getBags(anyMap())).thenReturn(new ExceptedCall());
        when(transfer.updateReplication(replicationId, r)).thenReturn(new SuccessfulCall<>(r));
        downloader.received();

        verify(transfer, times(0)).updateReplication(replicationId, r);
        // Assert.assertFalse("ReplicationFlow is not stored", flow.isPushed());
        Assert.assertFalse("Replication is not stored", r.isStored());
    }

    @Test
    public void notPreserved() throws Exception {
        // Setup our downloader object
        EarthSettings settings = new EarthSettings();
        settings.setLogChron(true);

        downloader = new Downloader(settings, chronopolis, apis, factory);

        Bag b = new Bag("5ayadda", "mock-node");
        b.setStatus(BagStatus.REPLICATING);
        String replicationId = "store-not-preserved";
        Replication r = createReplication(replicationId, true, false);
        saveNewFlow(r, true, true, true, true);
        when(transfer.getReplications(anyMap())).thenReturn(new SuccessfulCall<>(responseWrapper(r)));
        when(chronopolis.getBags(anyMap())).thenReturn(new SuccessfulCall(new PageImpl<>(ImmutableList.of(b))));
        when(transfer.updateReplication(replicationId, r)).thenReturn(new SuccessfulCall<>(r));
        downloader.received();

        verify(transfer, times(0)).updateReplication(replicationId, r);
        // Assert.assertFalse("ReplicationFlow is not stored", flow.isPushed());
        Assert.assertFalse("Replication is not stored", r.isStored());
    }
}
