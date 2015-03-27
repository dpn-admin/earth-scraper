package org.chronopolis.earth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by shake on 11/14/14.
 */
@Component
public class EarthSettings {

    @Value("${auth.key:5cecab71a3d952df9083b2c51ba4a2c5a664d526}")
    // @Value("${auth.key:081f1b0f37be8923502fd54c54eb381c9318092b}")
    String authorizationKey;

    public String getAuthorizationKey() {
        return authorizationKey;
    }

    public void setAuthorizationKey(final String authorizationKey) {
        this.authorizationKey = authorizationKey;
    }
}
