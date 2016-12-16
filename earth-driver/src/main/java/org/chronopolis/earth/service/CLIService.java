package org.chronopolis.earth.service;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.earth.SimpleCallback;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.Remote;
import org.chronopolis.earth.domain.ReplicationFlow;
import org.chronopolis.earth.models.Bag;
import org.chronopolis.earth.models.Node;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.chronopolis.earth.models.SumResponse;
import org.chronopolis.earth.scheduled.Downloader;
import org.chronopolis.earth.scheduled.Synchronizer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import retrofit2.Call;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Service for when we are run from the command line
 * TODO: Allow iteration for pages
 *     : -Continue
 *     : -Stop
 * TODO: We might share some of the code in the consume methods with the scheduled based
 *       classes. If we do, find a common place for them.
 *
 * Created by shake on 7/7/15.
 */
@Component
@Profile("cli")
public class CLIService implements DpnService {
    private final Logger log = LoggerFactory.getLogger(CLIService.class);

    private final List<Remote> remotes;
    private final SessionFactory factory;
    private final ApplicationContext context;

    @Autowired
    public CLIService(List<Remote> remotes, ApplicationContext context, SessionFactory factory) {
        this.remotes = remotes;
        this.context = context;
        this.factory = factory;
    }

    @Override
    public void replicate() {
        boolean done = false;
        System.out.println("Enter 'q' to quit");
        while (!done) {
            OPTION option = inputOption();
            if (option.equals(OPTION.BAG)) {
                remotes.forEach(r -> consumeBag(r.getEndpoint().getName(), r.getBags()));
            } else if (option.equals(OPTION.TRANSFER)) {
                remotes.forEach(r -> consumeTransfer(r.getEndpoint().getName(), r.getTransfers()));
            } else if (option.equals(OPTION.NODE)) {
                remotes.forEach(r -> consumeNode(r.getEndpoint().getName(), r.getNodes()));
            } else if (option.equals(OPTION.SYNC)) {
                sync();
            } else if (option.equals(OPTION.QUIT)) {
                log.info("Quitting");
                done = true;
            } else if (option.equals(OPTION.REPLICATE)) {
                doReplication();
            }
        }
    }

    private void doReplication() {
        Downloader dl;
        Replication replication;
        try {
            dl = context.getBean(Downloader.class);
        } catch (Exception e) {
            log.error("", e);
            System.out.println("Unable to create downloader");
            return;
        }

        System.out.println("DPN Node: ");
        String node = readLine();
        System.out.println("Replication UUID: ");
        String uuid = readLine();

        remotes.stream()
                .filter(r -> r.getEndpoint().getName().equals(node))
                // There should only be one... hopefully...
                .forEach(r -> replicate(dl, r, uuid));
    }

