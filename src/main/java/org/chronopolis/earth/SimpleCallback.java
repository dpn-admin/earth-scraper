package org.chronopolis.earth;

import com.google.common.collect.Lists;
import org.chronopolis.earth.models.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit.Callback;
import retrofit.RetrofitError;

import java.util.concurrent.CountDownLatch;

/**
 *
 * Created by shake on 7/8/15.
 */
public class SimpleCallback<E> implements Callback<Response<E>>, ResponseGetter<E> {
    private final Logger log = LoggerFactory.getLogger(SimpleCallback.class);

    private Response<E> response;
    private CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void success(Response<E> eResponse, retrofit.client.Response response) {
        this.response = eResponse;
        latch.countDown();
    }

    @Override
    public void failure(RetrofitError retrofitError) {
        this.response = new Response();
        this.response.setResults(Lists.<E>newArrayList());
        latch.countDown();
    }

    @Override
    public Response<E> getResponse() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("Error awaiting latch count down", e);
        }

        return response;
    }
}
