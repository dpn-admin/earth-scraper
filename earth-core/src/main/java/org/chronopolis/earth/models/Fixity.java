package org.chronopolis.earth.models;

import org.joda.time.DateTime;

/**
 * @deprecated Will be removed by 2.0.0-RELEASE
 * Fixity encapsulation for DPN
 *
 * Created by shake on 5/1/15.
 */
@Deprecated
public class Fixity {

    private String algorithm;
    private String digest;
    private DateTime createdAt;

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
