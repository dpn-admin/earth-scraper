package org.chronopolis.earth.models;


import java.time.ZonedDateTime;
import java.util.List;

/**
 * Representation of a DPN bag
 *
 * Created by shake on 3/27/15.
 */
public class Bag {

    private String uuid;
    private String localId;
    private Long size;
    private String firstVersionUuid;
    private String ingestNode;
    private String adminNode;
    private Long version;
    private char bagType;
    private List<String> interpretive;
    private List<String> rights;
    private List<String> replicatingNodes;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private String member;

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

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public Bag setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Bag setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public String getMember() {
        return member;
    }

    public Bag setMember(String member) {
        this.member = member;
        return this;
    }
}
