package org.chronopolis.earth.api;

import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.chronopolis.earth.models.Restore;
import org.springframework.beans.factory.annotation.Value;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.PATCH;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.QueryMap;

import java.util.Map;

/**
 * DPN API for CRUD operations on {@link Replication} and {@link Restore} objects
 *
 * Created by shake on 3/2/15.
 */
public interface BalustradeTransfers {

    // Replications
    @GET("/api-v1/replicate/")
    Response<Replication> getReplications(@QueryMap Map<String, String> params);

    @GET("/api-v1/replicate/")
    void getReplications(@QueryMap Map<String, String> params, Callback<Response<Replication>> callback);

    @POST("/api-v1/replicate/")
    Replication createReplication(@Body Replication replication);

    @POST("/api-v1/replicate/")
    void createReplication(@Body Replication replication, Callback<Void> callback);

    @GET("/api-v1/replicate/{id}/")
    Replication getReplication(@Path("id") String id);

    @GET("/api-v1/replicate/{id}/")
    void getReplication(@Path("id") String id, Callback<Replication> callback);

    @PUT("/api-v1/replicate/{id}/")
    Replication updateReplication(@Path("id") String id, @Body Replication replication);

    @PUT("/api-v1/replicate/{id}/")
    void updateReplication(@Path("id") String id, @Body Replication replication, Callback<Replication> callback);

    @PATCH("/api-v1/replicate/{id}/")
    Replication patchReplication(@Path("id") String id, @Body Replication replication);

    @PATCH("/api-v1/replicate/{id}/")
    void patchReplication(@Path("id") String id, @Body Replication replication, Callback<Replication> callback);

    // Restores
    @GET("/api-v1/restore/")
    Response<Restore> getRestores(@QueryMap Map<String, String> params);

    @GET("/api-v1/restore/")
    void getRestores(@QueryMap Map<String, String> params, Callback<Response<Restore>> callback);

    @POST("/api-v1/restore/")
    Restore createRestore(@Body Restore replication);

    @POST("/api-v1/restore/")
    void createRestore(@Body Restore replication, Callback<Void> callback);

    @GET("/api-v1/restore/{id}/")
    Restore getRestore(@Path("id") String id);

    @GET("/api-v1/restore/{id}/")
    void getRestore(@Path("id") String id, Callback<Restore> callback);

    @PUT("/api-v1/restore/{id}/")
    Restore updateRestore(@Path("id") String id, @Body Restore replication);

    @PUT("/api-v1/restore/{id}/")
    void updateRestore(@Path("id") String id, @Body Restore replication, Callback<Restore> callback);

    @PATCH("/api-v1/restore/{id}/")
    Restore patchRestore(@Path("id") String id, @Body Restore replication);

    @PATCH("/api-v1/restore/{id}/")
    void patchRestore(@Path("id") String id, @Body Restore replication, Callback<Restore> callback);

}
