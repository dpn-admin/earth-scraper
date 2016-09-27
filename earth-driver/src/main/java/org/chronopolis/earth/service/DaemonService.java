package org.chronopolis.earth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Service for when we run in the background
 * <p>
 * Created by shake on 7/7/15.
 */
@Component
@Profile("production")
public class DaemonService implements DpnService {
    private final Logger log = LoggerFactory.getLogger(DaemonService.class);

    @Autowired
    ApplicationContext ctx;

    @Override
    public void replicate() {
        System.out.close();
        System.err.close();
        boolean interrupted = false;

        while (!interrupted) {
            try {
                Thread.sleep(30000);
                log.trace("sleeping...");
            } catch (InterruptedException e) {
                log.error("Thread interrupted", e);
                interrupted = true;
                Thread.currentThread().interrupt();
            }
        }
    }
}
