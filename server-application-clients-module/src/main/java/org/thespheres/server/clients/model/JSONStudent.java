/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.config.PropertyOrderStrategy;

/**
 *
 * @author boris.heithecker
 */
@JsonbPropertyOrder(PropertyOrderStrategy.ANY)
public class JSONStudent {

    @JsonbProperty("authority")
    final private String authority;
    @JsonbProperty("id")
    final private long id;
    @JsonbProperty("fullname")
    final private String fullname;

    public JSONStudent(@JsonbProperty("authority") final String authority, @JsonbProperty("id") final long id, @JsonbProperty("fullname") final String fullname) {
        this.authority = authority;
        this.id = id;
        this.fullname = fullname;
    }

    public String getAuthority() {
        return authority;
    }

    public long getId() {
        return id;
    }

    public String getFullname() {
        return fullname;
    }

}
