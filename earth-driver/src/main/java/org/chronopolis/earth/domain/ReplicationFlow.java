package org.chronopolis.earth.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Keep track of what we have done for replications
 *
 * TODO: Replication Stats (by id)
 *
 * Created by shake on 8/9/16.
 */
@Entity
public class ReplicationFlow {

    @Id
    private String id;

    private String node;

    private boolean pushed;
    private boolean received;
    private boolean extracted;
    private boolean validated;

    public ReplicationFlow() {
        pushed = false;
        received = false;
        extracted = false;
        validated = false;
    }

    public String getId() {
        return id;
    }

    public ReplicationFlow setId(String id) {
        this.id = id;
        return this;
    }

    public boolean isPushed() {
        return pushed;
    }

    public ReplicationFlow setPushed(boolean pushed) {
        this.pushed = pushed;
        return this;
    }

    public boolean isReceived() {
        return received;
    }

    public ReplicationFlow setReceived(boolean received) {
        this.received = received;
        return this;
    }

    public boolean isExtracted() {
        return extracted;
    }

    public ReplicationFlow setExtracted(boolean extracted) {
        this.extracted = extracted;
        return this;
    }

    public boolean isValidated() {
        return validated;
    }

    public ReplicationFlow setValidated(boolean validated) {
        this.validated = validated;
        return this;
    }

    public String getNode() {
        return node;
    }

    public ReplicationFlow setNode(String node) {
        this.node = node;
        return this;
    }

}
