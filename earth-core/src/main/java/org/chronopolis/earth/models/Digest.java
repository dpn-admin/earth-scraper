package org.chronopolis.earth.models;

import java.time.ZonedDateTime;

/**
 * Digest resource in DPN
 *
 * Could possibly use HashCode/HashFunction to represent value/algorithm
 *
 * Created by shake on 8/3/16.
 */
public class Digest {

    private String bag;
    private String node;
    private String value;
    private String algorithm;
    private ZonedDateTime createdAt;

    public String getBag() {
        return bag;
    }

    public Digest setBag(String bag) {
        this.bag = bag;
        return this;
    }

    public String getNode() {
        return node;
    }

    public Digest setNode(String node) {
        this.node = node;
        return this;
    }

    public String getValue() {
        return value;
    }

    public Digest setValue(String value) {
        this.value = value;
        return this;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public Digest setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public Digest setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
