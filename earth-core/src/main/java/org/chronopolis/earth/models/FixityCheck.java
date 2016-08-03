package org.chronopolis.earth.models;

import java.time.ZonedDateTime;

/**
 * Class for a fixity check event in DPN
 *
 * Created by shake on 8/3/16.
 */
public class FixityCheck {

    private String bag;
    private String node;
    private String fixityCheckId;
    private Boolean success;
    private ZonedDateTime fixityAt;
    private ZonedDateTime createdAt;

    public FixityCheck() {
    }

    public String getBag() {
        return bag;
    }

    public FixityCheck setBag(String bag) {
        this.bag = bag;
        return this;
    }

    public String getNode() {
        return node;
    }

    public FixityCheck setNode(String node) {
        this.node = node;
        return this;
    }

    public String getFixityCheckId() {
        return fixityCheckId;
    }

    public FixityCheck setFixityCheckId(String fixityCheckId) {
        this.fixityCheckId = fixityCheckId;
        return this;
    }

    public Boolean getSuccess() {
        return success;
    }

    public FixityCheck setSuccess(Boolean success) {
        this.success = success;
        return this;
    }

    public ZonedDateTime getFixityAt() {
        return fixityAt;
    }

    public FixityCheck setFixityAt(ZonedDateTime fixityAt) {
        this.fixityAt = fixityAt;
        return this;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public FixityCheck setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
