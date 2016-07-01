package org.chronopolis.earth.scheduled;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.chronopolis.earth.SimpleCallback;
import org.chronopolis.earth.api.BagAPIs;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.LocalAPI;
import org.chronopolis.earth.api.NodeAPIs;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.models.Bag;
import org.chronopolis.earth.models.Node;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.chronopolis.earth.util.LastSync;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrofit2.Call;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * TODO: Make sure we only sync items which the remote node is the admin node of
 * TODO: DateTime -> LocalDate
 * <p>
 * <p>
 * Created by shake on 3/31/15.
 */
@Component
@Profile("sync")
@EnableScheduling
public class Synchronizer {

    private static final Logger log = LoggerFactory.getLogger(Synchronizer.class);

    @Autowired
    DateTimeFormatter formatter;

    @Autowired
    BagAPIs bagAPIs;

    @Autowired
    TransferAPIs transferAPIs;

    @Autowired
    NodeAPIs nodeAPIs;

    @Autowired
    LocalAPI localAPI;

    LastSync lastSync;
    ListeningExecutorService service;

    @Scheduled(cron = "${earth.cron.sync:0 0 0 * * *}")
    public void synchronize() {
        service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        readLastSync();
        syncNode();
        syncBags();
        syncTransfers();
        service.shutdown(); // shutdown the pool
        writeLastSync(lastSync);
    }

    void readLastSync() {
        try {
            lastSync = LastSync.read();
        } catch (IOException e) {
            log.error("Unable to read last sync!", e);
            lastSync = new LastSync();
        }
    }

    private void writeLastSync(LastSync sync) {
        try {
            log.info("Writing last sync");
            sync.write();
        } catch (IOException e) {
            log.error("Unable to write last sync!", e);
        }
    }

    void syncTransfers() {
        BalustradeTransfers local = localAPI.getTransfersAPI();

        for (String node : transferAPIs.getApiMap().keySet()) {
            DateTime now = DateTime.now();
            String after = lastSync.lastReplicationSync(node);
            BalustradeTransfers remote = transferAPIs.getApiMap().get(node);

            Map<String, String> params = new HashMap<>();
            params.put("from_node", node);
            params.put("after", after);

            log.info("[{}]: Syncing replications", node);

            PageIterable<Replication> it = new PageIterable<>(params, remote::getReplications);
            boolean failure = StreamSupport.stream(it.spliterator(), false)
                    .map(f -> f.map(r -> syncLocal(local::getReplication, local::createReplication, local::updateReplication, r, r.getReplicationId())))
                    .anyMatch(p -> !p.isPresent() || !p.get()); // not present or sync failed

            if (!failure) {
                log.info("Adding last sync to replication for {}", node);
                lastSync.addLastReplication(node, now);
            } else {
                log.warn("Not updating last sync to replication for {}", node);
            }
        }
    }

    static <T> retrofit2.Response<Response<T>> http(Call<Response<T>> call) {
        retrofit2.Response<Response<T>> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            log.error("", e);
            response = retrofit2.Response.error(503, ResponseBody.create(MediaType.parse("text/plain"), "Server is not available"));
        }

