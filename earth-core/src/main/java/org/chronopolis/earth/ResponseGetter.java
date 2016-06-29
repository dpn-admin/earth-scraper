package org.chronopolis.earth;

import java.util.Optional;

/**
 * Interface to allow our callback to have a predefined getter
 *
 * Created by shake on 7/8/15.
 */
public interface ResponseGetter<E> {

    Optional<E> getResponse();

}
