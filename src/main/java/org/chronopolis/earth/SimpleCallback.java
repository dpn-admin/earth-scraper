package org.chronopolis.earth;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit.Callback;
import retrofit.RetrofitError;

import java.util.concurrent.CountDownLatch;

/**
 * Implementation of a Callback and ResponseGetter
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
    public void success(E eResponse, retrofit.client.Response response) {
        this.response = Optional.of(eResponse);
        latch.countDown();
    }

    @Override
    public void failure(RetrofitError retrofitError) {
        String errorType = retrofitError.getKind().toString();
        if (retrofitError.getKind() == RetrofitError.Kind.HTTP) {
            errorType += " - " + retrofitError.getResponse().getStatus();
        }
        log.error("Error in HTTP call: [{}] {}", errorType, retrofitError.getUrl());
        log.debug("", retrofitError.getCause());
        this.response = Optional.absent();
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
