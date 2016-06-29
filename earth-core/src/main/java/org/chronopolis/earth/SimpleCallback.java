package org.chronopolis.earth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * Implementation of a Callback and ResponseGetter
 * TODO: Try using a Monitor from guava instead of a countdown latch
 *       It should allow us to use a callback multiple times
 *
 * Upon receiving the HTTP response we save the object or
 * log the error
 *
 * Created by shake on 7/8/15.
 */
public class SimpleCallback<E> implements Callback<E>, ResponseGetter<E> {
    private final Logger log = LoggerFactory.getLogger(SimpleCallback.class);

    private Optional<E> response;
    private CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void onResponse(Response<E> response) {
        if (response.isSuccess()) {
            // TODO: HTTP {GET/POST/PUT/etc}
            log.debug("Successfully completed HTTP Call with response code {} - {} ",
                response.code(),
                response.message());

            this.response = Optional.of(response.body());
        } else {
            String errorBody;
            try {
                errorBody = response.errorBody().string();
            } catch (IOException e) {
                errorBody = e.getMessage();
            }

            log.warn("HTTP call was not successful {}", response.code(), errorBody);
            this.response = Optional.empty();
        }

        latch.countDown();
    }

    @Override
    public void onFailure(Throwable throwable) {
        log.error("Error in HTTP Call: ", throwable);
        this.response = Optional.empty();
        latch.countDown();
    }

    @Override
    public Optional<E> getResponse() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("Error awaiting latch count down", e);
        }

        return response;
    }

}
