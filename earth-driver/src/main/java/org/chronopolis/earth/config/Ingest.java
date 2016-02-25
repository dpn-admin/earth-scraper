package org.chronopolis.earth.config;

/**
 * Ingest API Information for chronopolis
 *
 * Created by shake on 6/23/15.
 */
public class Ingest {

    String username;
    String password;
    String endpoint;
    String node;

    public String getUsername() {
        return username;
    }

    public Ingest setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public Ingest setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Ingest setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public String getNode() {
        return node;
    }

    public Ingest setNode(String node) {
        this.node = node;
        return this;
    }
}
