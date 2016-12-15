package org.chronopolis.earth.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parent sync object to keep track of sync operations
 * over an entire node
 *
 * Created by shake on 10/5/16.
 */
@Entity
public class Sync {

    @Transient
    private final Logger log = LoggerFactory.getLogger(Sync.class);

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(value = EnumType.STRING)
    private SyncStatus status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SyncOp> ops = new ArrayList<>();

    private String host;

    public Long getId() {
        return id;
    }

    public Sync setId(Long id) {
        this.id = id;
        return this;
    }

    public SyncStatus getStatus() {
        return status;
    }

    public Sync setStatus(SyncStatus status) {
        this.status = status;
        return this;
    }

    public List<SyncOp> getOps() {
        return ops;
    }

    public Sync setOps(List<SyncOp> ops) {
        this.ops = ops;
        return this;
    }

    public Sync addOp(SyncOp op) {
        this.ops.add(op);
        return this;
    }

    public String getHost() {
        return host;
    }

    public Sync setHost(String host) {
        this.host = host;
        return this;
    }

    public void updateStatus() {
        List<SyncOp> success = getOps().stream()
                .filter(x -> x.getStatus() == SyncStatus.SUCCESS)
                .collect(Collectors.toList());
        if(success.isEmpty()) {
            log.trace("NO SUCCESS, FAILING");
            setStatus(SyncStatus.FAIL);
        } else if (success.size() == getOps().size()) {
            log.trace("ALL SUCCESS");
            setStatus(SyncStatus.SUCCESS);
        } else {
            log.trace("WARN");
            setStatus(SyncStatus.WARN);
        }
    }

}
