package org.chronopolis.earth;

import org.chronopolis.earth.config.Dpn;
import org.chronopolis.earth.config.Ingest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration Properties for our application
 *
 * Created by shake on 11/14/14.
 */
@Component
@ConfigurationProperties(prefix = "earth")
public class EarthSettings {

    private Boolean disableSNI;
    private Boolean logLocal;
    private Boolean logRemote;
    private Boolean logChron;
    private String stage;
    private String name;
    private Ingest ingest;
    private Dpn dpn;

    public String getStage() {
        return stage;
    }

    public EarthSettings setStage(String stage) {
        this.stage = stage;
        return this;
    }

    public Ingest getIngest() {
        return ingest;
    }

    public EarthSettings setIngest(Ingest ingest) {
        this.ingest = ingest;
        return this;
    }

    public Boolean disableSNI() {
        return disableSNI;
    }

    public EarthSettings setDisableSNI(Boolean disableSNI) {
        this.disableSNI = disableSNI;
        return this;
    }

    public Dpn getDpn() {
        return dpn;
    }

    public EarthSettings setDpn(Dpn dpn) {
        this.dpn = dpn;
        return this;
    }

    public String getName() {
        return name;
    }

    public EarthSettings setName(String name) {
        this.name = name;
        return this;
    }

    public Boolean logLocal() {
        return logLocal;
    }

    public EarthSettings setLogLocal(Boolean logLocal) {
        this.logLocal = logLocal;
        return this;
    }

    public Boolean logRemote() {
        return logRemote;
    }

    public EarthSettings setLogRemote(Boolean logRemote) {
        this.logRemote = logRemote;
        return this;
    }

    public Boolean logChron() {
        return logChron;
    }

    public EarthSettings setLogChron(Boolean logChron) {
        this.logChron = logChron;
        return this;
    }
}