    /**
     * Helper for the above so we can have it cleanly consume
     *
     * @param dl The downloader to use
     * @param r The remote to query
     * @param uuid The replication id to use
     */
    private void replicate(Downloader dl, Remote r, String uuid) {
        String node = r.getEndpoint().getName();
        BalustradeTransfers api = r.getTransfers();
        Call<Replication> replicationCall = api.getReplication(uuid);
        Replication replication;
        try {
            retrofit2.Response<Replication> response = replicationCall.execute();
            replication = response.body();
        } catch (IOException e) {
            log.error("", e);
            System.out.println("Unable to get replication from " + node + " with uuid " + uuid);
            return;
        }

        try {
            Session session = factory.openSession();
            ReplicationFlow flow = getRF(session, replication);
            session.close();
            dl.download(replication, flow);
            System.out.println("Downloaded. Waiting on input to continue.");
            readLine();
            dl.update(api, replication, flow);
            System.out.println("Updated. Waiting on input to continue.");
            readLine();
            dl.untar(replication, flow);
            dl.validate(api, replication, flow);
            System.out.println("Validated. Waiting on input to continue.");
            readLine();
            dl.push(replication, flow);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    private ReplicationFlow getRF(Session session, Replication transfer) {
        ReplicationFlow flow = session.get(ReplicationFlow.class, transfer.getReplicationId());
        if (flow == null) {
            flow = new ReplicationFlow();
            flow.setId(transfer.getReplicationId());
            flow.setNode(transfer.getFromNode());
        }

        return flow;
    }


    /**
     * Attempt to sync; Display text saying we can't if the profile is not loaded
     *
     */
    private void sync() {
        Synchronizer bean;
        try {
            bean = context.getBean(Synchronizer.class);
            bean.synchronize();
        } catch (Exception e) {
            log.error("", e);
            System.out.println("Synchronizer not found, please add 'sync' to your spring.profiles");
        }
    }

    /**
     * Consumer for node apis
     *
     * @param name
     * @param api
     */
    private void consumeNode(String name, BalustradeNode api) {
        log.info("Current Admin node: {}", name);
        SimpleCallback<Node> callback = new SimpleCallback<>();

        Call<Node> call = api.getNode(name);
        call.enqueue(callback);
        Optional<Node> response = callback.getResponse();

        // wtb java 8 flat map ;_;
        if (response.isPresent()) {
            Node node = response.get();
            log.info("[{}] {} {}", node.getName(), node.getApiRoot(), node.getSshPubkey());
        }
    }

    /**
     * Consumer for bag apis
     *
     * @param name
     * @param api
     */
    private void consumeBag(String name, BalustradeBag api) {
        log.info("Current Admin node: {}", name);
        SimpleCallback<SumResponse<Bag>> callback = new SimpleCallback<>();

        Call<SumResponse<Bag>> call = api.getBags(ImmutableMap.of("admin_node", name));
        call.enqueue(callback);
        Optional<SumResponse<Bag>> response = callback.getResponse();

        if (response.isPresent()) {
            Response<Bag> bags = response.get();
            log.info("Showing {} out of {} total bags", bags.getResults().size(), bags.getCount());
            for (Bag bag : bags.getResults()) {
                log.info("[{}] {}", bag.getAdminNode(), bag.getUuid());
            }
        }
    }

    /**
     * Consumer for transfer apis
     *
     * @param node The node we're querying
     * @param api The transfer api we're querying on
     */
    private void consumeTransfer(String node, BalustradeTransfers api) {
        SimpleCallback<Response<Replication>> callback = new SimpleCallback<>();

        Call<Response<Replication>> call = api.getReplications(new HashMap<>());
        call.enqueue(callback);
        Optional<Response<Replication>> response = callback.getResponse();

        if (response.isPresent()) {
            Response<Replication> replications = response.get();
            log.info("Showing {} out of {} total", replications.getResults().size(), replications.getCount());
            for (Replication replication : replications.getResults()) {
                log.info("{}: storeRequested={}, stored={}, cancelled={}", replication.getReplicationId(),
                        replication.isStoreRequested(),
                        replication.isStored(),
                        replication.isCancelled());
            }
        }
    }

    /**
     * Create a prompt and read the input for the next option
     *
     * @return the input given
     */
    private OPTION inputOption() {
        OPTION option = OPTION.UNKNOWN;
        while (option.equals(OPTION.UNKNOWN)) {
            StringBuilder sb = new StringBuilder("Query Type: ");
            String sep = " | ";
            for (OPTION value : OPTION.values()) {
                if (!value.equals(OPTION.UNKNOWN)) {
                    sb.append(value.name());
                    sb.append(" [");
                    sb.append(value.name().charAt(0));
                    sb.append("]");
                    sb.append(sep);
                }
            }

            sb.replace(sb.length() - sep.length(), sb.length(), " -> ");
            System.out.println(sb.toString());
            option = OPTION.fromString(readLine().trim());
        }
        return option;
    }

    private String readLine() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException ex) {
            log.error("Unable to read from stdin", ex);
            throw new CLIException("Unable to read STDIN");
        }
    }

    class CLIException extends RuntimeException {
        CLIException(String s) {
            super(s);
        }
    }

    private enum OPTION {
        BAG, TRANSFER, NODE, QUIT, SYNC, REPLICATE, UNKNOWN;

        private static OPTION fromString(String text) {
            switch (text) {
                case "B":
                case "b":
                    return BAG;
                case "T":
                case "t":
                    return TRANSFER;
                case "N":
                case "n":
                    return NODE;
                case "Q":
                case "q":
                    return QUIT;
                case "S":
                case "s":
                    return SYNC;
                case "R":
                case "r":
                    return REPLICATE;
                default:
                    return UNKNOWN;
            }
        }
    }

}
