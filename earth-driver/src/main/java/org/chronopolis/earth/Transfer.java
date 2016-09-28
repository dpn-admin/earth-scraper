package org.chronopolis.earth;

/**
 * @deprecated old. no longer used. slated for removal in 2.0.0-RELEASE
 *
 * Created by shake on 11/13/14.
 */
@Deprecated
public class Transfer {
    String node;
    String dpn_object_id;
    String status;
    String event_id;
    String protocol;
    String link;
    long size;
    String receipt;
    String fixity;
    String valid;
    String created_on;
    String updated_on;

    public String getNode() {
        return node;
    }

    public void setNode(final String node) {
        this.node = node;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getDpn_object_id() {
        return dpn_object_id;
    }

    public void setDpn_object_id(final String dpn_object_id) {
        this.dpn_object_id = dpn_object_id;
    }

    public String getEvent_id() {
        return event_id;
    }

    public void setEvent_id(final String event_id) {
        this.event_id = event_id;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public String getLink() {
        return link;
    }

    public void setLink(final String link) {
        this.link = link;
    }

    public long getSize() {
        return size;
    }

    public void setSize(final long size) {
        this.size = size;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(final String receipt) {
        this.receipt = receipt;
    }

    public String getFixity() {
        return fixity;
    }

    public void setFixity(final String fixity) {
        this.fixity = fixity;
    }

    public String getValid() {
        return valid;
    }

    public void setValid(final String valid) {
        this.valid = valid;
    }

    public String getCreated_on() {
        return created_on;
    }

    public void setCreated_on(final String created_on) {
        this.created_on = created_on;
    }

    public String getUpdated_on() {
        return updated_on;
    }

    public void setUpdated_on(final String updated_on) {
        this.updated_on = updated_on;
    }
}
