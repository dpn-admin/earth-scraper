package org.chronopolis.earth.models;


import java.time.ZonedDateTime;

/**
 * Representation of a replication transfer in the DPN REST api
 *
 * Created by shake on 3/2/15.
 */
@SuppressWarnings("WeakerAccess")
public class Replication {

    String replicationId;
    String fromNode;
    String toNode;
    String bag;
    String fixityAlgorithm;
    String fixityNonce;
    String fixityValue;
    String protocol;
    String link;
    ZonedDateTime createdAt;
    ZonedDateTime updatedAt;
    Boolean storeRequested;
    Boolean stored;
    Boolean cancelled;
    String cancelReason;

    public Replication() {
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Replication setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public Replication setCreatedAt(ZonedDateTime createdAt) {
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

    public String getBag() {
        return bag;
    }

    public Replication setBag(String bag) {
        this.bag = bag;
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

    public Boolean isStoreRequested() {
        return storeRequested;
    }

    public Replication setStoreRequested(Boolean storeRequested) {
        this.storeRequested = storeRequested;
        return this;
    }

    public Boolean isStored() {
        return stored;
    }

    public Replication setStored(Boolean stored) {
        this.stored = stored;
        return this;
    }

    public Boolean isCancelled() {
        return cancelled;
    }

    public Replication setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
        return this;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public Replication setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
        return this;
    }

}
