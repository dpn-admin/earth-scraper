package org.chronopolis.earth.domain;

import org.chronopolis.earth.domain.handler.SyncViewListHandler;
import org.chronopolis.earth.domain.handler.SyncViewSingleHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by shake on 8/5/16.
 */
public class SyncView {
    private final Logger log = LoggerFactory.getLogger(SyncView.class);

    private Long id;
    private String host;
    private List<HttpDetail> httpDetails = new ArrayList<>();

    private SyncType type;
    private SyncStatus status;

    public Long getId() {
        return id;
    }

    public SyncView setId(Long id) {
        this.id = id;
        return this;
    }

    public String getHost() {
        return host;
    }

    public SyncView setHost(String host) {
        this.host = host;
        return this;
    }

    public List<HttpDetail> getHttpDetails() {
        return httpDetails;
    }

    public SyncView setHttpDetails(List<HttpDetail> httpDetails) {
        this.httpDetails = httpDetails;
        return this;
    }

    public SyncView addHttpDetail(HttpDetail detail) {
        httpDetails.add(detail);
        return this;
    }

    public SyncType getType() {
        return type;
    }

    public SyncView setType(SyncType type) {
        this.type = type;
        return this;
    }

    public SyncStatus getStatus() {
        return status;
    }

    public SyncView setStatus(SyncStatus status) {
        this.status = status;
        return this;
    }


    // DB Ops

    /**
     * Insert a SyncView in to the database. For the most part these are in their final state
     * when they reach us, so there's no real point to return it or try to query it on the get.
     *
     * One thing we might want is to do batch inserts, which looks like it will require two
     * Query objects, one for the view and one for the details. That will be for another day, though.
     *
     * @param sql2o the connection to the database
     */
    public void insert(Sql2o sql2o) {
        String insertView = "INSERT INTO sync_view(host, type, status) VALUES(:host, :type, :status)";
        try (Connection conn = sql2o.open()) {
            // log.debug("Creating sync view for {}:{}", host, type);
            Long key = conn.createQuery(insertView)
                    .addParameter("host", host)
                    .addParameter("type", type.toString())
                    .addParameter("status", status.toString())
                    .executeUpdate().getKey(Long.class);
            httpDetails.forEach(d -> d.insert("sync", key, conn));
        }
    }

    /**
     * Get a single SyncView based on it's Id. Not really sure what the purpose of this is.
     *
     * @param id the id of the view to retrieve
     * @param sql2o the connection to the database
     * @return the SyncView found
     */
    public static SyncView get(Long id, Sql2o sql2o) {
        String select = "SELECT * FROM sync_view INNER JOIN http_detail ON sync_view.sync_id = http_detail.sync WHERE sync_view.sync_id = :syncId";

        try (Connection conn = sql2o.open()) {
            return conn.createQuery(select)
                    .addParameter("syncId", id)
                    .executeAndFetchFirst(new SyncViewSingleHandler());
        }
    }

    /**
     * Get all SyncViews. May need to do FetchLazy in the Future.
     *
     * @param sql2o the connection to the database
     * @return the SyncView found
     */
    public static List<SyncView> getAll(Sql2o sql2o) {
        String select = "SELECT * FROM sync_view LEFT JOIN http_detail ON sync_view.sync_id = http_detail.sync ORDER BY sync_id DESC";

        try (Connection conn = sql2o.open()) {
            return conn.createQuery(select)
                    .executeAndFetchFirst(new SyncViewListHandler());
        }
    }

}
