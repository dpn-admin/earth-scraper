package org.chronopolis.earth.scheduled;

import com.google.common.io.Files;
import org.chronopolis.earth.EarthSettings;
import org.chronopolis.earth.domain.ReplicationFlow;
import org.chronopolis.earth.models.Replication;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static org.chronopolis.earth.scheduled.SynchronizerTest.responseWrapper;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.when;

/**
 * Test the download(rsync) functionality of the downloader class
 *
 * Created by shake on 8/11/16.
 */
public class DownloadTest extends DownloaderTest {
    private static File tmp;

    @BeforeClass
    public static void setupTmp() {
        tmp = Files.createTempDir();
    }

    @AfterClass
    public static void teardownTmp() throws IOException {
        java.nio.file.Files.walkFileTree(tmp.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                java.nio.file.Files.delete(path);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
                java.nio.file.Files.delete(path);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Test
    public void download() throws Exception {
        EarthSettings settings = new EarthSettings();
        settings.setStage(tmp.toString());

        downloader = new Downloader(settings, chronopolis, remotes, factory);

        String replicationId = "download-success";
        Replication dlSuccess = createReplication(replicationId, false, false);

        // TODO: Move SuccessfulCall/ResponseWrapper somewhere
        when(transfer.getReplications(anyMap())).thenReturn(new SuccessfulCall<>(responseWrapper(dlSuccess)));
        downloader.requested();

        // verifies...
        ReplicationFlow flow = getFlow("download-success");
        Assert.assertNotNull("ReplicationFlow exists", flow);
        Assert.assertTrue("Replication has been received", flow.isReceived());
    }

    @Test
    public void failure() throws Exception {
        EarthSettings settings = new EarthSettings();
        settings.setStage(tmp.toString());

        downloader = new Downloader(settings, chronopolis, remotes, factory);

        String replicationId = "download-failure";
        Replication r = createReplication(replicationId, "not-a-valid-link", false, false);

        when(transfer.getReplications(anyMap())).thenReturn(new SuccessfulCall<>(responseWrapper(r)));
        downloader.requested();

        // verifies...
        ReplicationFlow flow = getFlow("download-failure");
        Assert.assertNotNull("ReplicationFlow exists", flow);
        Assert.assertFalse("Replication has not been received", flow.isReceived());
    }
}
