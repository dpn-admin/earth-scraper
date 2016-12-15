package org.chronopolis.earth.scheduled;

import org.chronopolis.earth.EarthSettings;
import org.chronopolis.earth.domain.ReplicationFlow;
import org.chronopolis.earth.models.Replication;
import org.junit.Assert;
import org.junit.Test;

import static org.chronopolis.earth.scheduled.SynchronizerTest.responseWrapper;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test 'validate' in the Downloader class
 *
 * Created by shake on 8/11/16.
 */
public class ValidateTest extends DownloaderTest {


    @Test
    public void validate() throws Exception {
        EarthSettings settings = new EarthSettings();
        settings.setStage(bagExtracted.toString());

        downloader = new Downloader(settings, chronopolis, remotes, factory);
        String id = "validate-success";
        Replication r = createReplication(id, false, false);
        saveNewFlow(r, true, true, false, false);
        when(transfer.getReplications(anyMap())).thenReturn(new SuccessfulCall<>(responseWrapper(r)));
        downloader.received();

        // pull again
        ReplicationFlow flow = getFlow(id);
        Assert.assertTrue("bag has been validated", flow.isValidated());
    }

    @Test
    public void failureBadHash() throws Exception {
        EarthSettings settings = new EarthSettings();
        settings.setStage(bagExtracted.toString());

        downloader = new Downloader(settings, chronopolis, remotes, factory);
        String id = "validate-failure-hash";
        Replication r = createReplication(id, false, false);
        saveNewFlow(r, true, true, false, false);
        r.setBag(invalid);
        when(transfer.getReplications(anyMap())).thenReturn(new SuccessfulCall<>(responseWrapper(r)));
        when(transfer.updateReplication(id, r)).thenReturn(new SuccessfulCall<>(r));
        downloader.received();

        // pull again
        ReplicationFlow flow = getFlow(id);
        Assert.assertFalse("bag is not valid", flow.isValidated());
        Assert.assertTrue("replication is cancelled", r.isCancelled());
        verify(transfer, times(1)).updateReplication(id, r);
    }

    @Test
    public void failureIO() throws Exception {
        EarthSettings settings = new EarthSettings();
        settings.setStage(bagExtracted.getParent().toString());

        downloader = new Downloader(settings, chronopolis, remotes, factory);
        String id = "validate-failure-io";
        Replication r = createReplication(id, false, false);
        saveNewFlow(r, true, true, false, false);
        when(transfer.getReplications(anyMap())).thenReturn(new SuccessfulCall<>(responseWrapper(r)));
        when(transfer.updateReplication(id, r)).thenReturn(new SuccessfulCall<>(r));
        downloader.received();

        // pull again
        ReplicationFlow flow = getFlow(id);
        Assert.assertFalse("bag is not valid", flow.isValidated());
        Assert.assertTrue("replication is cancelled", r.isCancelled());
        verify(transfer, times(1)).updateReplication(id, r);
    }

    // TODO: Test invalid manifest with line: hash file comment
}
