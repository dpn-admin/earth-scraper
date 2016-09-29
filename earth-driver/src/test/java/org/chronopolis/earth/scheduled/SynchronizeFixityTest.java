package org.chronopolis.earth.scheduled;

import org.chronopolis.earth.domain.LastSync;
import org.chronopolis.earth.domain.SyncType;
import org.chronopolis.earth.models.FixityCheck;
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
public class SynchronizeFixityTest extends SynchronizerTest {
    FixityCheck f;

    @Before
    public void setup() {
        super.setup();

        f = new FixityCheck();
        f.setNode(node)
         .setCreatedAt(ZonedDateTime.now())
         .setBag(UUID.randomUUID().toString())
         .setFixityAt(ZonedDateTime.now())
         .setFixityCheckId(UUID.randomUUID().toString())
         .setSuccess(true);
    }

    @Test
    public void createFixity() {
        when(remoteEvents.getFixityChecks(anyMap())).thenReturn(new SuccessfulCall(responseWrapper(f)));
        when(localEvents.createFixityCheck(f)).thenReturn(new SuccessfulCall<>(f));

        synchronizer.syncFixities();

        verify(localEvents, times(1)).createFixityCheck(f);

        LastSync lastSync = getLastSync(node, SyncType.FIXITY);
        Assert.assertNotNull(lastSync);

        ZonedDateTime last = lastSync.getTime();
        Assert.assertTrue("LastSync was updated", last.isAfter(epoch));
    }

}
