package org.chronopolis.earth.api;

/**
 * Hold our local rest adapters...
 *
 * Created by shake on 7/15/15.
 */
public class LocalAPI {

    String node;
    BalustradeBag bagAPI;
    BalustradeNode nodeAPI;
    BalustradeTransfers transfersAPI;

    public LocalAPI() {
    }

    public String getNode() {
        return node;
    }

    public LocalAPI setNode(String node) {
        this.node = node;
        return this;
    }

    public BalustradeBag getBagAPI() {
        return bagAPI;
    }

    public LocalAPI setBagAPI(BalustradeBag bagAPI) {
        this.bagAPI = bagAPI;
        return this;
    }

    public BalustradeNode getNodeAPI() {
        return nodeAPI;
    }

    public LocalAPI setNodeAPI(BalustradeNode nodeAPI) {
        this.nodeAPI = nodeAPI;
        return this;
    }

    public BalustradeTransfers getTransfersAPI() {
        return transfersAPI;
    }

    public LocalAPI setTransfersAPI(BalustradeTransfers transfersAPI) {
        this.transfersAPI = transfersAPI;
        return this;
    }
}
