package org.chronopolis.earth.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Needed because spring refuses to autowire sets with generics
 *
 * Created by shake on 4/27/15.
 */
public class TransferAPIs {

    private Map<String, BalustradeTransfers> apiMap;

    public TransferAPIs() {
        this.apiMap = new HashMap<>();
    }

    public void put(String node, BalustradeTransfers transfers) {
        apiMap.put(node, transfers);
    }

    public Map<String, BalustradeTransfers> getApiMap() {
        return apiMap;
    }
}
