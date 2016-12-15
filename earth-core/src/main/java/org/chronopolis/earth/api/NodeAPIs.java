package org.chronopolis.earth.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulation for {@link BalustradeNode} apis and the associated node
 *
 * Created by shake on 4/27/15.
 */
@Deprecated
public class NodeAPIs {

    private Map<String, BalustradeNode> apiMap;

    public NodeAPIs() {
        this.apiMap = new HashMap<>();
    }

    public void put(String node, BalustradeNode api) {
        apiMap.put(node, api);
    }

    public Map<String, BalustradeNode> getApiMap() {
        return apiMap;
    }
}
