package org.chronopolis.earth.scheduled;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.chronopolis.earth.api.BagAPIs;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.EventAPIs;
import org.chronopolis.earth.api.Events;
import org.chronopolis.earth.api.LocalAPI;
import org.chronopolis.earth.api.NodeAPIs;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.domain.HttpDetail;
import org.chronopolis.earth.domain.SyncStatus;
import org.chronopolis.earth.domain.SyncType;
import org.chronopolis.earth.domain.SyncView;
import org.chronopolis.earth.models.Bag;
import org.chronopolis.earth.models.Digest;
import org.chronopolis.earth.models.FixityCheck;
import org.chronopolis.earth.models.Ingest;
import org.chronopolis.earth.models.Node;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.chronopolis.earth.util.DetailEmitter;
import org.chronopolis.earth.util.LastSync;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * Functions to synchronize registry data
 * TODO: We still have a lot of boilerplate, maybe we can cut down on it somehow
 * TODO: ZonedDateTime
 *
 * Created by shake on 3/31/15.
 */
@Component
@Profile("sync")
@EnableScheduling
@SuppressWarnings("WeakerAccess")
public class Synchronizer {

    private static final Logger log = LoggerFactory.getLogger(Synchronizer.class);

    final BagAPIs bagAPIs;
    final NodeAPIs nodeAPIs;
    final LocalAPI localAPI;
    final EventAPIs eventAPIs;
    final TransferAPIs transferAPIs;
    final DateTimeFormatter formatter;
    final SessionFactory sessionFactory;

    LastSync lastSync;
    ListeningExecutorService service;

    @Autowired
    public Synchronizer(DateTimeFormatter formatter, BagAPIs bagAPIs, TransferAPIs transferAPIs, NodeAPIs nodeAPIs, LocalAPI localAPI, EventAPIs eventAPIs, SessionFactory sessionFactory) {
        this.bagAPIs = bagAPIs;
        this.nodeAPIs = nodeAPIs;
        this.localAPI = localAPI;
        this.eventAPIs = eventAPIs;
        this.formatter = formatter;
        this.transferAPIs = transferAPIs;
        this.sessionFactory = sessionFactory;
    }

    @Scheduled(cron = "${earth.cron.sync:0 0 0 * * *}")
    public void synchronize() {
        // service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        readLastSync();
        // syncNode();
        syncBags();
        syncTransfers();
        // events
        syncIngests();
        syncFixities();
        syncDigests();
        // service.shutdown(); // shutdown the pool
        writeLastSync(lastSync);
    }

    void syncDigests() {
        List<SyncView> views = new ArrayList<>();
        BalustradeBag local = localAPI.getBagAPI();

        for (String node : eventAPIs.getApiMap().keySet()) {
            DateTime now = DateTime.now();
            String after = lastSync.lastDigestSync(node);
            Events remote = eventAPIs.getApiMap().get(node);

            Map<String, String> params = new HashMap<>();
            params.put("node", node);
            params.put("after", after);

            log.info("[{}] syncing message_digests", node);

            SyncView view = new SyncView();
            view.setHost(node);
            view.setType(SyncType.DIGEST);
            view.setStatus(SyncStatus.SUCCESS);

            PageIterable<Digest> it = new PageIterable<>(params, remote::getDigests, view);
            // Here we actually need a BiFunction for the create, so just do it in the map
            boolean failure = StreamSupport.stream(it.spliterator(), false)
                    .map(o -> o.map(d -> {
                        DetailEmitter<Digest> emitter = new DetailEmitter<>();
                        Call<Digest> create = local.createDigest(d.getBag(), d);
                        create.enqueue(emitter);
                        view.addHttpDetail(emitter.emit());
                        if (!emitter.getResponse().isPresent()) {
                            view.setStatus(SyncStatus.FAIL_LOCAL);
                        }

                        return emitter.getResponse().isPresent();
                    }))
                    .anyMatch(p -> !p.isPresent() || !p.get());

            views.add(view);
            if (!failure) {
                // log.info("Yadda yadda digest {}", node) ;
                lastSync.addLastDigest(node, now);
            } else {
                log.warn("Not updating last sync to digest for {}", node);
            }
        }

        save(views);
    }

    void syncFixities() {
        List<SyncView> views = new ArrayList<>();
        Events local = localAPI.getEventsAPI();

        for (String node : eventAPIs.getApiMap().keySet()) {
            DateTime now = DateTime.now();
            String after = lastSync.lastFixitySync(node);
            Events remote = eventAPIs.getApiMap().get(node);

            Map<String, String> params = new HashMap<>();
            params.put("node", node);
            params.put("after", after);

            log.info("[{}] syncing fixity_checks", node);

            SyncView view = new SyncView();
            view.setHost(node);
            view.setType(SyncType.FIXITY);
            view.setStatus(SyncStatus.SUCCESS);

            PageIterable<FixityCheck> it = new PageIterable<>(params, remote::getFixityChecks, view);

            // TODO: lastSync
            boolean failure = StreamSupport.stream(it.spliterator(), false)
                    .map(o -> o.map(f -> syncImmutable(local::createFixityCheck, f, view)))
                    .anyMatch(p -> !p.isPresent() || !p.get());

            views.add(view);
            if (!failure) {
                lastSync.addLastFixity(node, now); 
            } else {
                log.warn("Not updating last sync to digest for {}", node);
            }
        }

        save(views);
    }

