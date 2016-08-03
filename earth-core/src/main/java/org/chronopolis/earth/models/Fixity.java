package org.chronopolis.earth.models;

import org.joda.time.DateTime;

/**
 * Fixity encapsulation for DPN
 *
 * Created by shake on 5/1/15.
 */
@Deprecated
public class Fixity {

    String algorithm;
    String digest;
    DateTime createdAt;

    public Fixity() {
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public Fixity setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    public String getDigest() {
        return digest;
    }

    public Fixity setDigest(String digest) {
        this.digest = digest;
        return this;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public Fixity setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
