package org.chronopolis.earth.domain;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

/**
 * A sync operation for a specific data type
 *
 * Created by shake on 10/5/16.
 */
@Entity
public class SyncOp {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Sync parent;

    @Enumerated(value = EnumType.STRING)
    private SyncType type;

    @Enumerated(value = EnumType.STRING)
    private SyncStatus status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HttpDetail> details = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public SyncOp setId(Long id) {
        this.id = id;
        return this;
    }

    public Sync getParent() {
        return parent;
    }

    public SyncOp setParent(Sync parent) {
        this.parent = parent;
        return this;
    }

    public List<HttpDetail> getDetails() {
        return details;
    }

    public SyncOp setDetails(List<HttpDetail> details) {
        this.details = details;
        return this;
    }

    public SyncOp addDetail(HttpDetail detail) {
        this.details.add(detail);
        return this;
    }

    public SyncType getType() {
        return type;
    }

    public SyncOp setType(SyncType type) {
        this.type = type;
        return this;
    }

    public SyncStatus getStatus() {
        return status;
    }

    public SyncOp setStatus(SyncStatus status) {
        this.status = status;
        return this;
    }

}
