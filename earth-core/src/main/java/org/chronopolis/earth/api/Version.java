package org.chronopolis.earth.api;

/**
 * Holder to set the version number externally
 *
 * Created by shake on 8/4/16.
 */
public class Version {

    private Version() {
        throw new IllegalAccessError("Not supported");
    }

    final static String VERSION = "api-v2";

}
