package org.chronopolis.earth;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.chronopolis.earth.api.BagAPIs;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.NodeAPIs;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.models.Endpoint;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.serializers.DateTimeDeserializer;
import org.chronopolis.earth.serializers.DateTimeSerializer;
import org.chronopolis.earth.serializers.ReplicationStatusDeserializer;
import org.chronopolis.earth.serializers.ReplicationStatusSerializer;
import org.chronopolis.rest.api.IngestAPI;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

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
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(DateTime.class, new DateTimeSerializer())
                .registerTypeAdapter(DateTime.class, new DateTimeDeserializer())
                .registerTypeAdapter(Replication.Status.class, new ReplicationStatusSerializer())
                .registerTypeAdapter(Replication.Status.class, new ReplicationStatusDeserializer())
                .create();

        for (Endpoint endpoint : settings.endpoints) {
            log.info("Creating adapter for {}", endpoint.getName());
            RestAdapter adapter = new RestAdapter.Builder()
                    .setEndpoint(endpoint.getApiRoot())
                    .setConverter(new GsonConverter(gson))
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

    @Bean
    IngestAPI ingestAPI() {
        // TODO: Get credentials
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint("http://localhost:8000")
                .setRequestInterceptor(new CredentialRequestInterceptor("admin", "admin"))
                // .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        return adapter.create(IngestAPI.class);
    }

}
