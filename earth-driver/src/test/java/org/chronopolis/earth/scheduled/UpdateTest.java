package org.chronopolis.earth.scheduled;

import org.chronopolis.earth.EarthSettings;
import org.chronopolis.earth.models.Replication;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.ZonedDateTime;

import static org.chronopolis.earth.scheduled.SynchronizerTest.responseWrapper;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Test the 'update' in the Downloader class
 *
 * Created by shake on 8/11/16.
 */
public class UpdateTest extends DownloaderTest {

    @Test
    public void update() throws Exception {
        EarthSettings settings = new EarthSettings();
        settings.setLogChron(true);
        settings.setStage(bagLink.toString());

        downloader = new Downloader(settings, chronopolis, remotes, factory);
        String replicationId = "update-success";
        Replication r = createReplication(replicationId, false, false);
        saveNewFlow(r, false, true, false, false);
        ZonedDateTime past = r.getUpdatedAt();
        String fixity = r.getFixityValue();

        // TODO: Move SuccessfulCall/ResponseWrapper somewhere
        when(transfer.getReplications(anyMap())).thenReturn(new SuccessfulCall<>(responseWrapper(r)));
        when(transfer.updateReplication(replicationId, r)).thenReturn(new SuccessfulCall<>(r));
        downloader.requested();
        String updated = r.getFixityValue();

        // verifies...
        Mockito.verify(transfer, times(1)).updateReplication(replicationId, r);
        Assert.assertNotEquals("Fixity has been updated", fixity, updated);
        Assert.assertFalse("Transfer is not cancelled", r.isCancelled());
        Assert.assertTrue("Transfer updated_at has been updated", past.isBefore(r.getUpdatedAt()));
    }

    @Test
    public void failureIO() throws Exception {
        EarthSettings settings = new EarthSettings();
        settings.setLogChron(true);
        settings.setStage(bagLink.getParent().toString());

        downloader = new Downloader(settings, chronopolis, remotes, factory);
        String replicationId = "update-failure-io";
        Replication r = createReplication(replicationId, false, false);
        saveNewFlow(r, false, true, false, false);
        ZonedDateTime past = r.getUpdatedAt();

        when(transfer.getReplications(anyMap())).thenReturn(new SuccessfulCall<>(responseWrapper(r)));
        // when(transfer.updateReplication(replicationId, r)).thenReturn(new SuccessfulCall<>(r));
        downloader.requested();
        String updated = r.getFixityValue();

        // verifies...
        Mockito.verify(transfer, times(0)).updateReplication(replicationId, r);
        Assert.assertNull("Fixity has not been updated", updated);
        Assert.assertFalse("Transfer is not cancelled", r.isCancelled());
        Assert.assertEquals("Transfer updated_at has not been updated", past, r.getUpdatedAt());
    }

    @Test
    public void failureBadHash() throws Exception {
        EarthSettings settings = new EarthSettings();
        settings.setLogChron(true);
        settings.setStage(bagLink.toString());

        downloader = new Downloader(settings, chronopolis, remotes, factory);
        String replicationId = "update-failure-hash";
        Replication r = createReplication(replicationId, false, false);
        saveNewFlow(r, false, true, false, false);
        r.setBag(invalid);
        ZonedDateTime past = r.getUpdatedAt();

        when(transfer.getReplications(anyMap())).thenReturn(new SuccessfulCall<>(responseWrapper(r)));
        when(transfer.updateReplication(replicationId, r)).thenReturn(new SuccessfulCall<>(r));
        downloader.requested();
        String updated = r.getFixityValue();

        // verifies...
        Mockito.verify(transfer, times(1)).updateReplication(replicationId, r);
        Assert.assertNull("Fixity has not been updated", updated);
        Assert.assertTrue("Transfer is cancelled", r.isCancelled());
        Assert.assertTrue("Transfer updated_at has been updated", past.isBefore(r.getUpdatedAt()));
    }
}
