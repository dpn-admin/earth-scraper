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

import static org.chronopolis.earth.api.Version.VERSION;

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

    @GET(VERSION + "/ingest")
    Call<Response<Ingest>> getIngests(@QueryMap Map<String, String> params); // TODO: 8/3/16 Map<IngestParams, String>

    @POST(VERSION + "/ingest")
    Call<Ingest> createIngest(@Body Ingest ingest);

    @GET(VERSION + "/fixity_check")
    Call<Response<FixityCheck>> getFixityChecks(@QueryMap Map<String, String> params); // TODO: 8/3/16 Map<IngestParams, String>

    @POST(VERSION + "/fixity_check")
    Call<FixityCheck> createFixityCheck(@Body FixityCheck fixityCheck);

    @GET(VERSION + "/digest")
    Call<Response<Digest>> getDigests(@QueryMap Map<String, String> params);

}
