package org.chronopolis.earth.api;

import java.util.HashSet;
import java.util.Set;

/**
 * Needed because spring refuses to autowire sets with generics
 *
 * Created by shake on 4/27/15.
 */
public class TransferAPIs {

    public Set<BalustradeTransfers> apis;

    public TransferAPIs() {
        this.apis = new HashSet<>();
    }

    public void add(BalustradeTransfers transfer) {
        apis.add(transfer);
    }
}
