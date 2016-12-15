package org.chronopolis.earth.api;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import org.chronopolis.earth.OkTokenInterceptor;
import org.chronopolis.earth.config.Endpoint;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 *
 * Created by shake on 12/14/16.
 */
public class Remote {

    private final Endpoint endpoint;
    private final Gson gson;

    public Remote(Endpoint endpoint, Gson gson) {
        this.endpoint = endpoint;
        this.gson = gson;
    }

    public BalustradeBag getBags() {
        return adapter().create(BalustradeBag.class);
    }

    public BalustradeMember getMembers() {
        return adapter().create(BalustradeMember.class);
    }

    public BalustradeTransfers getTransfers() {
        return adapter().create(BalustradeTransfers.class);
    }

    public BalustradeNode getNodes() {
        return adapter().create(BalustradeNode.class);
    }

    public Events getEvents() {
        return adapter().create(Events.class);
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    private Retrofit adapter() {
        return new Retrofit.Builder()
                .baseUrl(endpoint.getApiRoot())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client())
                .build();
    }

    private OkHttpClient client() {
        return new OkHttpClient.Builder()
                .addInterceptor(new OkTokenInterceptor(endpoint.getAuthKey()))
                .build();
    }

}
