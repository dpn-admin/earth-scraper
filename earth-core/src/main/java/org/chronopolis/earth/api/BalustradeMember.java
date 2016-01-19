package org.chronopolis.earth.api;

import org.chronopolis.earth.models.Member;
import org.chronopolis.earth.models.Response;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.Map;

/**
 * DPN Member API
 *
 * Created by shake on 10/9/15.
 */
public interface BalustradeMember {

    @GET("api-v1/member/")
    Call<Response<Member>> getMembers(@QueryMap Map<String, String> params);

    @GET("api-v1/member/{uuid}/")
    Call<Member> getMember(@Path("/uuid") String memberUUID);

    @POST("api-v1/member/")
    Call<Member> createMember(@Body Member member);

    @PUT("api-v1/member/{uuid}/")
    Call<Member> updateMember(@Path("/uuid") String memberUUID, @Body Member member);

}
