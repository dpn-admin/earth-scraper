package org.chronopolis.earth.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Holder for configuration properties of HikariCP
 *
 * Created by shake on 12/9/16.
 */
public class Hikari {

    private final String PROPERTY_URL = "hibernate.hikari.dataSource.url";
    private final String PROPERTY_USER = "hibernate.hikari.dataSource.user";
    private final String PROPERTY_PASS = "hibernate.hikari.dataSource.password";

    /**
     * JDBC URL of the database
     */
    private String url = "jdbc:h2:./db/intake";

    /**
     * Username to connect to the db with
     */
    private String username;

    /**
     * Password to connect to the db with
     */
    private String password;

    public String getUrl() {
        return url;
    }

    public Hikari setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public Hikari setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public Hikari setPassword(String password) {
        this.password = password;
        return this;
    }

    public Map<String, String> asMap() {
        HashMap<String, String> map = new HashMap<>();
        map.put(PROPERTY_URL, url);
        if (username != null) {
            map.put(PROPERTY_USER, username);
        }
        if (password != null) {
            map.put(PROPERTY_PASS, password);
        }

        return map;
    }
}
