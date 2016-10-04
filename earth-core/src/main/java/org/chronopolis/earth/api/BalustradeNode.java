package org.chronopolis.earth.api;

import org.chronopolis.earth.models.Node;
import org.chronopolis.earth.models.Response;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.Map;

import static org.chronopolis.earth.api.Version.VERSION;

/**
 * DPN API for operations on Nodes
 *
 * Created by shake on 3/27/15.
 */
public interface BalustradeNode {

    @GET(VERSION + "/node/")
    Call<Response<Node>> getNodes(@QueryMap Map<String, Integer> params);

    @GET(VERSION + "/node/{name}/")
    Call<Node> getNode(@Path("name") String name);

    @PUT(VERSION + "/node/{name}")
    Call<Node> updateNode(@Path("name") String name, @Body Node update);

}
