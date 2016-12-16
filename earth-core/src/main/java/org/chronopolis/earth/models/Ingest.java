package org.chronopolis.earth.models;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Ingest Event model for DPN
 *
 * Created by shake on 8/3/16.
 */
public class Ingest {

    private String bag;
    private String ingestId;
    private Boolean ingested;
    private ZonedDateTime createdAt;
    private List<String> replicatingNodes;

    public String getBag() {
        return bag;
    }

    public Ingest setBag(String bag) {
        this.bag = bag;
        return this;
    }

    public String getIngestId() {
        return ingestId;
    }

    public Ingest setIngestId(String ingestId) {
        this.ingestId = ingestId;
        return this;
    }

    public Boolean getIngested() {
        return ingested;
    }

    public Ingest setIngested(Boolean ingested) {
        this.ingested = ingested;
        return this;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public Ingest setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public List<String> getReplicatingNodes() {
        return replicatingNodes;
    }

    public Ingest setReplicatingNodes(List<String> replicatingNodes) {
        this.replicatingNodes = replicatingNodes;
        return this;
    }
}
