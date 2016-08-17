package org.chronopolis.earth.scheduled;

import org.chronopolis.earth.models.Digest;
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
public class SynchronizeDigestTest extends SynchronizerTest {
    Digest d;

    @Before
    public void setup() {
        super.setup();

        d = new Digest();
        d.setAlgorithm("sha256");
        d.setBag(UUID.randomUUID().toString());
        d.setCreatedAt(ZonedDateTime.now());
        d.setNode("mock-node");
        d.setValue("digest-value");
    }

    @Test
    public void digest() {
        when(remoteEvents.getDigests(anyMap())).thenReturn(new SuccessfulCall(responseWrapper(d)));
        when(localBag.createDigest(d.getBag(), d)).thenReturn(new SuccessfulCall<>(d));

        synchronizer.readLastSync();
        synchronizer.syncDigests();

        String lastString = synchronizer.lastSync.lastDigestSync(node);
        ZonedDateTime last = ZonedDateTime.parse(lastString);
        ZonedDateTime start = ZonedDateTime.parse(epoch);

        verify(localBag, times(1)).createDigest(d.getBag(), d);
        Assert.assertTrue("LastSync was updated", last.isAfter(start));
    }

}
