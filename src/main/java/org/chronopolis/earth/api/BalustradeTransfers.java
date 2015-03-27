package org.chronopolis.earth.api;

import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.chronopolis.earth.models.Restore;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.PATCH;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.QueryMap;

import java.util.Map;

/**
 * Created by shake on 3/2/15.
 */
public interface BalustradeTransfers {

    // Replications
    @GET("/api-v1/replicate/")
    Response<Replication> getReplications(@QueryMap Map<String, String> params);

    @POST("/api-v1/replicate/")
    void createReplication(@Body Replication replication);

    @GET("/api-v1/replicate/{id}/")
    Replication getReplication(@Path("id") String id);

    @POST("/api-v1/replicate/{id}/")
    Replication updateReplication(@Path("id") String id, @Body Replication replication);

    @PATCH("/api-v1/replicate/{id}/")
    Replication patchReplication(@Path("id") String id, @Body Replication replication);

    // Restores
    @GET("/api-v1/restore/")
    Response<Restore> getRestores(@QueryMap Map<String, String> params);

    @POST("/api-v1/restore/")
    void createRestore(@Body Restore replication);

    @GET("/api-v1/restore/{id}/")
    Restore getRestore(@Path("id") String id);

    @POST("/api-v1/restore/{id}/")
    Restore updateRestore(@Path("id") String id, @Body Restore replication);

    @PATCH("/api-v1/restore/{id}/")
    Restore patchRestore(@Path("id") String id, @Body Restore replication);




}
