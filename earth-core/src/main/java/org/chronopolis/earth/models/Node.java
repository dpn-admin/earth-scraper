package org.chronopolis.earth.models;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Node representation for DPN
 *
 * Created by shake on 3/27/15.
 */
@SuppressWarnings("WeakerAccess")
public class Node {

    String name;
    String namespace;
    String apiRoot;
    String sshPubkey;
    List<String> replicateFrom;
    List<String> replicateTo;
    List<String> restoreFrom;
    List<String> restoreTo;
    List<String> protocols;
    List<String> fixityAlgorithms;
    ZonedDateTime createdAt;
    ZonedDateTime updatedAt;
    Storage storage;

    public Node() {
    }

    public String getName() {
        return name;
    }

    public Node setName(String name) {
        this.name = name;
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public Node setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getApiRoot() {
        return apiRoot;
    }

    public Node setApiRoot(String apiRoot) {
        this.apiRoot = apiRoot;
        return this;
    }

    public String getSshPubkey() {
        return sshPubkey;
    }

    public Node setSshPubkey(String sshPubkey) {
        this.sshPubkey = sshPubkey;
        return this;
    }

    public List<String> getReplicateFrom() {
        return replicateFrom;
    }

    public Node setReplicateFrom(List<String> replicateFrom) {
        this.replicateFrom = replicateFrom;
        return this;
    }

    public List<String> getReplicateTo() {
        return replicateTo;
    }

    public Node setReplicateTo(List<String> replicateTo) {
        this.replicateTo = replicateTo;
        return this;
    }

    public List<String> getRestoreFrom() {
        return restoreFrom;
    }

    public Node setRestoreFrom(List<String> restoreFrom) {
        this.restoreFrom = restoreFrom;
        return this;
    }

    public List<String> getRestoreTo() {
        return restoreTo;
    }

    public Node setRestoreTo(List<String> restoreTo) {
        this.restoreTo = restoreTo;
        return this;
    }

    public List<String> getProtocols() {
        return protocols;
    }

    public Node setProtocols(List<String> protocols) {
        this.protocols = protocols;
        return this;
    }

    public List<String> getFixityAlgorithms() {
        return fixityAlgorithms;
    }

    public Node setFixityAlgorithms(List<String> fixityAlgorithms) {
        this.fixityAlgorithms = fixityAlgorithms;
        return this;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public Node setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Node setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Storage getStorage() {
        return storage;
    }

    public Node setStorage(Storage storage) {
        this.storage = storage;
        return this;
    }

    public Node setStorage(String region, String type) {
        this.storage = new Storage(region, type);
        return this;
    }

    public Storage newStorage(String region, String type) {
        return new Storage(region, type);
    }

    // Storage class to encapsulate the object

    public class Storage {
        String region;
        String type;

        public Storage(String region, String type) {
            this.region = region;
            this.type = type;
        }

        public String getRegion() {
            return region;
        }

        public Storage setRegion(String region) {
            this.region = region;
            return this;
        }

        public String getType() {
            return type;
        }

        public Storage setType(String type) {
            this.type = type;
            return this;
        }
    }

}
