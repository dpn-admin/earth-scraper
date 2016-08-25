package org.chronopolis.earth.domain;

import org.chronopolis.earth.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.util.List;

/**
 * Keep track of what we have done for replications
 *
 * TODO: Replication Stats (by id)
 * TODO: From Node
 *
 * Created by shake on 8/9/16.
 */
public class ReplicationFlow {

    private static final Logger log = LoggerFactory.getLogger(ReplicationFlow.class);
    private final String update = "UPDATE replication_flow SET pushed = :pushed, received = :received, extracted = :extracted, validated = :validated WHERE replication_id = :replicationId";

    private String replicationId;
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

    public String getReplicationId() {
        return replicationId;
    }

    public ReplicationFlow setReplicationId(String replicationId) {
        this.replicationId = replicationId;
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

    // DB Ops

    /**
     * Save a ReplicationFlow object to the db
     *
     * @param sql2o the sql2o connection
     */
    public void save(Sql2o sql2o) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(update)
                    .addParameter("pushed", pushed)
                    .addParameter("received", received)
                    .addParameter("extracted", extracted)
                    .addParameter("validated", validated)
                    .addParameter("replicationId", replicationId)
                    .executeUpdate();
        }
    }

    /**
     * Get or insert a replication flow for ${replicationId}
     *
     * @param replication the replication to get
     * @param sql2o the sql2o connection
     * @return the ReplicationFlow for replicationId
     */
    public static ReplicationFlow get(Replication replication, Sql2o sql2o) {
        String sql = "SELECT replication_id, received, extracted, validated, pushed " +
                "FROM replication_flow " +
                "WHERE replication_id = :replicationId";

        String replicationId = replication.getReplicationId();
        String node = replication.getFromNode();

        try (Connection conn = sql2o.open()) {
            ReplicationFlow flow = conn.createQuery(sql)
                    .addParameter("replicationId", replicationId)
                    .addColumnMapping("REPLICATION_ID", "replicationId")
                    .executeAndFetchFirst(ReplicationFlow.class);

            if (flow == null) {
                log.info("Creating new flow for {}", replicationId);
                flow = new ReplicationFlow();
                flow.setReplicationId(replicationId);
                flow.setNode(node);

                conn.createQuery("INSERT INTO replication_flow(replication_id, node, received, extracted, validated, pushed) " +
                        "VALUES (:replicationId, :node, :received, :extracted, :validated, :pushed)")
                        .bind(flow)
                        .executeUpdate();
            }

            return flow;
        }
    }

    /**
     * Get all ReplicationFlow objects in the DB
     *
     * TODO: We could probably wrap this some type of pagination object
     *
     * @param sql2o the sql2o connection
     * @return A list of ReplicationFlow objects
     */
    public static List<ReplicationFlow> getAll(Sql2o sql2o) {
        String select = "SELECT replication_id, node, received, extracted, validated, pushed " +
                "FROM replication_flow ORDER BY rowid DESC";
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(select)
                    .addColumnMapping("REPLICATION_ID", "replicationId")
                    .executeAndFetch(ReplicationFlow.class);
        }
    }
}
