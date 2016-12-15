package org.chronopolis.earth.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.chronopolis.earth.SimpleCallback;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.Remote;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * File visitor which searches for bags
 * <p/>
 * Created by shake on 6/19/15.
 */
public class BagVisitor extends SimpleFileVisitor<Path> {
    private final Logger log = LoggerFactory.getLogger(BagVisitor.class);

    private final int DEPTH_NODE = 1;
    private final int DEPTH_BAG = 2;

    // Keep track of both how deep we currently are and which API endpoint to use
    private int depth;
    private BalustradeTransfers current;

    private final Map<String, Remote> remotes;
    private final Multimap<String, Path> bags;

    public BagVisitor(List<Remote> remotes) {
        this.depth = 0;
        this.current = null;
        this.bags = HashMultimap.create();
        this.remotes = remotes.stream()
                .collect(Collectors.toMap(r -> r.getEndpoint().getName(), r -> r));
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attributes) throws IOException {
        FileVisitResult result = FileVisitResult.CONTINUE;
        String dir = path.getFileName().toString();
        log.trace("[{}] Visiting {}", depth, dir);

        if (depth == DEPTH_NODE) {
            if (!remotes.containsKey(dir)) {
                log.debug("Skipping {}", dir);

                // roll back our depth increment
                depth--;
                result = FileVisitResult.SKIP_SUBTREE;
            } else {
                current = remotes.get(dir).getTransfers();
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
        if (path.toFile().isDirectory() && stateIsTerminal(path)){
            log.debug("{} is in terminal state", path.getFileName());
            bags.put(path.getParent().getFileName().toString(), path);
        }

        return FileVisitResult.CONTINUE;
    }

    /**
     * Determine if a Bag is in a terminal state. In order to do so we query
     * the bag's admin node to get associated replications. We then check
     * against the status to see if we are done with any transfers of the bag.
     *
     * @param path the Path of the bag to check
     * @return true if we are finished replicating, false otherwise
     */
    private boolean stateIsTerminal(Path path) {
        if (current == null) {
            return false;
        }

        boolean terminal;
        ImmutableMap<String, String> params =
                ImmutableMap.of("uuid", path.getFileName().toString(),
                        "order_by", "updated_at");
        SimpleCallback<Response<Replication>> callback = new SimpleCallback<>();
        Call<Response<Replication>> call = current.getReplications(params);
        call.enqueue(callback);
        Optional<Response<Replication>> response = callback.getResponse();

        if (response.isPresent()) {
            Response<Replication> replications = response.get();
            if (replications.getResults().isEmpty()) {
                // No record... don't make any attempt
                terminal = false;
            } else {
                Replication replication = replications.getResults().get(0);

                // Check the state of the replication
                terminal = replication.isStored() || replication.isCancelled();
            }

        } else {
            // http issue, wait for the next time we check
            terminal = false;
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
