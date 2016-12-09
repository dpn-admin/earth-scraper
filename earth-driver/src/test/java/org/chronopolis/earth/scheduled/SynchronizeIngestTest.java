package org.chronopolis.earth.scheduled;

import com.google.common.collect.ImmutableList;
import org.chronopolis.earth.domain.LastSync;
import org.chronopolis.earth.domain.Sync;
import org.chronopolis.earth.domain.SyncType;
import org.chronopolis.earth.models.Ingest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * Created by shake on 8/17/16.
 */
public class SynchronizeIngestTest extends SynchronizerTest {

    private Ingest i;

    @Before
    public void setup() {
        super.setup();

        i = new Ingest()
                .setBag(UUID.randomUUID().toString())
                .setCreatedAt(ZonedDateTime.now())
                .setIngested(true)
                .setIngestId(UUID.randomUUID().toString())
                .setReplicatingNodes(ImmutableList.of());
    }

    @Test
    public void createIngest() {
        when(remoteEvents.getIngests(anyMap())).thenReturn(new SuccessfulCall(responseWrapper(i)));
        when(localEvents.createIngest(i)).thenReturn(new SuccessfulCall<>(i));

        synchronizer.syncIngests(remoteEvents, node, new Sync());

        verify(localEvents, times(1)).createIngest(i);

        LastSync lastSync = getLastSync(node, SyncType.INGEST);
        Assert.assertNotNull(lastSync);

        ZonedDateTime last = lastSync.getTime();
        Assert.assertTrue("LastSync was updated", last.isAfter(epoch));
    }
}
