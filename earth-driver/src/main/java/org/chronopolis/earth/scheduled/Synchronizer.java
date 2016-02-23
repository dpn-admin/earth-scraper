package org.chronopolis.earth.scheduled;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
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
import org.springframework.stereotype.Component;
import retrofit2.Call;

import java.io.IOException;
import java.util.Map;

/**
 * TODO: Make sure we only sync items which the remote node is the admin node of
 * TODO: DateTime -> LocalDate
 * Disabled temporarily
 *
 *
 * Created by shake on 3/31/15.
 */
@Component
@Profile("sync")
@EnableScheduling
public class Synchronizer {

    private final Logger log = LoggerFactory.getLogger(Synchronizer.class);

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

    // keep this disabled for the time being
    // @Scheduled(cron="${cron.sync:0 0 0 * * *}")
    public void synchronize() {
        readLastSync();
        syncNode();
        LastSync sync = syncBags();
        syncTransfers();
        writeLastSync(sync);
    }

    private void readLastSync() {
        try {
            lastSync = LastSync.read();
        } catch (IOException e) {
            log.error("Unable to read last sync!", e);
            lastSync = new LastSync();
        }
    }

    private void writeLastSync(LastSync sync) {
        try {
            sync.write();
        } catch (IOException e) {
            log.error("Unable to write last sync!", e);
        }
    }

    private void syncTransfers() {
        BalustradeTransfers transfers = local.getTransfersAPI();

        for (String node: transferAPIs.getApiMap().keySet()) {
            String after = lastSync.getLastSync(node);
            BalustradeTransfers api = transferAPIs.getApiMap().get(node);
            SimpleCallback<Response<Replication>> cb = new SimpleCallback<>();
            Call<Response<Replication>> call = api.getReplications(ImmutableMap.of(
                    "admin_node", node,
                    "after", after));
                    // "after", formatter.print(after)));

            call.enqueue(cb);

            Optional<Response<Replication>> response = cb.getResponse();
            if (response.isPresent()) {
                Response<Replication> replications = response.get();
                log.info("[{}]: {} Replications to sync", node, replications.getCount());

                // update each replication
                for (Replication replication : replications.getResults()) {
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
                        syncCall.execute();
                    } catch (IOException e) {
                        log.error("Error in call", e);
                    }
                }
            }

            // TODO: Sync restore requests
            // api.getRestores(new HashMap());
        }
    }

    private LastSync syncBags() {
        // Temporary placeholder for when we sync
        // we'll want a better way to do this
        LastSync newSyncs = new LastSync();
        BalustradeBag bagAPI = local.getBagAPI();
        Map<String, BalustradeBag> apis = bagAPIs.getApiMap();
        for (String node : apis.keySet()) {
            boolean update = true;
            String after = lastSync.getLastSync(node);

            SimpleCallback<Response<Bag>> cb = new SimpleCallback<>();
            BalustradeBag api = apis.get(node);
            Call<Response<Bag>> call = api.getBags(ImmutableMap.of(
                    "admin_node", node,
                    "after", after));
                    // "after", formatter.print(after)));

            call.enqueue(cb);

            Optional<Response<Bag>> response = cb.getResponse();
            if (response.isPresent()) {
                Response<Bag> bags = response.get();
                log.info("[{}]: {} Bags to sync", node, bags.getCount());

                // Update each bag
                for (Bag bag : bags.getResults()) {
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
                        sync.execute();
                    } catch (IOException e) {
                        update = false;
                        log.error("Error in call", e);
                    }
                }
            } else {
                update = false;
            }

            if (update) {
                newSyncs.addLastSync(node, DateTime.now());
            } else {
                newSyncs.addLastSync(node, lastSync.getLastSync(node));
            }

        }

        return newSyncs;
    }

    /**
     * Update a nodes current information based on its own record
     *
     */
    private void syncNode() {
        Map<String, BalustradeNode> apis = nodeAPIs.getApiMap();
        for (String node : apis.keySet()) {
            SimpleCallback<Node> cb = new SimpleCallback<>();
            BalustradeNode api = apis.get(node);
            Call<Node> call = api.getNode(node);
            call.enqueue(cb);
            Optional<Node> response = cb.getResponse();

            if (response.isPresent()) {
                Node n = response.get();
                log.trace("[{}]: Updating Node", node);
            }

        }

    }

}
