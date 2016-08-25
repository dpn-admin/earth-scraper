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

import static org.chronopolis.earth.api.Version.VERSION;


/**
 * DPN API for CRUD operations on bags
 *
 * TODO: Figure out if we want to support getting the total in a single call via SumResponse
 *       Or if we want to delegate that to something else later because it's not needed in
 *       this clients functionality
 *
 * Created by shake on 3/27/15.
 */
public interface BalustradeBag {

    @GET(VERSION + "/bag")
    Call<SumResponse<Bag>> getBags(@QueryMap Map<String, String> params);

    @POST(VERSION + "/bag")
    Call<Bag> createBag(@Body Bag bag);

    @GET(VERSION + "/bag/{uuid}")
    Call<Bag> getBag(@Path("uuid") String uuid);

    @PUT(VERSION + "/bag/{uuid}")
    Call<Bag> updateBag(@Path("uuid") String uuid, @Body Bag bag);

    // Move to Events?
    @GET(VERSION + "/bag/{uuid}/digest")
    Call<Response<Digest>> getDigests(@QueryMap Map<String, String> params);

    @POST(VERSION + "/bag/{uuid}/digest")
    Call<Digest> createDigest(@Path("uuid") String uuid, @Body Digest digest);

    @GET(VERSION + "/bag/{uuid}/digest/{algorithm}")
    Call<Digest> getDigest(@Path("uuid") String uuid, @Path("algorithm") String algorithm);

}
