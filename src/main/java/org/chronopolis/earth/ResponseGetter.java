package org.chronopolis.earth;

import org.chronopolis.earth.models.Response;

/**
 * Created by shake on 7/8/15.
 */
public interface ResponseGetter<E> {

    Response<E> getResponse();

}
