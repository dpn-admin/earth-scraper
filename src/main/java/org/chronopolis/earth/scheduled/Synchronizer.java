package org.chronopolis.earth.scheduled;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.earth.api.BagAPIs;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.NodeAPIs;
import org.chronopolis.earth.api.TransferAPIs;
import org.springframework.beans.factory.annotation.Autowired;
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
// @Component
// @EnableScheduling
public class Synchronizer {

    @Autowired
    BagAPIs bagAPIs;

    @Autowired
    TransferAPIs transferAPIs;

    @Autowired
    NodeAPIs nodeAPIs;

    // @Scheduled(cron="0 55 * * * *")
    public void synchronize() {
        syncNode();
        syncBags();
        syncTransfers();
    }

    private void syncTransfers() {
        for (String node: transferAPIs.getApiMap().keySet()) {
            BalustradeTransfers api = transferAPIs.getApiMap().get(node);
            api.getReplications(new HashMap());
            api.getRestores(new HashMap());
        }
    }

    private void syncBags() {
        Map<String, String> params = new ImmutableMap.Builder<String, String>()
                .put("admin_node", "sample-node")
                .build();
        // bagAPI.getBags(params);
    }

    private void syncNode() {
        String node = "sample-node";
        // nodeAPI.getNode(node);
    }

}
