package org.chronopolis.earth.api;

import java.util.Map;

/**
 *
 * Created by shake on 10/9/15.
 */
@Deprecated
public class MemberAPIs {

    private Map<String, BalustradeMember> apiMap;

    public MemberAPIs(Map<String, BalustradeMember> apiMap) {
        this.apiMap = apiMap;
    }

    public void put(String node, BalustradeMember member) {
        apiMap.put(node, member);
    }

    public Map<String, BalustradeMember> getApiMap() {
        return apiMap;
    }

}
