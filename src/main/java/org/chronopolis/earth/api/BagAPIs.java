package org.chronopolis.earth.api;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by shake on 4/27/15.
 */
public class BagAPIs {

    public Set<BalustradeBag> apis;

    public BagAPIs() {
        this.apis = new HashSet<>();
    }

    public void add(BalustradeBag bag) {
        apis.add(bag);
    }
}
