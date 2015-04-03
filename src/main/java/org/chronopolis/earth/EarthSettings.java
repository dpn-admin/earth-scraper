package org.chronopolis.earth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Created by shake on 11/14/14.
 */
@ConfigurationProperties(prefix = "earth")
public class EarthSettings {

    List<Endpoint> endpoints;

    static class Endpoint {
        String authKey;
        String apiRoot;
        String name;

        public String getApiRoot() {
            return apiRoot;
        }

        public Endpoint setApiRoot(String apiRoot) {
            this.apiRoot = apiRoot;
            return this;
        }

        public String getAuthKey() {
            return authKey;
        }

        public Endpoint setAuthKey(String authKey) {
            this.authKey = authKey;
            return this;
        }

        public String getName() {
            return name;
        }

        public Endpoint setName(String name) {
            this.name = name;
            return this;
        }
    }
}
