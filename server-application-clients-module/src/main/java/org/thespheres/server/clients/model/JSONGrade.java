/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import java.util.List;
import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.config.PropertyOrderStrategy;
import org.thespheres.server.clients.model.Property.PropertyList;

/**
 *
 * @author boris.heithecker
 */
@JsonbPropertyOrder(PropertyOrderStrategy.ANY)
public class JSONGrade {

    @JsonbProperty(value = "id")
    protected final String id;
    @JsonbProperty(value = "label")
    protected String label;
    @JsonbProperty("long-label")
    private String longLabel;
    @JsonbProperty(value = "styles")
    private PropertyList styles;

    @JsonbCreator
    public JSONGrade(@JsonbProperty("id") final String id) {
        this.id = id;
    }

    public JSONGrade(final String id, final String label) {
        this(id);
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getLongLabel() {
        return longLabel;
    }

    public void setLongLabel(String longLabel) {
        this.longLabel = longLabel;
    }

    public List<Property> getStyles() {
        return styles;
    }

    public void addStyle(final String name, final String value) {
        final Property add = new Property(name, value);
        if (styles == null) {
            styles = new PropertyList();
        }
        styles.add(add);
    }

}