    void syncIngests() {
        List<SyncView> views = new ArrayList<>();
        Events local = localAPI.getEventsAPI();

        for (String node : eventAPIs.getApiMap().keySet()) {
            DateTime now = DateTime.now();
            String after = lastSync.lastDigestSync(node);
            Events remote = eventAPIs.getApiMap().get(node);

            Map<String, String> params = new HashMap<>();
            params.put("node", node);
            params.put("after", after);

            log.info("[{}] syncing ingests", node);

            SyncView view = new SyncView();
            view.setHost(node);
            view.setType(SyncType.INGEST);
            view.setStatus(SyncStatus.SUCCESS);

            PageIterable<Ingest> it = new PageIterable<>(params, remote::getIngests, view);

            // TODO: lastSync
            boolean failure = StreamSupport.stream(it.spliterator(), false)
                    .map(o -> o.map(i -> syncImmutable(local::createIngest, i, view)))
                    .anyMatch(p -> !p.isPresent() || !p.get()); // not present or sync failed

            views.add(view);
            if (!failure) {
                lastSync.addLastIngest(node, now);
            } else {
                log.warn("Not updating last sync to digest for {}", node);
            }
        }

        save(views);
    }

    /**
     * Persist a group of SyncViews
     *
     * TODO: This probably isn't the best idiom to use
     *
     * @param views
     */
    void save(List<SyncView> views) {
        try (Session session = sessionFactory.openSession()) {
            views.forEach(session::persist);
        }
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
            log.debug("Writing last sync");
            sync.write();
        } catch (IOException e) {
            log.error("Unable to write last sync!", e);
        }
    }

    void syncTransfers() {
        BalustradeTransfers local = localAPI.getTransfersAPI();
        List<SyncView> views = new ArrayList<>();

        for (String node : transferAPIs.getApiMap().keySet()) {
            DateTime now = DateTime.now();
            String after = lastSync.lastReplicationSync(node);
            BalustradeTransfers remote = transferAPIs.getApiMap().get(node);

            Map<String, String> params = new HashMap<>();
            params.put("from_node", node);
            params.put("after", after);

            log.info("[{}] syncing replications", node);

            SyncView view = new SyncView();
            view.setHost(node);
            view.setType(SyncType.REPL);
            view.setStatus(SyncStatus.SUCCESS);

            PageIterable<Replication> it = new PageIterable<>(params, remote::getReplications, view);
            // We may be able to use a partially applied function here (and below), but it's not too big of a deal
            boolean failure = StreamSupport.stream(it.spliterator(), false)
                    .map(o -> o.map(r -> syncLocal(local::getReplication, local::createReplication, local::updateReplication, r, r.getReplicationId(), view)))
                    .anyMatch(p -> !p.isPresent() || !p.get()); // not present or sync failed

            views.add(view);
            if (!failure) {
                // log.info("Adding last sync to replication for {}", node);
                lastSync.addLastReplication(node, now);
            } else {
                log.warn("Not updating last sync to replication for {}", node);
            }
        }

        save(views);
    }

    void syncBags() {
        BalustradeBag local = localAPI.getBagAPI();
        Map<String, BalustradeBag> apis = bagAPIs.getApiMap();
        List<SyncView> views = new ArrayList<>();
        for (String node : apis.keySet()) {
            DateTime now = DateTime.now();
            String after = lastSync.lastBagSync(node);
            BalustradeBag remote = apis.get(node);

            Map<String, String> params = new HashMap<>();
            params.put("admin_node", node);
            params.put("after", after);

            log.info("[{}] syncing bags", node);

            SyncView view = new SyncView();
            view.setHost(node);
            view.setType(SyncType.BAG);
            view.setStatus(SyncStatus.SUCCESS);

            PageIterable<Bag> it = new PageIterable<>(params, remote::getBags, view);
            boolean failure = StreamSupport.stream(it.spliterator(), false)
                    .map(f -> f.map(b -> syncLocal(local::getBag, local::createBag, local::updateBag, b, b.getUuid(), view)))
                    .anyMatch(p -> !p.isPresent() || !p.get()); // not present or sync failed

            views.add(view);
            if (!failure) {
                // log.info("Adding last sync to bags for {}", node);
                lastSync.addLastBagSync(node, now);
            } else {
                log.warn("Not updating last sync to bags for {}", node);
            }
        }

        save(views);
    }

    // We'll probably want to split these out in to their own classes, but for now this is fine

    class PageIterable<T> implements Iterable<Optional<T>> {

        final Map<String, String> params;
        final Function<Map<String, String>, Call<? extends Response<T>>> get;
        private final SyncView view;

        public PageIterable(Map<String, String> params, Function<Map<String, String>, Call<? extends Response<T>>> get, SyncView view) {
            this.get = get;
            this.params = params;
            this.view = view;
        }

        @Override
        public Iterator<Optional<T>> iterator() {
            return new PageIterator<>(params, get, view);
        }

    }

    // TODO: Figure out what we want to do with count
    class PageIterator<T> implements Iterator<Optional<T>> {

        final int pageSize = 25;
        private final SyncView view;

        int page;
        int count;
        List<T> results;
        Map<String, String> params;
        Function<Map<String, String>, Call<? extends Response<T>>> get;

        public PageIterator(Map<String, String> params, Function<Map<String, String>, Call<? extends Response<T>>> get, SyncView view) {
            this.page = 1;
            this.get = get;
            this.view = view;
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
            // Because of the weird typing we'll handle this here for now
            // instead of in DetailEmitter
            String body = "";
            HttpDetail detail = new HttpDetail();

            // On the first run this *should* have page = 1
            // Then we increment for successive runs
            // When we fail, add a null object which serves as a "poison pill"
            // log.info("{}", params);
            Call<? extends Response<T>> apply = get.apply(params);
            if (apply.request() != null) {
                detail.setUrl(apply.request().url().toString());
                detail.setRequestMethod(apply.request().method());
            }
            try {
                retrofit2.Response<? extends Response<T>> response = apply.execute();
                detail.setResponseCode(response.code());
                if (response.isSuccessful()) {
                    count = response.body().getCount();
                    results.addAll(response.body().getResults());
                } else {
                    count = -1;
                    results.add(null);
                    body = response.errorBody().toString();
                }

                // Increment our page and update our params
                ++page;
                params.put("page", String.valueOf(page));
            } catch (IOException e) {
                log.warn("Error communicating with remote server", e);
                count = -1;
                results.add(null);
                body = e.getMessage();

                // introspection for our view
                detail.setResponseBody(body);
                view.setStatus(SyncStatus.FAIL_REMOTE);
            }

            detail.setResponseBody(body);
            view.addHttpDetail(detail);
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


    /**
     * A sync function for models which are only created and never updated
     *
     * @param create Function to create T in the registry
     * @param argT The T to sync
     * @param view The sync view we record with
     * @param <T> The type of the registry model to create
     */
    static<T> boolean syncImmutable(Function<T, Call<T>> create, T argT, SyncView view) {
        boolean success = true;
        DetailEmitter<T> emitter = new DetailEmitter<>();
        Call<T> call = create.apply(argT);
        call.enqueue(emitter);
        view.addHttpDetail(emitter.emit());
        if (!emitter.getResponse().isPresent()) {
            success = false;
            view.setStatus(SyncStatus.FAIL_LOCAL);
        }
        return success;
    }

    /**
     * Our main synchronization function. Takes functions for getting, creating, and updating a type T.
     * These should all be local functions, as it's just determining whether a create or an update call
     * needs to be run because of the way the registry works.
     *
     * @param get Function to get T from the registry
     * @param create Function to create T in the registry
     * @param update Function to update T in the registry
     * @param argT The T to sync
     * @param argU An identifier for T used in the get/update calls
     * @param <T> A type, ideally part of the registry models
     * @param <U> An identifier for T
     * @return the result of the synchronization
     */
    static<T, U> boolean syncLocal(Function<U, Call<T>> get, Function<T, Call<T>> create, BiFunction<U, T, Call<T>> update, T argT, U argU, SyncView view) {
        Call<T> sync;
        boolean success = true;
        DetailEmitter<T> getCB = new DetailEmitter<>();
        DetailEmitter<T> syncCB = new DetailEmitter<>();

        // Perform our get to see if we need to create or update
        Call<T> getCall = get.apply(argU);
        getCall.enqueue(getCB);
        Optional<T> response = getCB.getResponse();
        view.addHttpDetail(getCB.emit());
        if (response.isPresent()) {
            sync = update.apply(argU, argT);
        } else {
            sync = create.apply(argT);
        }

        // Perform our 'sync' call
        sync.enqueue(syncCB);
        response = syncCB.getResponse();
        view.addHttpDetail(syncCB.emit());
        if (!response.isPresent()) {
            log.warn("Unable to perform sync {}:{}", view.getHost(), view.getType());
            view.setStatus(SyncStatus.FAIL_LOCAL);
            success = false;
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
                if (response.isSuccessful()) {
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

}
