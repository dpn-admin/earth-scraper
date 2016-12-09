package org.chronopolis.earth.config;


/**
 * DPN REST Endpoint information
 *
 * Created by shake on 4/27/15.
 */
public class Endpoint {
    private String authKey;
    private String apiRoot;
    private String name;

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
