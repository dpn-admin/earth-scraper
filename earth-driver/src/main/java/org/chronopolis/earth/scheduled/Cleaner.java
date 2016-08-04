package org.chronopolis.earth.scheduled;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.chronopolis.earth.EarthSettings;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.util.BagVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Clean up any used resources which no longer need to be held
 *
 * Created by shake on 5/18/15.
 */
@Component
@Profile("clean")
@EnableScheduling
public class Cleaner {
    private final Logger log = LoggerFactory.getLogger(Cleaner.class);

    @Autowired
    EarthSettings settings;

    @Autowired
    TransferAPIs transferAPIs;

    @Scheduled(cron = "*/30 * * * * *")
    void clean() {
        Path stage = Paths.get(settings.getStage());
        BagVisitor visitor = new BagVisitor(transferAPIs);
        Set<FileVisitOption> options =
                ImmutableSet.of(FileVisitOption.FOLLOW_LINKS);

        try {
            Files.walkFileTree(stage, options, 2, visitor);
            Multimap<String, Path> bags = visitor.getBags();
            for (String node : bags.keySet()) {
                for (Path path : bags.get(node)) {
                    log.info("[{}] Removing directory for {}", node, path.getFileName());
                }
            }

        } catch (IOException e) {
            log.error("", e);
        }
    }

    private void clean(Replication replication) {
        String from = replication.getFromNode();
        String uuid = replication.getBag();

        Path bag = Paths.get(settings.getStage(), from, uuid);
        if (bag.toFile().exists()) {
            log.info("Removing cancelled transfer {}::{}", from, uuid);
        }
    }

}
