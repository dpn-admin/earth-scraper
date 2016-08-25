package org.chronopolis.earth.scheduled;

import org.chronopolis.earth.EarthSettings;
import org.chronopolis.earth.domain.ReplicationFlow;
import org.chronopolis.earth.models.Replication;
import org.junit.Assert;
import org.junit.Test;

import static org.chronopolis.earth.scheduled.SynchronizerTest.responseWrapper;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.when;

/**
 * Test the untar functionality for the Downloader class
 *
 * Created by shake on 8/11/16.
 */
public class UntarTest extends DownloaderTest {

    @Test
    public void untar() throws Exception {
        EarthSettings settings = new EarthSettings();
        settings.setStage(bagLink.toString());

        downloader = new Downloader(settings, chronopolis, apis, sql2o);
        String id = "untar-success";
        Replication r = createReplication(id, false, false);
        ReplicationFlow flow = ReplicationFlow.get(r, sql2o);
        flow.setReceived(true);
        flow.save(sql2o);
        when(transfer.getReplications(anyMap())).thenReturn(new SuccessfulCall<>(responseWrapper(r)));
        downloader.received();

        // pull again
        flow = ReplicationFlow.get(r, sql2o);
        Assert.assertTrue("tarball has been extracted", flow.isExtracted());
        // java.nio.file.Files.deleteIfExists(bagLink.getParent().resolve("599b663d-b5e8-4f56-b332-4418eac0f8f2"));
    }

    @Test
    public void failure() throws Exception {
        EarthSettings settings = new EarthSettings();
        settings.setStage(bagExtracted.getParent().toString());

        downloader = new Downloader(settings, chronopolis, apis, sql2o);
        String id = "untar-failure";
        Replication r = createReplication(id, false, false);
        ReplicationFlow flow = ReplicationFlow.get(r, sql2o);
        flow.setReceived(true);
        flow.save(sql2o);
        when(transfer.getReplications(anyMap())).thenReturn(new SuccessfulCall<>(responseWrapper(r)));
        downloader.received();

        // pull again
        flow = ReplicationFlow.get(r, sql2o);
        Assert.assertFalse("tarball has not been extracted", flow.isExtracted());
    }
}
