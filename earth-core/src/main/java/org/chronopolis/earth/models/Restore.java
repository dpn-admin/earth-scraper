package org.chronopolis.earth.models;

import java.time.ZonedDateTime;

/**
 * Restore request from the DPN API
 *
 * Created by shake on 3/27/15.
 */
@SuppressWarnings("WeakerAccess")
public class Restore {

    String restoreId;
    String fromNode;
    String toNode;
    String bag;
    String protocol;
    String link;
    ZonedDateTime createdAt;
    ZonedDateTime updatedAt;
    Boolean accepted;
    Boolean finished;
    Boolean cancelled;
    String cancelReason;

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

    public String getBag() {
        return bag;
    }

    public Restore setBag(String bag) {
        this.bag = bag;
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

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public Restore setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Restore setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Boolean isAccepted() {
        return accepted;
    }

    public Restore setAccepted(Boolean accepted) {
        this.accepted = accepted;
        return this;
    }

    public Boolean isFinished() {
        return finished;
    }

    public Restore setFinished(Boolean finished) {
        this.finished = finished;
        return this;
    }

    public Boolean isCancelled() {
        return cancelled;
    }

    public Restore setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
        return this;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public Restore setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
        return this;
    }


    public enum Status {
        REQUESTED, ACCEPTED, REJECTED, PREPARED, FINISHED, CANCELLED
    }


}
