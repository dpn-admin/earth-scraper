package org.chronopolis.earth.scheduled;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.earth.api.BagAPIs;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.NodeAPIs;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.models.Node;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Make sure we only sync items which the remote node is the admin node of
 * Disabled temporarily
 *
 *
 * Created by shake on 3/31/15.
 */
@Component
@Profile("sync")
@EnableScheduling
public class Synchronizer {

    @Autowired
    DateTimeFormatter formatter;

    @Autowired
    BagAPIs bagAPIs;

    @Autowired
    TransferAPIs transferAPIs;

    @Autowired
    NodeAPIs nodeAPIs;

    // keep this disabled for the time being
    // @Scheduled(cron="${cron.sync:0 0 0 * * *}")
    public void synchronize() {
        syncNode();
        syncBags();
        syncTransfers();
    }

    private void syncTransfers() {
        DateTime after = DateTime.now().minusWeeks(1);
        for (String node: transferAPIs.getApiMap().keySet()) {
            BalustradeTransfers api = transferAPIs.getApiMap().get(node);
            api.getReplications(ImmutableMap.of(
                    "admin_node", node,
                    "after", formatter.print(after)));

            // api.getRestores(new HashMap());
        }
    }

    private void syncBags() {
        // Temporary placeholder for when we sync
        // we'll want a better way to do this
        DateTime after = DateTime.now().minusWeeks(1);
        Map<String, BalustradeBag> apis = bagAPIs.getApiMap();
        for (String node : apis.keySet()) {
            BalustradeBag api = apis.get(node);
            api.getBags(ImmutableMap.of(
                    "admin_node", node,
                    "after", formatter.print(after)));

            // ourAPI.updateBag("uuid", bag, callback);
        }
        Map<String, String> params = new ImmutableMap.Builder<String, String>()
                .put("admin_node", "sample-node")
                .build();
    }

    /**
     * Update a nodes current information based on its own record
     *
     */
    private void syncNode() {
        Map<String, BalustradeNode> apis = nodeAPIs.getApiMap();
        for (String node : apis.keySet()) {
            BalustradeNode api = apis.get(node);
            api.getNode(node);
            // ourAPI.updateNode(node);
        }

    }

}
