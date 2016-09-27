package org.chronopolis.earth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Phaser;

/**
 * Implementation of a Callback and ResponseGetter
 * TODO: Could still check out Monitor, it may reduce some of the cognitive load
 *
 * Upon receiving the HTTP response we save the object or
 * log the error
 *
 * Created by shake on 7/8/15.
 */
public class SimpleCallback<E> implements Callback<E>, ResponseGetter<E> {
    private final Logger log = LoggerFactory.getLogger(SimpleCallback.class);

    private E response;

    /**
     * A quick note about our phaser:
     * We start with 2 parties (the response/failure and client request)
     * When the initial response gets back, we deregister so that we can
     * continue to make calls to getResponse
     *
     */
    private Phaser phaser = new Phaser(2);

    @Override
    public void onResponse(Call<E> call, Response<E> response) {
        if (response.isSuccessful()) {
            // TODO: HTTP {GET/POST/PUT/etc}
            log.debug("Successfully completed HTTP Call with response code {} - {} ",
                response.code(),
                response.message());

            this.response = response.body();
        } else {
            String errorBody;
            try {
                errorBody = response.errorBody().string();
            } catch (IOException e) {
                errorBody = e.getMessage();
                log.error("Error writing response", e);
            }

            log.warn("HTTP call was not successful {}", response.code(), errorBody);
        }

        phaser.arriveAndDeregister();
    }

    @Override
    public void onFailure(Call<E> call, Throwable throwable) {
        log.error("Error in HTTP Call: ", throwable);
        phaser.arriveAndDeregister();
    }

    @Override
    public Optional<E> getResponse() {
        phaser.arriveAndAwaitAdvance();
        return Optional.ofNullable(response);
    }

}
