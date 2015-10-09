package org.chronopolis.earth.api;

import org.chronopolis.earth.models.Member;
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
 * Created by shake on 10/9/15.
 */
public interface BalustradeMember {

    @GET("/api-v1/member/")
    void getMembers(@QueryMap Map<String, String> params, Callback<Response<Member>> cb);

    @GET("/api-v1/member/{uuid}/")
    void getMember(@Path("/uuid") String memberUUID, Callback<Response<Member>> cb);

    @POST("/api-v1/member/")
    void createMember(@Body Member member, Callback<Response<Member>> cb);

    @PUT("/api-v1/member/{uuid}/")
    void updateMember(@Path("/uuid") String memberUUID, @Body Member member, Callback<Response<Member>> cb);

}
