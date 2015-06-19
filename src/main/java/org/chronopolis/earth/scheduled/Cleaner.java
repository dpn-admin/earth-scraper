package org.chronopolis.earth.scheduled;

import com.google.common.collect.Maps;
import org.chronopolis.earth.EarthSettings;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Clean up any used resources which no longer need to be held
 *
 * Created by shake on 5/18/15.
 */
@Component
@EnableScheduling
public class Cleaner {
    private final Logger log = LoggerFactory.getLogger(Cleaner.class);

    @Autowired
    EarthSettings settings;

    @Autowired
    TransferAPIs transferAPIs;

    void clean() {
        int page = 1;
        int pageSize = 10;
        Map<String, String> params = Maps.newHashMap();
        params.put("status", Replication.Status.CANCELLED.getName());
        params.put("page", String.valueOf(page));
        params.put("page_size", String.valueOf(pageSize));
        params.put("order_by", "updated_at");

        for (BalustradeTransfers api: transferAPIs.getApiMap().values()) {
            Response<Replication> replications = api.getReplications(params);
        }

    }

    private void clean(Replication replication) {
        String from = replication.getFromNode();
        String uuid = replication.getUuid();

        Path bag = Paths.get(settings.getStage(), from, uuid);
        if (bag.toFile().exists()) {
            log.info("Removing cancelled transfer {}::{}", from, uuid);
        }
    }

}
