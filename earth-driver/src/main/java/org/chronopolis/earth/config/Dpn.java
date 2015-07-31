package org.chronopolis.earth.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Small class to encapsulate our Dpn Settings
 *
 */
public class Dpn {
    private Endpoint local;
    private List<Endpoint> remote = new ArrayList<>();

    public Endpoint getLocal() {
        return local;
    }

    public Dpn setLocal(Endpoint local) {
        this.local = local;
        return this;
    }

    public List<Endpoint> getRemote() {
        return remote;
    }
}
