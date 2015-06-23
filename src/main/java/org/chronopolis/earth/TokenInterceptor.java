package org.chronopolis.earth;

import retrofit.RequestInterceptor;

/**
 * {@link RequestInterceptor} to add the token authorization header for DPN API
 * calls
 *
 * Created by shake on 11/13/14.
 */
public class TokenInterceptor implements RequestInterceptor {

    private String token;

    public TokenInterceptor(final String token) {
        this.token = token;
    }


    @Override
    public void intercept(final RequestFacade requestFacade) {
        String tokenAuth = "token " + token;
        requestFacade.addHeader("Authorization", tokenAuth);

        // needed to get back JSON
        // could also use application/json
        requestFacade.addHeader("Accept", "*/*");
    }
}
