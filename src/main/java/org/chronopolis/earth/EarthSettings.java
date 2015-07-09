package org.chronopolis.earth;

import org.chronopolis.earth.models.Endpoint;
import org.chronopolis.earth.models.Ingest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shake on 11/14/14.
 */
@Component
@ConfigurationProperties(prefix = "earth")
public class EarthSettings {

    Boolean disableSNI;
    String stage;
    Ingest ingest;
    List<Endpoint> endpoints = new ArrayList<>();

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

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

    /*
    public EarthSettings setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
        return this;
    }
    */

}
