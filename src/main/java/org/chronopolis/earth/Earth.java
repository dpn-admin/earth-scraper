package org.chronopolis.earth;

import org.chronopolis.earth.service.DpnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

/**
 * Entry point for our program.
 *
 * Created by shake on 11/13/14.
 */
@SpringBootApplication
@EnableConfigurationProperties
public class Earth implements CommandLineRunner {

    @Autowired
    DpnService service;

    @Autowired
    EarthSettings settings;

    public static void main(String[] args) {
        SpringApplication.exit(SpringApplication.run(Earth.class));
    }

    @Override
    public void run(final String... args) throws Exception {
        // Disable ssl cert validation
        if (settings.disableSNI()) {
            System.out.println("Disabling SNI/cert validation");
            System.setProperty("jsse.enableSNIExtension", "false");
            disableCertValidation();
        }

        service.replicate();
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
