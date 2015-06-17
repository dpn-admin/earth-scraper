package org.chronopolis.earth;

import org.apache.commons.codec.binary.Base64;
import retrofit.RequestInterceptor;

/**
 * RequestInterceptor to give us Basic auth
 *
 * Created by shake on 6/17/15.
 */
public class CredentialRequestInterceptor implements RequestInterceptor {
    private final String username;
    private final String password;

    public CredentialRequestInterceptor(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void intercept(RequestFacade requestFacade) {
        String credentials = username + ":" + password;
        String basicAuth = "Basic " + Base64.encodeBase64String(credentials.getBytes());

        requestFacade.addHeader("Authorization", basicAuth);
    }

}
