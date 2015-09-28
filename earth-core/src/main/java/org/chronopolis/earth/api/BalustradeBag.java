package org.chronopolis.earth.api;

import org.chronopolis.earth.models.Bag;
import org.chronopolis.earth.models.Response;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.QueryMap;

import java.util.Map;


/**
 * DPN API for CRUD operations on bags
 *
 * TODO: May be able to inject api version into the interface w/ spring
 *
 * Created by shake on 3/27/15.
 */
public interface BalustradeBag {

    @GET("/api-v1/bag/")
    Response<Bag> getBags(@QueryMap Map<String, String> params);

    @GET("/api-v1/bag/")
    void getBags(@QueryMap Map<String, String> params, Callback<Response<Bag>> callback);

    @POST("/api-v1/bag/")
    Bag createBag(@Body Bag bag);

    @POST("/api-v1/bag/")
    void createBag(@Body Bag bag, Callback<Bag> callback);

    @GET("/api-v1/bag/{uuid}")
    Bag getBag(@Path("uuid") String uuid);

    @GET("/api-v1/bag/{uuid}")
    void getBag(@Path("uuid") String uuid, Callback<Bag> callback);

    @PUT("/api-v1/bag/{uuid}")
    Bag updateBag(@Path("uuid") String uuid, @Body Bag bag);

    @PUT("/api-v1/bag/{uuid}")
    void updateBag(@Path("uuid") String uuid, @Body Bag bag, Callback<Bag> callback);

}
