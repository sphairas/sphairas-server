/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.config.PropertyOrderStrategy;

/**
 *
 * @author boris.heithecker
 */
@JsonbPropertyOrder(PropertyOrderStrategy.ANY)
public class Grade {

    protected final String id;
    protected String label;
    private String longLabel;
    private String icon;
    private String[] styles;

    @JsonbCreator
    public Grade(@JsonbProperty("id") final String id) {
        this.id = id;
    }

    public Grade(final org.thespheres.betula.assess.Grade orig) {
        this(orig.getId());
        setLabel(orig.getShortLabel());
        setLongLabel(orig.getLongLabel());
    }

    @JsonbProperty(value = "id")
    public String getId() {
        return id;
    }

    @JsonbProperty(value = "label")
    public String getLabel() {
        return label;
    }

    @JsonbProperty(value = "label")
    public void setLabel(final String label) {
        this.label = label;
    }

    @JsonbProperty("long-label")
    public String getLongLabel() {
        return longLabel;
    }

    @JsonbProperty("long-label")
    public void setLongLabel(final String longLabel) {
        this.longLabel = longLabel;
    }

    @JsonbProperty("icon")
    public String getIcon() {
        return icon;
    }

    @JsonbProperty("icon")
    public void setIcon(String icon) {
        this.icon = icon;
    }

    @JsonbProperty(value = "styles")
    public String[] getStyles() {
        return styles;
    }

    @JsonbProperty(value = "styles")
    public void setStyles(final String[] styles) {
        this.styles = styles;
    }

}
