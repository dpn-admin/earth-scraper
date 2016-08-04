package org.chronopolis.earth.scheduled;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.Buffer;
import org.chronopolis.earth.EarthSettings;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.config.Ingest;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.support.ZonedDateTimeDeserializer;
import org.chronopolis.rest.support.ZonedDateTimeSerializer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

/**
 *
 * Created by shake on 6/24/16.
 */
public class DownloaderTest {

    private final Logger log = LoggerFactory.getLogger(DownloaderTest.class);

    Downloader downloader;

    public IngestAPI createIngest() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeDeserializer())
                .create();

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.HOURS)
                .addInterceptor(chain -> {
                    Request req = chain.request();

                    log.debug("[{}] {}", req.method(), req.url());
                    if (req.body() != null) {
                        Buffer b = new Buffer();
                        req.body().writeTo(b);
                        log.debug("{}", b.readUtf8());
                    }

                    return chain.proceed(req);
                })
                .build();

        Retrofit adapter = new Retrofit.Builder()
                .baseUrl("http://localhost:9999/api/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                // .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        return adapter.create(IngestAPI.class);
    }

    @Test
    public void push() throws Exception {
        // Setup our downloader object
        EarthSettings settings = new EarthSettings();
        settings.setLogChron(true);
        settings.setIngest(new Ingest().setNode("ucsd-dpn"));

        IngestAPI chronopolis = createIngest();
        TransferAPIs apis = mock(TransferAPIs.class);
        // downloader = new Downloader(settings, chronopolis, apis);

        Replication transfer = new Replication();
        transfer.setFixityAccept(true);
        transfer.setBagValid(true);
        transfer.setFromNode("test-node");
        transfer.setBag("test-uuid");
        // downloader.push(transfer);

        int i = 0;
    }

}