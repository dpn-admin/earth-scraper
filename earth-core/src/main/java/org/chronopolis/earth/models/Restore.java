package org.chronopolis.earth.models;

import org.joda.time.DateTime;

/**
 * Restore request from the DPN API
 *
 * Created by shake on 3/27/15.
 */
public class Restore {

    String restoreId;
    String fromNode;
    String toNode;
    String uuid;
    String protocol;
    String link;
    Status status;
    DateTime createdAt;
    DateTime updatedAt;

    public Restore() {
    }

    public String getRestoreId() {
        return restoreId;
    }

    public Restore setRestoreId(String restoreId) {
        this.restoreId = restoreId;
        return this;
    }

    public String getFromNode() {
        return fromNode;
    }

    public Restore setFromNode(String fromNode) {
        this.fromNode = fromNode;
        return this;
    }

    public String getToNode() {
        return toNode;
    }

    public Restore setToNode(String toNode) {
        this.toNode = toNode;
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public Restore setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getProtocol() {
        return protocol;
    }

    public Restore setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public String getLink() {
        return link;
    }

    public Restore setLink(String link) {
        this.link = link;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public Restore setStatus(Status status) {
        this.status = status;
        return this;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public Restore setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public DateTime getUpdatedAt() {
        return updatedAt;
    }

    public Restore setUpdatedAt(DateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }


    public enum Status {
        REQUESTED, ACCEPTED, REJECTED, PREPARED, FINISHED, CANCELLED
    }


}
