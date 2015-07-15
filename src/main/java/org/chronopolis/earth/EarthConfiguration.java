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
import org.chronopolis.earth.config.Dpn;
import org.chronopolis.earth.config.Endpoint;
import org.chronopolis.earth.config.Ingest;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.serializers.DateTimeDeserializer;
import org.chronopolis.earth.serializers.DateTimeSerializer;
import org.chronopolis.earth.serializers.ReplicationStatusDeserializer;
import org.chronopolis.earth.serializers.ReplicationStatusSerializer;
import org.chronopolis.rest.api.IngestAPI;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
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
 * Configuration for our beans. Mostly just creation of the rest adapters to
 * access the various apis.
 *
 * Created by shake on 4/27/15.
 */
@Configuration
public class EarthConfiguration {

    private final Logger log = LoggerFactory.getLogger(EarthConfiguration.class);

    @Autowired
    EarthSettings settings;

    @Bean
    DateTimeFormatter formatter() {
        return ISODateTimeFormat.basicDateTimeNoMillis().withZoneUTC();
    }

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
    Gson gson() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(DateTime.class, new DateTimeSerializer())
                .registerTypeAdapter(DateTime.class, new DateTimeDeserializer())
                .registerTypeAdapter(Replication.Status.class, new ReplicationStatusSerializer())
                .registerTypeAdapter(Replication.Status.class, new ReplicationStatusDeserializer())
                .create();
    }

    @Bean
    List<RestAdapter> adapters(TransferAPIs transferAPIs,
                               NodeAPIs nodeAPIs,
                               BagAPIs bagAPIs,
                               Gson gson) {
        List<RestAdapter> adapters = new ArrayList<>();
        Dpn dpn = settings.getDpn();

        for (Endpoint endpoint : dpn.getRemote()) {
            log.debug("Creating adapter for {} {}", endpoint.getName(), endpoint.getApiRoot());
            RestAdapter adapter = new RestAdapter.Builder()
                    .setEndpoint(endpoint.getApiRoot())
                    .setConverter(new GsonConverter(gson))
                    .setRequestInterceptor(new TokenInterceptor(endpoint.getAuthKey()))
                    .setLogLevel(RestAdapter.LogLevel.NONE)
                    // .setExecutors(Executors.newCachedThreadPool(), Executors.newSingleThreadExecutor())
                    .build();

            adapters.add(adapter);
            bagAPIs.put(endpoint.getName(), adapter.create(BalustradeBag.class));
            nodeAPIs.put(endpoint.getName(), adapter.create(BalustradeNode.class));
            transferAPIs.put(endpoint.getName(), adapter.create(BalustradeTransfers.class));
        }


        return adapters;
    }

    @Bean
    RestAdapter local(Gson gson) {
        Dpn dpn = settings.getDpn();
        Endpoint local = dpn.getLocal();
        log.debug("Creating local adapter for root {}", local.getApiRoot());
        return new RestAdapter.Builder()
                .setEndpoint(local.getApiRoot())
                .setConverter(new GsonConverter(gson))
                .setRequestInterceptor(new TokenInterceptor(local.getAuthKey()))
                .setLogLevel(RestAdapter.LogLevel.NONE)
                .build();
    }

    @Bean
    IngestAPI ingestAPI() {
        Ingest api = settings.getIngest();
        // TODO: Get credentials
        log.debug("Staging: {}", settings.getStage());
        log.debug("Ingest Settings: {} {}", api.getEndpoint(), api.getUsername());
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(api.getEndpoint())
                .setRequestInterceptor(new CredentialRequestInterceptor(
                        api.getUsername(),
                        api.getPassword()))
                // .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        return adapter.create(IngestAPI.class);
    }

}
