package org.chronopolis.earth.api;

import org.chronopolis.earth.models.Bag;
import org.chronopolis.earth.models.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.QueryMap;

import java.util.Map;


/**
 * TODO: May be able to inject api version into the interface w/ spring
 *
 * Created by shake on 3/27/15.
 */
public interface BalustradeBag {

    @GET("/api-v1/bag/")
    Response<Bag> getBags(@QueryMap Map<String, String> params);

    @POST("/api-v1/bag/{uuid}")
    Bag createBag(@Path("uuid") String uuid, @Body Bag bag);

    @GET("/api-v1/bag/{uuid}")
    Bag getBag(@Path("uuid") String uuid);

    @PUT("/api-v1/bag/{uuid}")
    Bag updateBag(@Path("uuid") String uuid, @Body Bag bag);

}
