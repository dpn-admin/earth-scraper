package org.chronopolis.earth.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

/**
 * Keep track of what we have done for replications
 *
 * TODO: Replication Stats (by id)
 *
 * Created by shake on 8/9/16.
 */
public class ReplicationFlow {

    private final Logger log = LoggerFactory.getLogger(ReplicationFlow.class);
    private final String update = "UPDATE replication_flow SET %s = %s WHERE replication_id = :replication_id";

    private String replicationId;

    private Boolean pushed;
    private Boolean received;
    private Boolean extracted;
    private Boolean validated;

    public ReplicationFlow() {
        pushed = false;
        received = false;
        extracted = false;
        validated = false;
    }

    public String getReplicationId() {
        return replicationId;
    }

    public ReplicationFlow setReplicationId(String replicationId) {
        this.replicationId = replicationId;
        return this;
    }

    public Boolean isPushed() {
        return pushed;
    }

    public ReplicationFlow setPushed(Boolean pushed) {
        this.pushed = pushed;
        return this;
    }

    public Boolean isReceived() {
        return received;
    }

    public ReplicationFlow setReceived(Boolean received) {
        this.received = received;
        return this;
    }

    public Boolean isExtracted() {
        return extracted;
    }

    public ReplicationFlow setExtracted(Boolean extracted) {
        this.extracted = extracted;
        return this;
    }

    public Boolean isValidated() {
        return validated;
    }

    public ReplicationFlow setValidated(Boolean validated) {
        this.validated = validated;
        return this;
    }

    // DB Ops

    public void setPushed(boolean pushed, Sql2o sql2o) {
        this.pushed = pushed;
        update("pushed", String.valueOf(pushed), sql2o);
    }

    public void setReceived(boolean received, Sql2o sql2o) {
        this.received = received;
        update("received", String.valueOf(received), sql2o);
    }

    public void setExtracted(boolean extracted, Sql2o sql2o) {
        this.extracted = extracted;
        update("extracted", String.valueOf(extracted), sql2o);
    }

    public void setValidated(boolean validated, Sql2o sql2o) {
        this.validated = validated;
        update("validated", String.valueOf(validated), sql2o);
    }

    private void update(String col, String val, Sql2o sql2o) {
        String query = String.format(update, col, ":" + col);
        log.debug(query);

        try (Connection conn = sql2o.open()) {
            conn.createQuery(query)
                    .addParameter(col, val)
                    .addParameter("replicationId", replicationId)
                    .executeUpdate();
        }
    }
}
