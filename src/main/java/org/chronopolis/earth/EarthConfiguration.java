package org.chronopolis.earth;

import org.chronopolis.earth.api.BagAPIs;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.NodeAPIs;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.models.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit.RestAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shake on 4/27/15.
 */
@Configuration
public class EarthConfiguration {

    private final Logger log = LoggerFactory.getLogger(EarthConfiguration.class);

    @Autowired
    EarthSettings settings;

    @Bean
    TransferAPIs transferAPIs () {
        return new TransferAPIs();
    }

    @Bean
    NodeAPIs nodeAPIs() {
        return new NodeAPIs();
    }

    @Bean
    BagAPIs bagAPIs() {
        return new BagAPIs();
    }

    @Bean
    List<RestAdapter> adapters(TransferAPIs transferAPIs,
                               NodeAPIs nodeAPIs,
                               BagAPIs bagAPIs) {
        log.info("Creating adapters");
        List<RestAdapter> adapters = new ArrayList<>();

        for (Endpoint endpoint : settings.endpoints) {
            log.info("Creating adapter for {}", endpoint.getName());
            RestAdapter adapter = new RestAdapter.Builder()
                    .setEndpoint(endpoint.getApiRoot())
                    .setRequestInterceptor(new TokenInterceptor(endpoint.getAuthKey()))
                    .setLogLevel(RestAdapter.LogLevel.FULL)
                    .build();

            adapters.add(adapter);
            bagAPIs.put(endpoint.getName(), adapter.create(BalustradeBag.class));
            nodeAPIs.put(endpoint.getName(), adapter.create(BalustradeNode.class));
            transferAPIs.put(endpoint.getName(), adapter.create(BalustradeTransfers.class));
        }

        return adapters;
    }

}
