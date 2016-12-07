package org.chronopolis.earth.scheduled;

import com.google.common.annotations.VisibleForTesting;
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
import org.chronopolis.earth.domain.Sync;
import org.chronopolis.earth.domain.SyncOp;
import org.chronopolis.earth.domain.SyncStatus;
import org.chronopolis.earth.domain.SyncType;
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
 * <p>
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

    private final BagAPIs bagAPIs;
    private final NodeAPIs nodeAPIs;
    private final LocalAPI localAPI;
    private final EventAPIs eventAPIs;
    private final TransferAPIs transferAPIs;
    private final SessionFactory sessionFactory;

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
        for (Map.Entry<String, BalustradeBag> entry : bagAPIs.getApiMap().entrySet()) {
            String node = entry.getKey();
            Sync sync = new Sync().setHost(node);
            BalustradeBag bags = entry.getValue();
            BalustradeNode nodes = nodeAPIs.getApiMap().get(node);
            BalustradeTransfers transfers = transferAPIs.getApiMap().get(node);
            Events events = eventAPIs.getApiMap().get(node);

            syncDigests(events, node, sync);
            syncBags(bags, node, sync);
            syncTransfers(transfers, node, sync);
            syncIngests(events, node, sync);
            syncFixities(events, node, sync);
            syncNode(nodes, node, sync);

            sync.updateStatus();
            save(sync);
        }

        // syncNode();
        // syncBags();
        // syncTransfers();
        // events
        // syncIngests();
        // syncFixities();
        // syncDigests();
    }


    @VisibleForTesting
    public void syncFixities(Events remote, String node, Sync sync) {
        Events local = localAPI.getEventsAPI();
        ZonedDateTime now = ZonedDateTime.now();
        LastSync last = getLastSync(node, SyncType.FIXITY);
        String after = last.getFormattedTime();

        Map<String, String> params = new HashMap<>();
        params.put(NODE_PARAM, node);
        params.put(AFTER_PARAM, after);

        log.info("[{}] syncing fixity_checks", node);

        SyncOp op = new SyncOp().setParent(sync)
                .setType(SyncType.FIXITY)
                .setStatus(SyncStatus.SUCCESS);

        PageIterable<FixityCheck> it = new PageIterable<>(params, remote::getFixityChecks, op);

        boolean failure = StreamSupport.stream(it.spliterator(), false)
                .map(o -> o.map(f -> syncImmutable(local::createFixityCheck, f, op)))
                .anyMatch(p -> !p.isPresent() || !p.get());

        sync.addOp(op);
        if (!failure) {
            last.setTime(now);
        } else {
            log.warn("Not updating last sync to digest for {}", node);
        }

        saveSync(last);
    }

    public void syncIngests(Events remote, String node, Sync sync) {
        Events local = localAPI.getEventsAPI();
        ZonedDateTime now = ZonedDateTime.now();
        LastSync last = getLastSync(node, SyncType.INGEST);
        String after = last.getFormattedTime();

        Map<String, String> params = new HashMap<>();
        params.put(NODE_PARAM, node);
        params.put(AFTER_PARAM, after);

        log.info("[{}] syncing ingests", node);

        SyncOp op = new SyncOp().setParent(sync)
                .setType(SyncType.INGEST)
                .setStatus(SyncStatus.SUCCESS);

        PageIterable<Ingest> it = new PageIterable<>(params, remote::getIngests, op);

        // TODO: lastSync
        boolean failure = StreamSupport.stream(it.spliterator(), false)
                .map(o -> o.map(i -> syncImmutable(local::createIngest, i, op)))
                .anyMatch(p -> !p.isPresent() || !p.get()); // not present or sync failed

        sync.addOp(op);
        if (!failure) {
            last.setTime(now);
        } else {
            log.warn("Not updating last sync to digest for {}", node);
        }

        saveSync(last);
    }

    public void syncTransfers(BalustradeTransfers remote, String node, Sync sync) {
        BalustradeTransfers local = localAPI.getTransfersAPI();
        ZonedDateTime now = ZonedDateTime.now();
        LastSync last = getLastSync(node, SyncType.REPL);
        String after = last.getFormattedTime();

        Map<String, String> params = new HashMap<>();
        params.put(FROM_PARAM, node);
        params.put(AFTER_PARAM, after);

        log.info("[{}] syncing replications", node);

        SyncOp op = new SyncOp().setParent(sync)
                .setType(SyncType.REPL)
                .setStatus(SyncStatus.SUCCESS);

        PageIterable<Replication> it = new PageIterable<>(params, remote::getReplications, op);
        // We may be able to use a partially applied function here (and below), but it's not too big of a deal
        boolean failure = StreamSupport.stream(it.spliterator(), false)
                .map(o -> o.map(r -> syncLocal(local::getReplication, local::createReplication, local::updateReplication, r, r.getReplicationId(), op)))
                .anyMatch(p -> !p.isPresent() || !p.get()); // not present or sync failed

        sync.addOp(op);
        if (!failure) {
            last.setTime(now);
        } else {
            log.warn("Not updating last sync to replication for {}", node);
        }

        saveSync(last);
    }

    public void syncBags(BalustradeBag remote, String node, Sync sync) {
        BalustradeBag local = localAPI.getBagAPI();
        ZonedDateTime now = ZonedDateTime.now();
        LastSync last = getLastSync(node, SyncType.BAG);
        String after = last.getFormattedTime();

        Map<String, String> params = new HashMap<>();
        params.put(ADMIN_PARAM, node);
        params.put(AFTER_PARAM, after);

        log.info("[{}] syncing bags", node);

        SyncOp op = new SyncOp().setParent(sync)
                .setType(SyncType.BAG)
                .setStatus(SyncStatus.SUCCESS);

        PageIterable<Bag> it = new PageIterable<>(params, remote::getBags, op);
        boolean failure = StreamSupport.stream(it.spliterator(), false)
                .map(f -> f.map(b -> syncLocal(local::getBag, local::createBag, local::updateBag, b, b.getUuid(), op)))
                .anyMatch(p -> !p.isPresent() || !p.get()); // not present or sync failed

        sync.addOp(op);
        if (!failure) {
            last.setTime(now);
        } else {
            log.warn("Not updating last sync to bags for {}", node);
        }

        saveSync(last);
    }

    public void syncNode(BalustradeNode remote, String node, Sync sync) {
        BalustradeNode local = localAPI.getNodeAPI();
        boolean failure = false;
        log.info("[{}] syncing node", node);

        LastSync last = getLastSync(node, SyncType.NODE);

        SyncOp op = new SyncOp().setParent(sync)
                .setType(SyncType.NODE)
                .setStatus(SyncStatus.SUCCESS);

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

    public void syncDigests(Events remote, String node, Sync sync) {
        BalustradeBag local = localAPI.getBagAPI();
        ZonedDateTime now = ZonedDateTime.now();

        LastSync last = getLastSync(node, SyncType.DIGEST);
        String after = last.getFormattedTime();

        Map<String, String> params = new HashMap<>();
        params.put(NODE_PARAM, node);
        params.put(AFTER_PARAM, after);

        log.info("[{}] syncing message_digests", node);

        SyncOp op = new SyncOp().setParent(sync)
                .setType(SyncType.DIGEST)
                .setStatus(SyncStatus.SUCCESS);

        PageIterable<Digest> it = new PageIterable<>(params, remote::getDigests, op);
        // Here we actually need a BiFunction for the create, so just do it in the map
        boolean failure = StreamSupport.stream(it.spliterator(), false)
                .map(o -> o.map(d -> {
                    DetailEmitter<Digest> emitter = new DetailEmitter<>();
                    Call<Digest> create = local.createDigest(d.getBag(), d);
                    create.enqueue(emitter);
                    op.addDetail(emitter.emit());
                    if (!emitter.getResponse().isPresent()) {
                        op.setStatus(SyncStatus.FAIL_LOCAL);
                    }

                    return emitter.getResponse().isPresent();
                }))
                .anyMatch(p -> !p.isPresent() || !p.get());

        sync.addOp(op);
        if (!failure) {
            // TODO: saveorupdate
            last.setTime(now);
        } else {
            log.warn("Not updating last sync to digest for {}", node);
        }

        saveSync(last);
    }

    // Helper fns

    /**
     * Helper function to get the last sync for a certain operation
     * or create it if it does not exist
     *
     * @param node The node we're syncing from
     * @param type The type of sync we're doing
     * @return the last sync of the node + type
     */
    private LastSync getLastSync(String node, SyncType type) {
        LastSync last;
        try (Session session = sessionFactory.openSession()) {
            last = (LastSync) session.createQuery("select l from LastSync l where l.node = :node AND l.type = :type")
                    .setParameter("node", node)
                    .setParameter("type", type)
                    .getSingleResult();
        } catch (NoResultException ne) {
            // Init last here. Do we want to save it as well though?
            last = new LastSync();
            last.setNode(node);
            last.setType(type);
        }

        return last;
    }

    /**
     * Persist a single sync object
     *
     * @param sync the sync to persist
     */
    private void save(Sync sync) {
        try (Session session = sessionFactory.openSession()) {
            session.getTransaction().begin();
            session.persist(sync);
            session.getTransaction().commit();
        }
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

        private final Map<String, String> params;
        private final Function<Map<String, String>, Call<? extends Response<T>>> get;
        private final SyncOp view;

        public PageIterable(Map<String, String> params, Function<Map<String, String>, Call<? extends Response<T>>> get, SyncOp view) {
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

        private final int pageSize = 25;
        private final SyncOp op;

        private int page;
        private int count;
        private List<T> results;
        private Map<String, String> params;
        private Function<Map<String, String>, Call<? extends Response<T>>> get;

        public PageIterator(Map<String, String> params, Function<Map<String, String>, Call<? extends Response<T>>> get, SyncOp op) {
            this.page = 1;
            this.get = get;
            this.op = op;
            this.params = params;
            this.results = new ArrayList<>();
            this.params.put(PAGE_PARAM, String.valueOf(page));
            populate();
        }

        @Override
        public boolean hasNext() {
            return !results.isEmpty() || (page - 1) * pageSize <= count;
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

                // introspection for our op
                detail.setResponseBody(body);
                op.setStatus(SyncStatus.FAIL_REMOTE);
            }

            detail.setResponseBody(body);
            op.addDetail(detail);
        }

        @Override
        public void forEachRemaining(Consumer<? super Optional<T>> action) {
            while (hasNext()) {
                action.accept(next());
            }
        }
    }

    // Static helper functions

    /**
     * A sync function for models which are only created and never updated
     *
     * @param create Function to create T in the registry
     * @param argT   The T to sync
     * @param op     The sync op we record with
     * @param <T>    The type of the registry model to create
     */
    private static <T> boolean syncImmutable(Function<T, Call<T>> create, T argT, SyncOp op) {
        boolean success = true;
        DetailEmitter<T> emitter = new DetailEmitter<>();
        Call<T> call = create.apply(argT);
        call.enqueue(emitter);
        op.addDetail(emitter.emit());
        if (!emitter.getResponse().isPresent()) {
            success = false;
            op.setStatus(SyncStatus.FAIL_LOCAL);
        }
        return success;
    }

    /**
     * Our main synchronization function. Takes functions for getting, creating, and updating a type T.
     * These should all be local functions, as it's just determining whether a create or an update call
     * needs to be run because of the way the registry works.
     *
     * @param get    Function to get T from the registry
     * @param create Function to create T in the registry
     * @param update Function to update T in the registry
     * @param argT   The T to sync
     * @param argU   An identifier for T used in the get/update calls
     * @param <T>    A type, ideally part of the registry models
     * @param <U>    An identifier for T
     * @return the result of the synchronization
     */
    private static <T, U> boolean syncLocal(Function<U, Call<T>> get, Function<T, Call<T>> create, BiFunction<U, T, Call<T>> update, T argT, U argU, SyncOp op) {
        Call<T> sync;
        boolean success = true;
        DetailEmitter<T> getCB = new DetailEmitter<>();
        DetailEmitter<T> syncCB = new DetailEmitter<>();

        // Perform our get to see if we need to create or update
        Call<T> getCall = get.apply(argU);
        getCall.enqueue(getCB);
        Optional<T> response = getCB.getResponse();
        op.addDetail(getCB.emit());
        if (response.isPresent()) {
            sync = update.apply(argU, argT);
        } else {
            sync = create.apply(argT);
        }

        // Perform our 'sync' call
        sync.enqueue(syncCB);
        response = syncCB.getResponse();
        op.addDetail(syncCB.emit());
        if (!response.isPresent()) {
            log.warn("Unable to perform sync {}:{}", op.getParent().getHost(), op.getType());
            op.setStatus(SyncStatus.FAIL_LOCAL);
            success = false;
        }

        return success;
    }


}
