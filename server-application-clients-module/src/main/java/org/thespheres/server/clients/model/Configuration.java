/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import java.util.ArrayList;
import java.util.List;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.config.PropertyOrderStrategy;
import org.thespheres.server.clients.model.Property.PropertyList;

/**
 *
 * @author boris.heithecker
 */
@JsonbPropertyOrder(PropertyOrderStrategy.ANY)
public class Configuration {

    @JsonbProperty("properties")
    private final Property.PropertyList properties = new PropertyList();
    @JsonbProperty("conventions")
    private final List<JSONConvention> conventions = new ArrayList<>();

    public List<Property> getProperties() {
        return properties;
    }

    public List<JSONConvention> getConventions() {
        return conventions;
    }

}
