package org.chronopolis.earth.controller;

import org.chronopolis.earth.domain.ReplicationFlow;
import org.chronopolis.earth.domain.Sync;
import org.chronopolis.earth.domain.SyncOp;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * Controller for introspecting sync operations
 *
 // TODO: Could have all the Session stuff in a service/dto class
 *
 * Created by shake on 8/4/16.
 */
@Controller
public class ViewController {
    private final Logger log = LoggerFactory.getLogger(ViewController.class);

    private final SessionFactory factory;

    @Autowired
    public ViewController(SessionFactory factory) {
        this.factory = factory;
    }

    @RequestMapping(value = "/")
    public String index() {
        return "index";
    }

    @RequestMapping(value = "/replications")
    public String getReplications(Model model) {
        List<ReplicationFlow> replications;
        try (Session session = factory.openSession()) {
            replications = session.createQuery("from ReplicationFlow", ReplicationFlow.class).list();
        }
        model.addAttribute("replications", replications);
        return "replicate/index";
    }
    @RequestMapping(value = "/replications/{id}")
    public String getReplication(Model model, @PathVariable("id") String id) {
        ReplicationFlow op;
        try (Session session = factory.openSession()) {
            op = session.find(ReplicationFlow.class, id);
            Hibernate.initialize(op.getDetails());
            Hibernate.initialize(op.getRsyncs());
        }
        model.addAttribute("op", op);
        return "replicate/replication";
    }

    @RequestMapping(value = "/syncs", method = RequestMethod.GET)
    public String getSyncs(Model model) {
        List<Sync> views;
        try (Session session = factory.openSession()) {
            // todo limit on this?
            views = session.createQuery("from Sync order by id desc", Sync.class).list();
        }
        model.addAttribute("syncs", views);
        return "sync/index";
    }

    @RequestMapping(value = "/syncs/{id}", method = RequestMethod.GET)
    public String getSync(Model model, @PathVariable("id") Long id) {
        Sync sync;
        try (Session session = factory.openSession()) {
            sync = session.find(Sync.class, id);
            Hibernate.initialize(sync.getOps());
        }
        model.addAttribute("sync", sync);
        return "sync/sync";
    }

    @RequestMapping(value = "/syncs/{syncId}/ops/{opId}", method = RequestMethod.GET)
    public String getOpDetails(Model model, @PathVariable("syncId") Long syncId, @PathVariable("opId") Long opId) {
        SyncOp op;
        Sync sync;
        try (Session session = factory.openSession()) {
            op = session.find(SyncOp.class, opId);
            sync = session.find(Sync.class, syncId);
            Hibernate.initialize(op.getDetails());
        }

        model.addAttribute("op", op);
        model.addAttribute("sync", sync);
        return "sync/op-details";
    }

}
