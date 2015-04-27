package org.chronopolis.earth.api;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by shake on 4/27/15.
 */
public class NodeAPIs {

    public Set<BalustradeNode> apis;

    public NodeAPIs() {
        this.apis = new HashSet<>();
    }

    public void add(BalustradeNode node) {
        apis.add(node);
    }

}
