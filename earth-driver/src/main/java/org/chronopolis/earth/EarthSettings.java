package org.chronopolis.earth;

import org.chronopolis.earth.config.Dpn;
import org.chronopolis.earth.config.Ingest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration Properties for our application
 *
 * Created by shake on 11/14/14.
 */
@Component
@ConfigurationProperties(prefix = "earth")
public class EarthSettings {

    Boolean disableSNI;
    String stage;
    Ingest ingest;
    Dpn dpn;

    public String getStage() {
        return stage;
    }

    public EarthSettings setStage(String stage) {
        this.stage = stage;
        return this;
    }

    public Ingest getIngest() {
        return ingest;
    }

    public EarthSettings setIngest(Ingest ingest) {
        this.ingest = ingest;
        return this;
    }

    public Boolean disableSNI() {
        return disableSNI;
    }

    public EarthSettings setDisableSNI(Boolean disableSNI) {
        this.disableSNI = disableSNI;
        return this;
    }

    public Dpn getDpn() {
        return dpn;
    }

    public EarthSettings setDpn(Dpn dpn) {
        this.dpn = dpn;
        return this;
    }

}
