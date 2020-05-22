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
public class JSONTerm {

    @JsonbProperty("authority")
    final private String authority;
    @JsonbProperty("id")
    final private int id;
    @JsonbProperty("label")
    final private String label;

    public JSONTerm(@JsonbProperty("authority") final String authority, @JsonbProperty("id") final int id, @JsonbProperty("label") final String label) {
        this.authority = authority;
        this.id = id;
        this.label = label;
    }

    public String getAuthority() {
        return authority;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

}
