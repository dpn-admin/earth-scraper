package org.chronopolis.earth;

import org.chronopolis.earth.service.DpnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

/**
 * Entry point for our program.
 *
 * Created by shake on 11/13/14.
 */
@EnableConfigurationProperties
@SpringBootApplication(exclude = {HibernateJpaAutoConfiguration.class})
public class Earth implements CommandLineRunner {
    private final Logger log = LoggerFactory.getLogger(Earth.class);

    @Autowired
    DpnService service;

    @Autowired
    EarthSettings settings;

    /**
     * Main method for our application
     *
     * @param args execution arguments
     */
    public static void main(String[] args) {
        SpringApplication.exit(SpringApplication.run(Earth.class));
    }

    @Override
    public void run(final String... args) throws Exception {
        // Disable ssl cert validation
        if (settings.disableSNI()) {
            log.info("Disabling SNI/cert validation");
            System.setProperty("jsse.enableSNIExtension", "false");
            disableCertValidation();
        }

        service.replicate();
    }

    private void disableCertValidation() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    @Override
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    @Override
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
            log.error("Error in sslcontext", e);
        }
    }
}
