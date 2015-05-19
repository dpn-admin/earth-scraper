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
    boolean fixityAccept;
    boolean bagValid;
    Status status;
    String protocol;
    String link;
    DateTime createdAt;
    DateTime updatedAt;

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

    public Status getStatus() {
        return status;
    }

    public Replication setStatus(Status status) {
        this.status = status;
        return this;
    }

    public boolean isBagValid() {
        return bagValid;
    }

    public Replication setBagValid(boolean bagValid) {
        this.bagValid = bagValid;
        return this;
    }

    public boolean isFixityAccept() {
        return fixityAccept;
    }

    public Replication setFixityAccept(boolean fixityAccept) {
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
        REQUESTED("Requested"), REJECTED("Rejected"), RECEIVED("Received"), CONFIRMED("Confirmed"), STORED("Stored"), CANCELLED("Cancelled");

        private final String name;

        Status(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
