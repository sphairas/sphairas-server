/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;

/**
 *
 * @author boris.heithecker@gmx.net
 */
public class Icon {

    private final String name;
    private String icon;

    @JsonbCreator
    public Icon(@JsonbProperty("name") final String name) {
        this.name = name;
    }

    public Icon(final String name, final String icon) {
        this(name);
        this.icon = icon;
    }

    @JsonbProperty("name")
    public String getName() {
        return name;
    }

    @JsonbProperty("icon")
    public String getIcon() {
        return icon;
    }

    @JsonbProperty("icon")
    public void setIcon(String icon) {
        this.icon = icon;
    }

}
