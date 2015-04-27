package org.chronopolis.earth.models;


/**
 * Created by shake on 4/27/15.
 */
public class Endpoint {
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
