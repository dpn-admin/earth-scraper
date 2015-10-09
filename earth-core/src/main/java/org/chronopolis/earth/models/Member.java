package org.chronopolis.earth.models;

/**
 * Created by shake on 10/9/15.
 */
public class Member {

    final String uuid;
    final String name;
    final String email;

    public Member(String uuid, String name, String email) {
        this.uuid = uuid;
        this.name = name;
        this.email = email;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
