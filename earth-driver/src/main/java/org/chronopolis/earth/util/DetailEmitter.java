package org.chronopolis.earth.util;

import okhttp3.Request;
import okio.Buffer;
import org.chronopolis.earth.ResponseGetter;
import org.chronopolis.earth.domain.HttpDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Phaser;

/**
 * Based off of the SimpleCallback, a class to emit HttpDetails for us
 *
 * Created by shake on 8/12/16.
 */
public class DetailEmitter<T> implements Callback<T>, ResponseGetter<T> {
    private final Logger log = LoggerFactory.getLogger(DetailEmitter.class);

    private T response;
    private Request rawRequest;
    private int responseCode = -1;
    private String responseBody;
    private Phaser phaser = new Phaser(2);

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        responseCode = response.code();
        this.rawRequest = call.request();
        if (response.isSuccessful()) {
            this.response = response.body();
        } else {
            String error;
            try {
                error = response.errorBody().string();
            } catch (IOException e) {
                log.error("Error creating response body", e);
                error = e.getMessage();
            }

            responseBody = error;
            log.warn("Failed http call: [{}]{} | {}", call.request().method(), call.request().url(), response.code());
        }

        phaser.arriveAndDeregister();
    }

    @Override
    public void onFailure(Call<T> call, Throwable throwable) {
        log.warn("Failed http call: [{}]{}", call.request().method(), call.request().url(), throwable);
        rawRequest = call.request();
        responseBody = throwable.getMessage();

        phaser.arriveAndDeregister();
    }

    public HttpDetail emit() {
        phaser.arriveAndAwaitAdvance();
        HttpDetail detail = new HttpDetail();
        if (rawRequest != null) {
            detail.setUrl(rawRequest.url().toString());
            detail.setRequestMethod(rawRequest.method());
            if (rawRequest.body() != null) {
                try (Buffer b = new Buffer()) {
                    rawRequest.body().writeTo(b);
                    detail.setRequestBody(b.readUtf8());
                } catch (IOException ignored) {
                    log.error("Error emitting http detail", ignored);
                }
            }
        }
        detail.setResponseBody(responseBody);
        detail.setResponseCode(responseCode);
        return detail;
    }

    @Override
    public Optional<T> getResponse() {
        phaser.arriveAndAwaitAdvance();
        return Optional.ofNullable(response);
    }
}
