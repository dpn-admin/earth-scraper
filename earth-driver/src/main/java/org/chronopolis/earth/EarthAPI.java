package org.chronopolis.earth;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.QueryMap;

import java.util.Map;

/**
 * Created by shake on 11/13/14.
 */
@Deprecated
public interface EarthAPI {

    @GET("/api-v1/transfer/")
    // @Headers("Accept: */*") // needed to get back JSON
    TransferResponse getTransfers(@QueryMap Map<String, String> options);

    @GET("/api-v1/transfer/{id}")
    Transfer getTransfer(@Path("id") String eventId);

    // We need the trailing slash so that the server doesn't return a 3xx status
    @PUT("/api-v1/transfer/{id}/")
    Transfer updateTransfer(@Path("id") String eventId, @Body Transfer transfer);

}
