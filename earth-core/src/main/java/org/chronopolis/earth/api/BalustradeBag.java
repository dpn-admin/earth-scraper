package org.chronopolis.earth.api;

import org.chronopolis.earth.models.Bag;
import org.chronopolis.earth.models.Digest;
import org.chronopolis.earth.models.Response;
import org.chronopolis.earth.models.SumResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.Map;


/**
 * DPN API for CRUD operations on bags
 *
 * TODO: May be able to inject api version into the interface w/ spring
 *
 * Created by shake on 3/27/15.
 */
public interface BalustradeBag {

    @GET("api-v2/bag")
    Call<SumResponse<Bag>> getBags(@QueryMap Map<String, String> params);

    @POST("api-v2/bag")
    Call<Bag> createBag(@Body Bag bag);

    @GET("api-v2/bag/{uuid}")
    Call<Bag> getBag(@Path("uuid") String uuid);

    @PUT("api-v2/bag/{uuid}")
    Call<Bag> updateBag(@Path("uuid") String uuid, @Body Bag bag);

    @GET("api-v2/bag/{uuid}/digest")
    Call<Response<Digest>> getDigests(@QueryMap Map<String, String> params);

    @POST("api-v2/bag/{uuid}/digest")
    Call<Digest> createDigest(@Path("uuid") String uuid, @Body Digest digest);

    @GET("api-v2/bag/{uuid}/digest/{algorithm}")
    Call<Digest> getDigest(@Path("uuid") String uuid, @Path("algorithm") String algorithm);

}
