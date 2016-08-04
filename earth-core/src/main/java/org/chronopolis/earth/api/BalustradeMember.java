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

import static org.chronopolis.earth.api.Version.VERSION;

/**
 * DPN Member API
 *
 * Created by shake on 10/9/15.
 */
public interface BalustradeMember {

    @GET(VERSION + "/member/")
    Call<Response<Member>> getMembers(@QueryMap Map<String, String> params);

    @GET(VERSION + "/member/{uuid}/")
    Call<Member> getMember(@Path("/uuid") String memberUUID);

    @POST(VERSION + "/member/")
    Call<Member> createMember(@Body Member member);

    @PUT(VERSION + "/member/{uuid}/")
    Call<Member> updateMember(@Path("/uuid") String memberUUID, @Body Member member);

}
