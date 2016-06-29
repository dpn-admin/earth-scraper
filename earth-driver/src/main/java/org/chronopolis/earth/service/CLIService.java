package org.chronopolis.earth.service;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.earth.SimpleCallback;
import org.chronopolis.earth.api.BagAPIs;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.NodeAPIs;
import org.chronopolis.earth.api.TransferAPIs;
import org.chronopolis.earth.models.Bag;
import org.chronopolis.earth.models.Node;
import org.chronopolis.earth.models.Replication;
import org.chronopolis.earth.models.Response;
import org.chronopolis.earth.scheduled.Downloader;
import org.chronopolis.earth.scheduled.Synchronizer;
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
import java.util.Map;
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

    @Autowired
    TransferAPIs transferAPIs;

    @Autowired
    BagAPIs bagAPIs;

    @Autowired
    NodeAPIs nodeAPIs;

    @Autowired
    ApplicationContext context;

    @Override
    public void replicate() {
        boolean done = false;
        System.out.println("Enter 'q' to quit");
        while (!done) {
            OPTION option = inputOption();
            if (option.equals(OPTION.BAG)) {
                for (Map.Entry<String, BalustradeBag> entry: bagAPIs.getApiMap().entrySet()) {
                    consumeBag(entry.getKey(), entry.getValue());
                }
            } else if (option.equals(OPTION.TRANSFER)) {
                for (Map.Entry<String, BalustradeTransfers> entry: transferAPIs.getApiMap().entrySet()) {
                    consumeTransfer(entry);
                }
            } else if (option.equals(OPTION.NODE)) {
                for (Map.Entry<String, BalustradeNode> entry : nodeAPIs.getApiMap().entrySet()) {
                    consumeNode(entry.getKey(), entry.getValue());
                }
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
            System.out.println("Unable to create downloader");
            return;
        }

        System.out.println("DPN Node: ");
        String node = readLine();
        System.out.println("Replication UUID: ");
        String uuid = readLine();

        BalustradeTransfers api = transferAPIs.getApiMap().get(node);
        Call<Replication> replicationCall = api.getReplication(uuid);
        try {
            retrofit2.Response<Replication> response = replicationCall.execute();
            replication = response.body();
        } catch (IOException e) {
            System.out.println("Unable to get replication from " + node + " with uuid " + uuid);
            return;
        }

        try {
            dl.download(api, replication);
            System.out.println("Downloaded. Waiting on input to continue.");
            readLine();
            dl.update(api, replication);
            System.out.println("Updated. Waiting on input to continue.");
            readLine();
            dl.untar(replication);
            dl.validate(api, replication);
            System.out.println("Validated. Waiting on input to continue.");
            readLine();
            dl.push(replication);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
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
        SimpleCallback<Response<Bag>> callback = new SimpleCallback<>();

        Call<Response<Bag>> call = api.getBags(ImmutableMap.of("admin_node", name));
        call.enqueue(callback);
        Optional<Response<Bag>> response = callback.getResponse();

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
     * @param entry
     */
    private void consumeTransfer(Map.Entry<String, BalustradeTransfers> entry) {
        log.info("{}", entry.getKey());
        BalustradeTransfers api = entry.getValue();
        SimpleCallback<Response<Replication>> callback = new SimpleCallback<>();

        Call<Response<Replication>> call = api.getReplications(new HashMap<String, String>());
        call.enqueue(callback);
        Optional<Response<Replication>> response = callback.getResponse();

        if (response.isPresent()) {
            Response<Replication> replications = response.get();
            log.info("Showing {} out of {} total", replications.getResults().size(), replications.getCount());
            for (Replication replication : replications.getResults()) {
                log.info("[{}] {}", replication.status(), replication.getReplicationId());
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
            throw new RuntimeException("Unable to read STDIN");
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
