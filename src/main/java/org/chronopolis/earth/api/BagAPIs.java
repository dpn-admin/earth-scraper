package org.chronopolis.earth.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by shake on 4/27/15.
 */
public class BagAPIs {

    private Map<String, BalustradeBag> apiMap;

    public BagAPIs() {
        this.apiMap = new HashMap<>();
    }

    public void put(String node, BalustradeBag bag) {
        apiMap.put(node, bag);
    }

    public Map<String, BalustradeBag> getApiMap() {
        return apiMap;
    }
}
