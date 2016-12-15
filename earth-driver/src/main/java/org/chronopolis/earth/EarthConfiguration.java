package org.chronopolis.earth;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.Events;
import org.chronopolis.earth.api.LocalAPI;
import org.chronopolis.earth.api.Remote;
import org.chronopolis.earth.config.Dpn;
import org.chronopolis.earth.config.Endpoint;
import org.chronopolis.earth.config.Hikari;
import org.chronopolis.earth.config.Ingest;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.support.PageDeserializer;
import org.chronopolis.rest.support.ZonedDateTimeDeserializer;
import org.chronopolis.rest.support.ZonedDateTimeSerializer;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Configuration for our beans. Mostly just creation of the rest adapters to
 * access the various apis.
 * <p>
 * Created by shake on 4/27/15.
 */
@Configuration
@EnableConfigurationProperties(EarthSettings.class)
public class EarthConfiguration {

    private final Logger log = LoggerFactory.getLogger(EarthConfiguration.class);

    @Bean
    public DateTimeFormatter formatter() {
        return ISODateTimeFormat.dateTimeNoMillis().withZoneUTC();
    }

    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeDeserializer())
                .serializeNulls()
                .create();
    }

    @Bean
    public List<Remote> remotes(EarthSettings settings,
                                Gson gson) {
        Dpn dpn = settings.getDpn();
        return dpn.getRemote().stream()
                .map(e -> new Remote(e, gson))
                .collect(Collectors.toList());
    }

    @Bean
    public LocalAPI local(EarthSettings settings, Gson gson) {
        Dpn dpn = settings.getDpn();
        Endpoint local = dpn.getLocal();
        log.debug("Creating local adapter for root {}", local.getApiRoot());

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new OkTokenInterceptor(local.getAuthKey()))
                .build();

        Retrofit adapter = new Retrofit.Builder()
                .baseUrl(local.getApiRoot())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build();

        return new LocalAPI().setNode(local.getName())
                .setBagAPI(adapter.create(BalustradeBag.class))
                .setNodeAPI(adapter.create(BalustradeNode.class))
                .setEventsAPI(adapter.create(Events.class))
                .setTransfersAPI(adapter.create(BalustradeTransfers.class));
    }

    @Bean
    public IngestAPI ingestAPI(EarthSettings settings) {
        Ingest api = settings.getIngest();

        Type bagPage = new TypeToken<PageImpl<Bag>>() {}.getType();
        Type bagList = new TypeToken<List<Bag>>() {}.getType();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(bagPage, new PageDeserializer(bagList))
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeDeserializer())
                .create();

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.HOURS)
                .addInterceptor(new OkBasicInterceptor(
                        api.getUsername(),
                        api.getPassword()))
                .build();
        // TODO: Get credentials
        log.debug("Staging: {}", settings.getStage());
        log.debug("Ingest Settings: {} {}", api.getEndpoint(), api.getUsername());
        Retrofit adapter = new Retrofit.Builder()
                .baseUrl(api.getEndpoint())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return adapter.create(IngestAPI.class);
    }

    @Bean
    public SessionFactory sessionFactory(EarthSettings settings) {
        Hikari hikari = settings.getHikari();
        // SessionFactory factory = new SessionFactoryImpl()
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure()
                .applySettings(hikari.asMap())
                .build();

        return new MetadataSources(registry)
                .buildMetadata().buildSessionFactory();
    }

}
