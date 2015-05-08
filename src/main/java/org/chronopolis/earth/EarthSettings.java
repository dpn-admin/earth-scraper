package org.chronopolis.earth;

import org.chronopolis.earth.models.Endpoint;
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

    List<Endpoint> endpoints = new ArrayList<>();
    String stage;

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

    /*
    public EarthSettings setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
        return this;
    }
    */

}
