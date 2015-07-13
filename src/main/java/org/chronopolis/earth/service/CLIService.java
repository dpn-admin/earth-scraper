package org.chronopolis.earth.service;

import com.google.common.base.Optional;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

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
            } else if (option.equals(OPTION.QUIT)) {
                log.info("Quitting");
                done = true;
            }
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

        api.getNode(name, callback);
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

        api.getBags(ImmutableMap.of("admin_node", name), callback);
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

        api.getReplications(new HashMap<String, String>(), callback);
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
        BAG, TRANSFER, NODE, QUIT, UNKNOWN;

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
                default:
                    return UNKNOWN;
            }
        }
    }

}
