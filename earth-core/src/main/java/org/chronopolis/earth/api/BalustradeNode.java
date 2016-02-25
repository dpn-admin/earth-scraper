package org.chronopolis.earth.api;

import org.chronopolis.earth.models.Node;
import org.chronopolis.earth.models.Response;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.Map;

/**
 * DPN API for operations on Nodes
 *
 * Created by shake on 3/27/15.
 */
public interface BalustradeNode {

    @GET("api-v1/node/")
    Call<Response<Node>> getNodes(@QueryMap Map<String, Integer> params);

    @GET("api-v1/node/{name}/")
    Call<Node> getNode(@Path("name") String name);

}
