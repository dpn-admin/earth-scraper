package org.chronopolis.earth.scheduled;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.chronopolis.earth.SimpleCallback;
import org.chronopolis.earth.api.BagAPIs;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.LocalAPI;
import org.chronopolis.earth.api.NodeAPIs;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.models.Bag;
import org.chronopolis.earth.models.Node;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.chronopolis.earth.util.LastSync;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrofit2.Call;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * TODO: Make sure we only sync items which the remote node is the admin node of
 * TODO: DateTime -> LocalDate
 * <p>
 * <p>
 * Created by shake on 3/31/15.
 */
@Component
@Profile("sync")
@EnableScheduling
public class Synchronizer {

    private static final Logger log = LoggerFactory.getLogger(Synchronizer.class);

    @Autowired
    DateTimeFormatter formatter;

    @Autowired
    BagAPIs bagAPIs;

    @Autowired
    TransferAPIs transferAPIs;

    @Autowired
    NodeAPIs nodeAPIs;

    @Autowired
    LocalAPI local;

    LastSync lastSync;
    ListeningExecutorService service;

    @Scheduled(cron = "${earth.cron.sync:0 0 0 * * *}")
    public void synchronize() {
        service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        readLastSync();
        syncNode();
        syncBags();
        syncTransfers();
        writeLastSync(lastSync);
        service.shutdown();
    }

    void readLastSync() {
        try {
            lastSync = LastSync.read();
        } catch (IOException e) {
            log.error("Unable to read last sync!", e);
            lastSync = new LastSync();
        }
    }

    void writeLastSync(LastSync sync) {
        try {
            sync.write();
        } catch (IOException e) {
            log.error("Unable to write last sync!", e);
        }
    }

    void syncTransfers() {
        BalustradeTransfers transfers = local.getTransfersAPI();

        for (String node : transferAPIs.getApiMap().keySet()) {
            DateTime now = DateTime.now();
            String after = lastSync.lastReplicationSync(node);
            BalustradeTransfers api = transferAPIs.getApiMap().get(node);
            SimpleCallback<Response<Replication>> cb = new SimpleCallback<>();
            Call<Response<Replication>> call = api.getReplications(ImmutableMap.of(
                    "from_node", node,
                    "after", after));

            CompletableFuture<retrofit2.Response<Response<Replication>>> future = CompletableFuture.supplyAsync(() -> http(call), service);
            CompletableFuture<Boolean> complete = future.thenApply(response -> {
                boolean success = response.isSuccess();
                List<Replication> replications = ImmutableList.of();
                if (success) {
                    replications = response.body().getResults();
                } else {

                }
                log.info("[{}]: {} Replications to sync", node, replications.size());
                for (Replication replication : replications) {
                    SimpleCallback<Replication> rcb = new SimpleCallback<>();
                    log.trace("[{}]: Updating replication {}", node, replication.getReplicationId());

                    // First check if the replication exists
                    Call<Replication> syncCall;
                    Call<Replication> get = transfers.getReplication(replication.getReplicationId());
                    get.enqueue(rcb);
                    Optional<Replication> replResponse = rcb.getResponse();

                    if (replResponse.isPresent()) {
                        syncCall = transfers.updateReplication(replication.getReplicationId(), replication);
                    } else {
                        syncCall = transfers.createReplication(replication);
                    }

                    try {
                        retrofit2.Response<Replication> syncResponse = syncCall.execute();
                        if (syncResponse.isSuccess()) {
                            log.info("[{}]: Successfully updated replication {}", node, replication.getReplicationId());
                        } else {
                            success = false;
                        }

                    } catch (IOException e) {
                        success = false;
                        log.error("Error in call", e);
                    }
                }
                return success;
            });

            complete.handle((ok, th) -> {
                if (ok != null && ok) {
                    lastSync.addLastReplication(node, now);
                }
                return true;
            });
        }
    }

    static <T> retrofit2.Response<Response<T>> http(Call<Response<T>> call) {
        // List<T> results = ImmutableList.of();
        retrofit2.Response<Response<T>> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            log.error("", e);
            response = retrofit2.Response.error(503, ResponseBody.create(MediaType.parse("text/plain"), "Server is not available"));
        }

