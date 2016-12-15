package org.chronopolis.earth.api;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by shake on 8/3/16.
 */
@Deprecated
public class EventAPIs {
    private Map<String, Events> apiMap;

    public EventAPIs() {
        this.apiMap = new HashMap<>();
    }

    public void put(String key, Events val) {
        apiMap.put(key, val);
    }

    public Map<String, Events> getApiMap() {
        return apiMap;
    }
}
