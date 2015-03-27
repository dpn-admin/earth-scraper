package org.chronopolis.earth;

import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import retrofit.RestAdapter;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by shake on 11/13/14.
 */
@ComponentScan
@EnableAutoConfiguration
public class Earth implements CommandLineRunner {

    @Autowired
    EarthSettings earthSettings;

    BalustradeTransfers balustrade;

    public static void main(String[] args) {
        SpringApplication.exit(SpringApplication.run(Earth.class));
    }

    @Override
    public void run(final String... args) throws Exception {
        // Disable ssl cert validation
        System.setProperty("jsse.enableSNIExtension", "false");
        disableCertValidation();

        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint("https://devops.aptrust.org/dpnode/")
                .setRequestInterceptor(new TokenInterceptor(earthSettings.getAuthorizationKey()))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        balustrade = adapter.create(BalustradeTransfers.class);

        ////
        // First we queue any transfers which are incomplete
        // Then we look for new transfers to download
        ////
        Map<String, String> ongoing = Maps.newHashMap();
        ongoing.put("status", "A");
        ongoing.put("fixity", "False");
        System.out.println("Getting ongoing transfers");
        get(ongoing);


        System.out.println("Getting new transfers");
        get(Maps.<String, String>newHashMap());

        System.out.println("Done");
    }

    private void get(Map<String, String> query) throws InterruptedException {
        int page = 1;
        String next;
        do {
            Response<Replication> transfers = balustrade.getReplications(query);
            next = transfers.getNext();
            System.out.printf("Count: %d\nNext: %s\nPrevious: %s\n",
                    transfers.getCount(),
                    transfers.getNext(),
                    transfers.getPrevious());
            for (Replication transfer : transfers.getResults()) {
                download(transfer);
                update(transfer);
            }

            ++page;
            query.put("page", String.valueOf(page));
        } while (next != null);
    }

    private void download(Replication transfer) throws InterruptedException {
        System.out.printf("Getting %s from %s\n", transfer.getUuid(), transfer.getLink());
        String[] cmd = new String[]{"rsync", "-aL", "--stats", transfer.getLink(), "/tmp/dpn/"};
        String stats;

        ProcessBuilder pb = new ProcessBuilder(cmd);
        try {
            Process p = pb.start();
            int exit = p.waitFor();

            stats = stringFromStream(p.getInputStream());


            if (exit != 0) {
                System.out.println("There was an error rsyncing!");
            }

            System.out.printf("Rsync stats:\n %s", stats);
        } catch (IOException e) {
            System.out.println("Error executing rsync");
        }

        TimeUnit.SECONDS.sleep(1);
    }

    private String stringFromStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line).append("\n");
        }
        return out.toString();
    }


    private void update(Replication transfer) {
        // Get the files digest
        HashFunction func = Hashing.sha256();
        Path file = Paths.get("/tmp/dpn/", transfer.getUuid() + ".tar");
        HashCode hash;
        try {
            hash = Files.hash(file.toFile(), func);
        } catch (IOException e) {
            System.out.println("Error hashing file");
            return;
        }

        // Set the receipt
        String receipt = hash.toString();
        transfer.setFixityValue(receipt);
        balustrade.updateReplication(transfer.getReplicationId(), transfer);
    }

    private void disableCertValidation() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (GeneralSecurityException e) {
        }
    }
}
