package org.chronopolis.earth.models;

import org.joda.time.DateTime;

/**
 * Representation of a replication transfer in the DPN REST api
 *
 * Created by shake on 3/2/15.
 */
public class Replication {

    String replicationId;
    String fromNode;
    String toNode;
    String uuid;
    String fixityAlgorithm;
    String fixityNonce;
    String fixityValue;
    Boolean fixityAccept;
    Boolean bagValid;
    String protocol;
    String link;
    DateTime createdAt;
    DateTime updatedAt;
    Status status;

    public Replication() {
    }

    public DateTime getUpdatedAt() {
        return updatedAt;
    }

    public Replication setUpdatedAt(DateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public Replication setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public String getLink() {
        return link;
    }

    public Replication setLink(String link) {
        this.link = link;
        return this;
    }

    public String getProtocol() {
        return protocol;
    }

    public Replication setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public Status status() {
        return status;
    }

    public Replication setStatus(Status status) {
        this.status = status;
        return this;
    }

    public Boolean isBagValid() {
        return bagValid != null && bagValid;
    }

    public Replication setBagValid(Boolean bagValid) {
        this.bagValid = bagValid;
        return this;
    }

    public Boolean isFixityAccept() {
        return fixityAccept != null && fixityAccept;
    }

    public Replication setFixityAccept(Boolean fixityAccept) {
        this.fixityAccept = fixityAccept;
        return this;
    }

    public String getFixityValue() {
        return fixityValue;
    }

    public Replication setFixityValue(String fixityValue) {
        this.fixityValue = fixityValue;
        return this;
    }

    public String getFixityNonce() {
        return fixityNonce;
    }

    public Replication setFixityNonce(String fixityNonce) {
        this.fixityNonce = fixityNonce;
        return this;
    }

    public String getFixityAlgorithm() {
        return fixityAlgorithm;
    }

    public Replication setFixityAlgorithm(String fixityAlgorithm) {
        this.fixityAlgorithm = fixityAlgorithm;
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public Replication setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getToNode() {
        return toNode;
    }

    public Replication setToNode(String toNode) {
        this.toNode = toNode;
        return this;
    }

    public String getFromNode() {
        return fromNode;
    }

    public Replication setFromNode(String fromNode) {
        this.fromNode = fromNode;
        return this;
    }

    public String getReplicationId() {
        return replicationId;
    }

    public Replication setReplicationId(String replicationId) {
        this.replicationId = replicationId;
        return this;
    }


    public enum Status {
        REQUESTED("Requested"),
        REJECTED("Rejected"),
        RECEIVED("Received"),
        CONFIRMED("Confirmed"),
        STORED("Stored"),
        CANCELLED("Cancelled"),
        UNKNOWN("Unknown");

        private final String name;

        Status(String name) {
            this.name = name;
        }

        public static Status fromString(String status) {
            if (status.equalsIgnoreCase("Requested")) {
                return REQUESTED;
            } else if (status.equalsIgnoreCase("Rejected")) {
                return REJECTED;
            } else if (status.equalsIgnoreCase("Received")) {
                return RECEIVED;
            } else if (status.equalsIgnoreCase("Confirmed")) {
                return CONFIRMED;
            } else if (status.equalsIgnoreCase("Stored")) {
                return STORED;
            } else if (status.equalsIgnoreCase("Cancelled")) {
                return CANCELLED;
            }

            return UNKNOWN;
        }

        public String getName() {
            return name;
        }
    }

}
