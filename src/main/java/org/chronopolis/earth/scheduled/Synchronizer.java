package org.chronopolis.earth.scheduled;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Hook in endpoints
 *
 *
 * Created by shake on 3/31/15.
 */
@Component
@EnableScheduling
public class Synchronizer {

    @Autowired
    BalustradeNode nodeAPI;

    @Autowired
    BalustradeBag bagAPI;

    @Autowired
    BalustradeTransfers transferAPI;

    @Scheduled(cron="0 55 * * * *")
    public void synchronize() {
        syncNode();
        syncBags();
        syncTransfers();
    }

    private void syncTransfers() {
        transferAPI.getReplications(new HashMap());
        transferAPI.getRestores(new HashMap());
    }

    private void syncBags() {
        Map<String, String> params = new ImmutableMap.Builder<String, String>()
                .put("admin_node", "sample-node")
                .build();
        bagAPI.getBags(params);
    }

    private void syncNode() {
        String node = "sample-node";
        nodeAPI.getNode(node);
    }

}
