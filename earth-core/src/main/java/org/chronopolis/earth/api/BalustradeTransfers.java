package org.chronopolis.earth.api;

import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.chronopolis.earth.models.Restore;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.Map;

/**
 * DPN API for CRUD operations on {@link Replication} and {@link Restore} objects
 *
 * Created by shake on 3/2/15.
 */
public interface BalustradeTransfers {

    // Replications
    @GET("api-v1/replicate/")
    Call<Response<Replication>> getReplications(@QueryMap Map<String, String> params);

    @POST("api-v1/replicate/")
    Call<Replication> createReplication(@Body Replication replication);

    @GET("api-v1/replicate/{id}/")
    Call<Replication> getReplication(@Path("id") String id);

    @PUT("api-v1/replicate/{id}/")
    Call<Replication> updateReplication(@Path("id") String id, @Body Replication replication);

    @PATCH("api-v1/replicate/{id}/")
    Call<Replication> patchReplication(@Path("id") String id, @Body Replication replication);

    // Restores
    @GET("api-v1/restore/")
    Call<Response<Restore>> getRestores(@QueryMap Map<String, String> params);

    @POST("api-v1/restore/")
    Call<Restore> createRestore(@Body Restore replication);

    @GET("api-v1/restore/{id}/")
    Call<Restore> getRestore(@Path("id") String id);

    @PUT("api-v1/restore/{id}/")
    Call<Restore> updateRestore(@Path("id") String id, @Body Restore replication);

    @PATCH("api-v1/restore/{id}/")
    Call<Restore> patchRestore(@Path("id") String id, @Body Restore replication);

}
