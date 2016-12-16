package org.chronopolis.earth.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

/**
 * Information from rsync
 *
 * Created by shake on 12/12/16.
 */
@Entity(name = "RsyncDetail")
public class RsyncDetail {

    @Id
    @GeneratedValue
    private Long id;

    private String link;

    @Lob
    private String output;

    public Long getId() {
        return id;
    }

    public RsyncDetail setId(Long id) {
        this.id = id;
        return this;
    }

    public String getLink() {
        return link;
    }

    public RsyncDetail setLink(String link) {
        this.link = link;
        return this;
    }

    public String getOutput() {
        return output;
    }

    public RsyncDetail setOutput(String requestBody) {
        this.output = requestBody;
        return this;
    }
}
