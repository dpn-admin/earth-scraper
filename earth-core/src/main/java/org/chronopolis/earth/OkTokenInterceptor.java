package org.chronopolis.earth;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * Token interceptor for OkHttp
 * Can add logging as well... we'll see
 *
 * Created by shake on 1/19/16.
 */
public class OkTokenInterceptor implements Interceptor {

    private static final String AUTHORIZATION = "Authorization";
    private final String token;

    public OkTokenInterceptor(String token) {
        this.token = token;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String tokenAuth = "Token " + token;
        Request authRequest = chain.request().newBuilder()
                .header(AUTHORIZATION, tokenAuth)
                .build();

        return chain.proceed(authRequest);
    }
}
