package org.chronopolis.earth;

import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.PATCH;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by shake on 3/2/15.
 */
public interface Balustrade {

    @GET("/api-v1/replicate/")
    Response<Replication> getReplications();

    @POST("/api-v1/replicate/")
    void postReplication(@Body Replication replication);

    @GET("/api-v1/replicate/{id}/")
    Replication getReplication(@Path("id") String id);

    @POST("/api-v1/replicate/{id}/")
    Replication updateReplication(@Path("id") String id, @Body Replication replication);

    @PATCH("/api-v1/replicate/{id}/")
    Replication patchReplication(@Path("id") String id, @Body Replication replication);
}
