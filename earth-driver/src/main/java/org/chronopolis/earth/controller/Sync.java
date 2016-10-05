package org.chronopolis.earth.controller;

import org.chronopolis.earth.domain.ReplicationFlow;
import org.chronopolis.earth.domain.SyncView;
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
 // TODO: Could have all the Session stuff in a service/dto/whateverthefuck class
 *
 * Created by shake on 8/4/16.
 */
@Controller
@SuppressWarnings("WeakerAccess")
public class Sync {
    private final Logger log = LoggerFactory.getLogger(Sync.class);

    SessionFactory factory;

    @Autowired
    public Sync(SessionFactory factory) {
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

    @RequestMapping(value = "/syncs", method = RequestMethod.GET)
    public String getSyncs(Model model) {
        List<SyncView> views;
        try (Session session = factory.openSession()) {
            views = session.createQuery("from SyncView", SyncView.class).list();
            // log.info("Found {} views to display", views.size());
        }
        model.addAttribute("syncs", views);
        return "sync/index";
    }

    @RequestMapping(value = "/syncs/{id}", method = RequestMethod.GET)
    public String getSync(Model model, @PathVariable("id") Long id) {
        SyncView view;
        try (Session session = factory.openSession()) {
            view = session.find(SyncView.class, id);
            Hibernate.initialize(view.getHttpDetails());
        }
        model.addAttribute("sync", view);
        return "sync/sync";
    }

}
