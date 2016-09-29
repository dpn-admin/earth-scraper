package org.chronopolis.earth.domain;


import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * Created by shake on 9/28/16.
 */
@Entity
public class LastSync {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    private SyncType type;

    private String node;
    private ZonedDateTime time;

    public LastSync() {
        // Need to pass in Instant (TemporalAccessor) + Zoned UTC
        time = ZonedDateTime.from(Instant.EPOCH.atZone(ZoneOffset.UTC));
    }

    public Long getId() {
        return id;
    }

    public LastSync setId(Long id) {
        this.id = id;
        return this;
    }

    public String getNode() {
        return node;
    }

    public LastSync setNode(String node) {
        this.node = node;
        return this;
    }

    public SyncType getType() {
        return type;
    }

    public LastSync setType(SyncType type) {
        this.type = type;
        return this;
    }

    public ZonedDateTime getTime() {
        // ensure we always deal with UTC time
        return time.withZoneSameInstant(ZoneOffset.UTC);
    }

    public LastSync setTime(ZonedDateTime time) {
        this.time = time;
        return this;
    }

    public String getFormattedTime() {
        // TODO: TEST
        return getTime().format(DateTimeFormatter.ISO_INSTANT);
    }
}
