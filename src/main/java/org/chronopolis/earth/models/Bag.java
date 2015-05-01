package org.chronopolis.earth.models;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

/**
 * Created by shake on 3/27/15.
 */
public class Bag {

    String uuid;
    String localId;
    Long size;
    String firstVersionUuid;
    String ingestNode;
    String adminNode;
    Long version;
    char bagType;
    List<String> interpretive;
    List<String> rights;
    List<String> replicatingNodes;
    Map<String, String> fixities;
    DateTime createdAt;
    DateTime updatedAt;

    public Bag() {
    }

    public String getUuid() {
        return uuid;
    }

    public Bag setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getLocalId() {
        return localId;
    }

    public Bag setLocalId(String localId) {
        this.localId = localId;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public Bag setSize(Long size) {
        this.size = size;
        return this;
    }

    public String getFirstVersionUuid() {
        return firstVersionUuid;
    }

    public Bag setFirstVersionUuid(String firstVersionUuid) {
        this.firstVersionUuid = firstVersionUuid;
        return this;
    }

    public String getIngestNode() {
        return ingestNode;
    }

    public Bag setIngestNode(String ingestNode) {
        this.ingestNode = ingestNode;
        return this;
    }

    public String getAdminNode() {
        return adminNode;
    }

    public Bag setAdminNode(String adminNode) {
        this.adminNode = adminNode;
        return this;
    }

    public Long getVersion() {
        return version;
    }

    public Bag setVersion(Long version) {
        this.version = version;
        return this;
    }

    public char getBagType() {
        return bagType;
    }

    public Bag setBagType(char bagType) {
        this.bagType = bagType;
        return this;
    }

    public List<String> getInterpretive() {
        return interpretive;
    }

    public Bag setInterpretive(List<String> interpretive) {
        this.interpretive = interpretive;
        return this;
    }

    public List<String> getRights() {
        return rights;
    }

    public Bag setRights(List<String> rights) {
        this.rights = rights;
        return this;
    }

    public List<String> getReplicatingNodes() {
        return replicatingNodes;
    }

    public Bag setReplicatingNodes(List<String> replicatingNodes) {
        this.replicatingNodes = replicatingNodes;
        return this;
    }

    public Map<String, String> getFixities() {
        return fixities;
    }

    public Bag setFixities(Map<String, String> fixities) {
        this.fixities = fixities;
        return this;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public Bag setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public DateTime getUpdatedAt() {
        return updatedAt;
    }

    public Bag setUpdatedAt(DateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