        return response;
    }

    void syncBags() {
        // Temporary placeholder for when we sync
        // we'll want a better way to do this
        LastSync newSyncs = new LastSync();
        BalustradeBag bagAPI = local.getBagAPI();
        Map<String, BalustradeBag> apis = bagAPIs.getApiMap();
        for (String node : apis.keySet()) {
            boolean update = false;
            DateTime now = DateTime.now();
            String after = lastSync.lastBagSync(node);
            BalustradeBag api = apis.get(node);
            Call<Response<Bag>> call = api.getBags(ImmutableMap.of(
                    "admin_node", node,
                    "after", after));

            CompletableFuture<retrofit2.Response<Response<Bag>>> future =
                    CompletableFuture.supplyAsync(() -> http(call), service);

            CompletableFuture<Boolean> completed = future.thenApply(response -> {
                boolean success = response.isSuccess();
                List<Bag> bags = ImmutableList.of();

                if (success) {
                    bags = response.body().getResults();
                }

                log.info("[{}]: {} Bags to sync", node, bags.size());
                // TODO: Would be nice to have these execute async and reduce them to a single value
                for (Bag bag : bags) {
                    log.trace("[{}]: Updating bag {}", node, bag.getUuid());
                    SimpleCallback<Bag> bagCB = new SimpleCallback<>();

                    Call<Bag> sync;
                    Call<Bag> get = bagAPI.getBag(bag.getUuid());
                    get.enqueue(bagCB);
                    Optional<Bag> bagResponse = bagCB.getResponse();

                    if (bagResponse.isPresent()) {
                        sync = bagAPI.updateBag(bag.getUuid(), bag);
                    } else {
                        sync = bagAPI.createBag(bag);
                    }

                    try {
                        retrofit2.Response<Bag> syncResponse = sync.execute();
                        if (syncResponse.isSuccess()) {
                            log.info("[{}]: Updated bag {} successfully", node, bag.getUuid());
                        } else {
                            log.warn("[{}]: Unable to update bag {}", node, bag.getUuid());
                            success = false;
                        }
                    } catch (IOException e) {
                        log.error("Error in call", e);
                        success = false;
                    }
                }

                return success;
            });

            // We don't really need this but it makes it easier to handle the exception
            completed.handle((ok, throwable) -> {
                if (ok != null && ok) {
                    lastSync.addLastBagSync(node, now);
                }
                return true;
            });
        }
    }

    /**
     * Update a nodes current information based on its own record
     */
    void syncNode() {
        Map<String, BalustradeNode> apis = nodeAPIs.getApiMap();
        for (String node : apis.keySet()) {
            SimpleCallback<Node> cb = new SimpleCallback<>();
            BalustradeNode api = apis.get(node);
            DateTime now = DateTime.now();

            /*
            Call<Node> call = api.getNode(node);
            call.enqueue(cb);
            Optional<Node> response = cb.getResponse();
            if (response.isPresent()) {
                Node n = response.get();
                log.trace("[{}]: Updating Node", node);
            }
            */

            ListenableFuture<Node> submit = service.submit(() -> {
                Call<Node> call = api.getNode(node);
                retrofit2.Response<Node> response = call.execute();
                if (response.isSuccess()) {
                    return response.body();
                }

                throw new Exception(response.errorBody().string());
            });

            Futures.addCallback(submit, new FutureCallback<Node>() {
                @Override
                public void onSuccess(@Nullable Node node) {
                    if (node != null) {
                        lastSync.addLastNode(node.getNamespace(), now);
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log.error("Error syncing node", throwable);
                }
            });


        }
    }

    // TODO: Might be able to do something like this to recognize
    // when calls have failed in the completable futures
    private class HeldException<T> {
        T t;
        Throwable throwable;

        public HeldException(T t) {
            this.t = t;
        }

        public boolean isSuccess() {
            return throwable == null;
        }
    }

}
