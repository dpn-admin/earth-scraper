package org.chronopolis.earth.scheduled;

import org.chronopolis.earth.EarthSettings;
import org.chronopolis.earth.config.Ingest;
import org.chronopolis.earth.domain.ReplicationFlow;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.IngestRequest;
import org.junit.Assert;
import org.junit.Test;

import static org.chronopolis.earth.scheduled.SynchronizerTest.responseWrapper;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the 'push' operation of the downloader class
 *
 * Created by shake on 8/11/16.
 */
public class PushTest extends DownloaderTest {

    @Test
    public void push() throws Exception {
        // Setup our downloader object
        EarthSettings settings = new EarthSettings();
        settings.setLogChron(true);
        settings.setIngest(new Ingest().setNode("ucsd-dpn"));

        downloader = new Downloader(settings, chronopolis, apis, sql2o);

        Bag b = new Bag("5ayadda", "mock-node");
        String id = "push-success";
        Replication r = createReplication(id, true, false);
        ReplicationFlow flow = ReplicationFlow.get(r, sql2o);
        flow.setExtracted(true);
        flow.setReceived(true);
        flow.setValidated(true);
        flow.save(sql2o);
        when(transfer.getReplications(anyMap())).thenReturn(new SuccessfulCall<>(responseWrapper(r)));
        when(chronopolis.stageBag(any(IngestRequest.class))).thenReturn(new SuccessfulCall<>(b));
        downloader.received();

        flow = ReplicationFlow.get(r, sql2o);
        verify(chronopolis, times(1)).stageBag(any(IngestRequest.class));
        Assert.assertTrue("ReplicationFlow isPushed", flow.isPushed());
    }

    @Test
    public void failure() throws Exception {
        // Setup our downloader object
        EarthSettings settings = new EarthSettings();
        settings.setLogChron(true);
        settings.setIngest(new Ingest().setNode("ucsd-dpn"));

        downloader = new Downloader(settings, chronopolis, apis, sql2o);

        Bag b = new Bag("5ayadda", "mock-node");
        String id = "push-failure";
        Replication r = createReplication(id, true, false);
        ReplicationFlow flow = ReplicationFlow.get(r, sql2o);
        flow.setExtracted(true);
        flow.setReceived(true);
        flow.setValidated(true);
        flow.save(sql2o);
        when(transfer.getReplications(anyMap())).thenReturn(new SuccessfulCall<>(responseWrapper(r)));
        when(chronopolis.stageBag(any(IngestRequest.class))).thenReturn(new FailedCall<>(b));
        downloader.received();

        flow = ReplicationFlow.get(r, sql2o);
        verify(chronopolis, times(1)).stageBag(any(IngestRequest.class));
        Assert.assertFalse("ReplicationFlow is not pushed", flow.isPushed());
    }
}
