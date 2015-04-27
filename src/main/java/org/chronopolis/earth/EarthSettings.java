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

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    /*
    public EarthSettings setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
        return this;
    }
    */

}
