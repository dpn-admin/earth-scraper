package org.chronopolis.earth.api;

import org.chronopolis.earth.models.Node;
import org.chronopolis.earth.models.Response;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.QueryMap;

import java.util.Map;

/**
 * DPN API for operations on Nodes
 *
 * Created by shake on 3/27/15.
 */
public interface BalustradeNode {

    @GET("/api-v1/node/")
    Response<Node> getNodes(@QueryMap Map<String, Integer> params);

    @GET("/api-v1/node/{name}/")
    Node getNode(@Path("name") String name);

}
