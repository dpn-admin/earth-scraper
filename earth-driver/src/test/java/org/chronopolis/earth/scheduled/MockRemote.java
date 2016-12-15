package org.chronopolis.earth.scheduled;

import com.google.gson.Gson;
import org.chronopolis.earth.api.BalustradeBag;
import org.chronopolis.earth.api.BalustradeNode;
import org.chronopolis.earth.api.BalustradeTransfers;
import org.chronopolis.earth.api.Events;
import org.chronopolis.earth.api.Remote;
import org.chronopolis.earth.config.Endpoint;

/**
 *
 * Created by shake on 12/15/16.
 */
public class MockRemote extends Remote {

    private BalustradeBag bags;
    private BalustradeNode nodes;
    private BalustradeTransfers transfers;
    private Events events;

    public MockRemote(Endpoint endpoint, Gson gson) {
        super(endpoint, gson);
    }

    @Override
    public BalustradeBag getBags() {
        return bags;
    }

    public MockRemote setBags(BalustradeBag bags) {
        this.bags = bags;
        return this;
    }

    @Override
    public BalustradeNode getNodes() {
        return nodes;
    }

    public MockRemote setNodes(BalustradeNode nodes) {
        this.nodes = nodes;
        return this;
    }

    @Override
    public BalustradeTransfers getTransfers() {
        return transfers;
    }

    public MockRemote setTransfers(BalustradeTransfers transfers) {
        this.transfers = transfers;
        return this;
    }

    @Override
    public Events getEvents() {
        return events;
    }

    public MockRemote setEvents(Events events) {
        this.events = events;
        return this;
    }
}
