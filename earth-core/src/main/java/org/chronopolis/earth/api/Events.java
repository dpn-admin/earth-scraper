package org.chronopolis.earth.api;

import org.chronopolis.earth.models.Digest;
import org.chronopolis.earth.models.FixityCheck;
import org.chronopolis.earth.models.Ingest;
import org.chronopolis.earth.models.Response;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;

import java.util.Map;

/**
 * Events in DPN
 *   Ingest
 *   Fixity Check
 *   Digests
 *
 * May look in to using rxjava in place of Call
 *
 * Created by shake on 8/3/16.
 */
public interface Events {

    @GET("api-v2/ingest")
    Call<Response<Ingest>> getIngests(@QueryMap Map<String, String> params); // TODO: 8/3/16 Map<IngestParams, String>

    @POST("api-v2/ingest")
    Call<Ingest> createIngest(@Body Ingest ingest);

    @GET("api-v2/fixity_check")
    Call<Response<FixityCheck>> getFixityChecks(@QueryMap Map<String, String> params); // TODO: 8/3/16 Map<IngestParams, String>

    @POST("api-v2/fixity_check")
    Call<FixityCheck> createFixityCheck(@Body FixityCheck fixityCheck);

    @GET("api-v2/digest")
    Call<Response<Digest>> getDigests(@QueryMap Map<String, String> params);

}
