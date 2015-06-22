package org.chronopolis.earth.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * File visitor which searches for bags
 *
 * Created by shake on 6/19/15.
 */
public class BagVisitor extends SimpleFileVisitor<Path> {
    private final Logger log = LoggerFactory.getLogger(BagVisitor.class);

    private final int DEPTH_NODE = 1;
    private final int DEPTH_BAG = 2;

    private int depth;
    private BalustradeTransfers current;

    private final TransferAPIs apis;
    private final Multimap<String, Path> bags;

    public BagVisitor(TransferAPIs apis) {
        this.depth = 0;
        this.apis = apis;
        this.current = null;
        this.bags = HashMultimap.create();
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attributes) throws IOException {
        FileVisitResult result = FileVisitResult.CONTINUE;
        String dir = path.getFileName().toString();
        log.trace("[{}] Visiting {}", depth, dir);

        if (depth == DEPTH_NODE) {
            if (!apis.getApiMap().containsKey(dir)) {
                log.debug("Skipping {}", dir);

                // roll back our depth increment
                depth--;
                result = FileVisitResult.SKIP_SUBTREE;
            } else {
                current = apis.getApiMap().get(dir);
            }
        }

        depth++;
        return result;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) throws IOException {
        if (depth > DEPTH_BAG) {
            log.debug("Dove too far, aborting");
            return FileVisitResult.SKIP_SIBLINGS;
        }

        // Kind of funny how we check if it's a directory in visitFile, but it be how it be
        // Ignore tarballs, just get the directory names
        if (path.toFile().isDirectory()) {
            if (stateIsTerminal(path)) {
                log.debug("{} is in terminal state", path.getFileName());
                bags.put(path.getParent().getFileName().toString(), path);
            }
        }

        return FileVisitResult.CONTINUE;
    }

    private boolean stateIsTerminal(Path path) {
        if (current == null) {
            return false;
        }

        boolean terminal;
        ImmutableMap<String, String> params =
                ImmutableMap.of("uuid",     path.getFileName().toString(),
                                "order_by", "updated_at");
        Response<Replication> replications = current.getReplications(params);

        if (replications.getResults().isEmpty()) {
            // No record... ???
            terminal = false;
        } else {
            Replication replication = replications.getResults().get(0);

            // Check the state of the replication
            terminal = replication.status() == Replication.Status.STORED
                    || replication.status() == Replication.Status.CANCELLED
                    || replication.status() == Replication.Status.REJECTED;
        }

        return terminal;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path path, IOException exc) {
        depth--;
        current = null;
        return FileVisitResult.CONTINUE;
    }

    public Multimap<String, Path> getBags() {
        return bags;
    }
}