        return response;
    }

    void syncBags() {
        BalustradeBag local = localAPI.getBagAPI();
        Map<String, BalustradeBag> apis = bagAPIs.getApiMap();
        for (String node : apis.keySet()) {
            DateTime now = DateTime.now();
            String after = lastSync.lastBagSync(node);
            BalustradeBag remote = apis.get(node);

            Map<String, String> params = new HashMap<>();
            params.put("admin_node", node);
            params.put("after", after);

            log.info("[{}]: Syncing bags", node);

            PageIterable<Bag> it = new PageIterable<>(params, remote::getBags);
            boolean failure = StreamSupport.stream(it.spliterator(), false)
                    .map(f -> f.map(b -> syncLocal(local::getBag, local::createBag, local::updateBag, b, b.getUuid())))
                    .anyMatch(p -> !p.isPresent() || !p.get()); // not present or sync failed

            if (!failure) {
                log.info("Adding last sync to bags for {}", node);
                lastSync.addLastBagSync(node, now);
            } else {
                log.warn("Not updating last sync to bags for {}", node);
            }
        }
    }

    // We'll probably want to split these out in to their own classes, but for now this is fine

    class PageIterable<T> implements Iterable<Optional<T>> {

        final Map<String, String> params;
        final Function<Map<String, String>, Call<Response<T>>> get;

        public PageIterable(Map<String, String> params, Function<Map<String, String>, Call<Response<T>>> get) {
            this.get = get;
            this.params = params;
        }

        @Override
        public Iterator<Optional<T>> iterator() {
            return new PageIterator<>(params, get);
        }

    }

    // TODO: Figure out what we want to do with count
    class PageIterator<T> implements Iterator<Optional<T>> {

        final int pageSize = 25;

        int page;
        int count;
        List<T> results;
        Map<String, String> params;
        Function<Map<String, String>, Call<Response<T>>> get;

        public PageIterator(Map<String, String> params, Function<Map<String, String>, Call<Response<T>>> get) {
            this.page = 1;
            this.get = get;
            this.params = params;
            this.results = new ArrayList<>();
            this.params.put("page", String.valueOf(page));
            populate();
        }

        @Override
        public boolean hasNext() {
            return results.size() > 0 || (page-1) * pageSize <= count;
        }

        @Override
        public Optional<T> next() {
            if (results.isEmpty()) {
                populate();
            }

            return Optional.ofNullable(results.remove(0));
        }

        private void populate() {
            // On the first run this *should* have page = 1
            // Then we increment for successive runs
            log.info("{}", params);
            Call<Response<T>> apply = get.apply(params);
            try {
                retrofit2.Response<Response<T>> response = apply.execute();
                if (response.isSuccess()) {
                    count = response.body().getCount();
                    results.addAll(response.body().getResults());
                } else {
                    count = -1;
                    results.add(null);
                }

                // Increment our page and update our params
                ++page;
                params.put("page", String.valueOf(page));
            } catch (IOException e) {
                log.warn("Error communicating with remote server");
                count = -1;
                results.add(null);
            }
        }

        @Override
        public void remove() {
            throw new RuntimeException("Not Supported");
        }

        @Override
        public void forEachRemaining(Consumer<? super Optional<T>> action) {
            while (hasNext()) {
                action.accept(next());
            }
        }
    }

    static<T, U> boolean syncLocal(Function<U, Call<T>> get, Function<T, Call<T>> create, BiFunction<U, T, Call<T>> update, T argT, U argU) {
        Call<T> sync;
        boolean success = true;
        SimpleCallback<T> getCB = new SimpleCallback<T>();
        Call<T> getCall = get.apply(argU);
        getCall.enqueue(getCB);
        Optional<T> response = getCB.getResponse();

        if (response.isPresent()) {
            sync = update.apply(argU, argT);
        } else {
            sync = create.apply(argT);
        }

        try {
            retrofit2.Response<T> execute = sync.execute();
            if (execute.isSuccess()) {
                log.info("Successfully ran sync");
            } else {
                success = false;
                log.warn("Unable to perform sync");
            }
        } catch (IOException e) {
            success = false;
            log.error("Error in sync call");
        }

        return success;
    }

    /**
     * Update a nodes current information based on its own record
     */
    void syncNode() {
        Map<String, BalustradeNode> apis = nodeAPIs.getApiMap();
        for (String node : apis.keySet()) {
            BalustradeNode api = apis.get(node);
            DateTime now = DateTime.now();

            ListenableFuture<Node> submit = service.submit(() -> {
                Call<Node> call = api.getNode(node);
                retrofit2.Response<Node> response = call.execute();
                if (response.isSuccess()) {
                    return response.body();
                }

                throw new Exception(response.errorBody().string());
            });

            Futures.addCallback(submit, new FutureCallback<Node>() {
                @Override
                public void onSuccess(@Nullable Node node) {
                    if (node != null) {
                        lastSync.addLastNode(node.getNamespace(), now);
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log.error("Error syncing node", throwable);
                }
            });
        }
    }

    // TODO: Might be able to do something like this to recognize
    // when calls have failed in the completable futures
    private class HeldException<T> {
        T t;
        Throwable throwable;

        public HeldException(T t) {
            this.t = t;
        }

        public boolean isSuccess() {
            return throwable == null;
        }
    }

}
