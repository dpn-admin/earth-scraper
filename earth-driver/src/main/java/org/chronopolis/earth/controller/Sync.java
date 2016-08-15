package org.chronopolis.earth.controller;

import org.chronopolis.earth.domain.SyncView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.sql2o.Sql2o;

/**
 * Controller for introspecting sync operations
 *
 * Created by shake on 8/4/16.
 */
@Controller
@SuppressWarnings("WeakerAccess")
public class Sync {

    Sql2o sql2o;

    @Autowired
    public Sync(Sql2o sql2o) {
        this.sql2o = sql2o;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String getSyncs(Model model) {
        model.addAttribute("syncs", SyncView.getAll(sql2o));
        return "index";
    }

    @RequestMapping(value = "/syncs/{id}", method = RequestMethod.GET)
    public String getSync(Model model, @PathVariable("id") Long id) {
        model.addAttribute("sync", SyncView.get(id, sql2o));
        return "sync";
    }

}
