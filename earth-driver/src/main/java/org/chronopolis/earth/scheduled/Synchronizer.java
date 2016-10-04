package org.chronopolis.earth.scheduled;

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
import org.chronopolis.earth.domain.LastSync;
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
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrofit2.Call;

import javax.persistence.NoResultException;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
    static final String PAGE_PARAM = "page";
    static final String NODE_PARAM = "node";
    static final String FROM_PARAM = "from_node";
    static final String AFTER_PARAM = "after";
    static final String ADMIN_PARAM = "admin_node";

    final BagAPIs bagAPIs;
    final NodeAPIs nodeAPIs;
    final LocalAPI localAPI;
    final EventAPIs eventAPIs;
    final TransferAPIs transferAPIs;
    final SessionFactory sessionFactory;

    ListeningExecutorService service;

    @Autowired
    public Synchronizer(BagAPIs bagAPIs,
                        TransferAPIs transferAPIs,
                        NodeAPIs nodeAPIs,
                        LocalAPI localAPI,
                        EventAPIs eventAPIs,
                        SessionFactory sessionFactory) {
        this.bagAPIs = bagAPIs;
        this.nodeAPIs = nodeAPIs;
        this.localAPI = localAPI;
        this.eventAPIs = eventAPIs;
        this.transferAPIs = transferAPIs;
        this.sessionFactory = sessionFactory;
    }

    @Scheduled(cron = "${earth.cron.sync:0 0 0 * * *}")
    public void synchronize() {
        // leave this commented out for now, not sure if we want to sync nodes or not
        // syncNode();
        syncBags();
        syncTransfers();
        // events
        syncIngests();
        syncFixities();
        syncDigests();
    }

    void syncDigests() {
        List<SyncView> views = new ArrayList<>();
        BalustradeBag local = localAPI.getBagAPI();

        for (String node : eventAPIs.getApiMap().keySet()) {
            ZonedDateTime now = ZonedDateTime.now();
            org.chronopolis.earth.domain.LastSync last = getLastSync(node, SyncType.DIGEST);
            String after = last.getFormattedTime();
            Events remote = eventAPIs.getApiMap().get(node);

            Map<String, String> params = new HashMap<>();
            params.put(NODE_PARAM, node);
            params.put(AFTER_PARAM, after);

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
                // TODO: saveorupdate
                last.setTime(now);
            } else {
                log.warn("Not updating last sync to digest for {}", node);
            }

            saveSync(last);
        }

        save(views);
    }

    private org.chronopolis.earth.domain.LastSync getLastSync(String node, SyncType type) {
        org.chronopolis.earth.domain.LastSync last;
        try (Session session = sessionFactory.openSession()) {
            last = (org.chronopolis.earth.domain.LastSync) session.createQuery("select l from LastSync l where l.node = :node AND l.type = :type")
                    .setParameter("node", node)
                    .setParameter("type", type)
                    .getSingleResult();
        } catch (NoResultException ne) {
            // Init last here. Do we want to save it as well though?
            last = new org.chronopolis.earth.domain.LastSync();
            last.setNode(node);
            last.setType(type);
        }

        return last;
    }

    void syncFixities() {
        List<SyncView> views = new ArrayList<>();
        Events local = localAPI.getEventsAPI();

        for (String node : eventAPIs.getApiMap().keySet()) {
            ZonedDateTime now = ZonedDateTime.now();
            org.chronopolis.earth.domain.LastSync last = getLastSync(node, SyncType.FIXITY);
            String after = last.getFormattedTime();
            Events remote = eventAPIs.getApiMap().get(node);

            Map<String, String> params = new HashMap<>();
            params.put(NODE_PARAM, node);
            params.put(AFTER_PARAM, after);

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
                // TODO: saveorupdate
                last.setTime(now);
            } else {
                log.warn("Not updating last sync to digest for {}", node);
            }

            saveSync(last);
        }

        save(views);
    }

    void syncIngests() {
        List<SyncView> views = new ArrayList<>();
        Events local = localAPI.getEventsAPI();

        for (String node : eventAPIs.getApiMap().keySet()) {
            ZonedDateTime now = ZonedDateTime.now();
            org.chronopolis.earth.domain.LastSync last = getLastSync(node, SyncType.INGEST);
            String after = last.getFormattedTime();
            Events remote = eventAPIs.getApiMap().get(node);

            Map<String, String> params = new HashMap<>();
            params.put(NODE_PARAM, node);
            params.put(AFTER_PARAM, after);

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
                // TODO: saveorupdate
                last.setTime(now);
            } else {
                log.warn("Not updating last sync to digest for {}", node);
            }

            saveSync(last);
        }

        save(views);
    }

    /**
     * Persist a group of SyncViews
     *
     * TODO: This probably isn't the best idiom to use + should use batching properly
     *
     * @param views the SyncViews to save
     */
    void save(List<SyncView> views) {
        try (Session session = sessionFactory.openSession()) {
            log.info("Saving {} views", views.size());
            session.getTransaction().begin();
            views.forEach(session::persist);
            session.getTransaction().commit();
        }
    }

    void syncTransfers() {
        BalustradeTransfers local = localAPI.getTransfersAPI();
        List<SyncView> views = new ArrayList<>();

        for (String node : transferAPIs.getApiMap().keySet()) {
            ZonedDateTime now = ZonedDateTime.now();
            org.chronopolis.earth.domain.LastSync last = getLastSync(node, SyncType.REPL);
            String after = last.getFormattedTime();
            BalustradeTransfers remote = transferAPIs.getApiMap().get(node);

            Map<String, String> params = new HashMap<>();
            params.put(FROM_PARAM, node);
            params.put(AFTER_PARAM, after);

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
                // TODO: saveorupdate
                last.setTime(now);
            } else {
                log.warn("Not updating last sync to replication for {}", node);
            }

            saveSync(last);
        }

        save(views);
    }

    void syncBags() {
        BalustradeBag local = localAPI.getBagAPI();
        Map<String, BalustradeBag> apis = bagAPIs.getApiMap();
        List<SyncView> views = new ArrayList<>();
        for (String node : apis.keySet()) {
            ZonedDateTime now = ZonedDateTime.now();
            org.chronopolis.earth.domain.LastSync last = getLastSync(node, SyncType.BAG);
            String after = last.getFormattedTime();
            BalustradeBag remote = apis.get(node);

            Map<String, String> params = new HashMap<>();
            params.put(ADMIN_PARAM, node);
            params.put(AFTER_PARAM, after);

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
                // TODO: 9/28/16 saveorupdate
                last.setTime(now);
            } else {
                log.warn("Not updating last sync to bags for {}", node);
            }

            saveSync(last);
        }

        save(views);
    }

    private void saveSync(LastSync last) {
        try (Session session = sessionFactory.openSession()) {
            session.getTransaction().begin();
            session.saveOrUpdate(last);
            session.getTransaction().commit();
        }
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
            this.params.put(PAGE_PARAM, String.valueOf(page));
            populate();
        }

        @Override
        public boolean hasNext() {
            return !results.isEmpty() || (page-1) * pageSize <= count;
        }

        @Override
        public Optional<T> next() {
            if (results.isEmpty()) {
                populate();
            }

            if (!hasNext()) {
                throw new NoSuchElementException();
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
                params.put(PAGE_PARAM, String.valueOf(page));
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
        BalustradeNode local = localAPI.getNodeAPI();

        for (Map.Entry<String, BalustradeNode> entry : apis.entrySet()) {
            boolean failure = false;
            String node = entry.getKey();
            BalustradeNode remote = entry.getValue();
            log.info("[{}] syncing node", node);

            org.chronopolis.earth.domain.LastSync last = getLastSync(node, SyncType.NODE);

            SyncView view = new SyncView();
            view.setHost(node);
            view.setType(SyncType.NODE);
            view.setStatus(SyncStatus.SUCCESS);

            Call<Node> call = remote.getNode(node);
            try {
                retrofit2.Response<Node> execute = call.execute();
                Node update = execute.body();
                if (update.getUpdatedAt().isAfter(last.getTime())) {
                    Call<Node> updateCall = local.updateNode(node, update);
                    retrofit2.Response<Node> uExecute = updateCall.execute();

                    // check if we didn't succeed
                    failure = !uExecute.isSuccessful();
                }
            } catch (IOException e) {
                log.error("Error syncing node {}", node, e);
                failure = true;
            }

            if (!failure) {
                last.setTime(ZonedDateTime.now());
            } else {
                log.warn("Not updating last sync to digest for {}", node);
            }

            saveSync(last);
        }

        // save(views);
    }

}
