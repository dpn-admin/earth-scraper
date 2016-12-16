package org.chronopolis.earth.domain;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

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

    // R e l a t i o n s h i p s
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HttpDetail> details = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RsyncDetail> rsyncs = new ArrayList<>();

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

    public List<HttpDetail> getDetails() {
        return details;
    }

    public ReplicationFlow setDetails(List<HttpDetail> details) {
        this.details = details;
        return this;
    }

    public ReplicationFlow addHttpDetail(HttpDetail detail) {
        this.details.add(detail);
        return this;
    }

    public List<RsyncDetail> getRsyncs() {
        return rsyncs;
    }

    public ReplicationFlow setRsyncs(List<RsyncDetail> rsyncs) {
        this.rsyncs = rsyncs;
        return this;
    }

    public ReplicationFlow addRsync(RsyncDetail rsync) {
        this.rsyncs.add(rsync);
        return this;
    }
}
