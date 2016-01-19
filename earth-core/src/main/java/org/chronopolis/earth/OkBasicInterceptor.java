package org.chronopolis.earth;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;

/**
 * Basic Auth interceptor for OkHttp
 *
 * Created by shake on 1/19/16.
 */
public class OkBasicInterceptor implements Interceptor {

    private final String username;
    private final String password;

    public OkBasicInterceptor(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String credentials = username + ":" + password;
        String basicAuth = "Basic " + Base64.encodeBase64String(credentials.getBytes());

        Request request = chain.request().newBuilder().header("Authorization", basicAuth).build();

        return chain.proceed(request);
    }
}
