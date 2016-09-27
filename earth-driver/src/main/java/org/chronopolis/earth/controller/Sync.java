package org.chronopolis.earth.controller;

import org.chronopolis.earth.domain.ReplicationFlow;
import org.chronopolis.earth.domain.SyncView;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
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
        Session session = factory.openSession();
        session.beginTransaction();
        List<ReplicationFlow> replications = session.createQuery("from ReplicationFlow", ReplicationFlow.class).list();
        session.close();
        model.addAttribute("replications", replications);
        return "replicate/index";
    }

    @RequestMapping(value = "/syncs", method = RequestMethod.GET)
    public String getSyncs(Model model) {
        Session session = factory.openSession();
        session.beginTransaction();
        List<SyncView> views = session.createQuery("from SyncView", SyncView.class).list();
        session.close();
        model.addAttribute("syncs", views);
        return "sync/index";
    }

    @RequestMapping(value = "/syncs/{id}", method = RequestMethod.GET)
    public String getSync(Model model, @PathVariable("id") Long id) {
        Session session = factory.openSession();
        session.beginTransaction();
        SyncView view = session.find(SyncView.class, id);
        model.addAttribute("sync", view);
        return "sync/sync";
    }

}
