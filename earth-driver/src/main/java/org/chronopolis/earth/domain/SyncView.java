package org.chronopolis.earth.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by shake on 8/5/16.
 */
@Entity
public class SyncView {

    @Id
    @GeneratedValue
    private Long id;

    private String host;

    @Enumerated(EnumType.STRING)
    private SyncType type;

    @Enumerated(EnumType.STRING)
    private SyncStatus status;

    @OneToMany(fetch = FetchType.EAGER)
    private List<HttpDetail> httpDetails = new ArrayList<>();

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

}
