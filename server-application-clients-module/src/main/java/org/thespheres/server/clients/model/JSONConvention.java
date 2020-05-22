/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.config.PropertyOrderStrategy;

/**
 *
 * @author boris.heithecker
 */
@JsonbPropertyOrder(PropertyOrderStrategy.ANY)
public class JSONConvention {

    @JsonbProperty("name")
    private String name;
    @JsonbProperty("grades")
    private final List<JSONGrade> values = new ArrayList<>();

    public JSONConvention() {
    }

    public JSONConvention(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<JSONGrade> getGrades() {
        return Collections.unmodifiableList(values);
    }

    public JSONGrade addGrade(final String id, final String label) {
        final JSONGrade ret = new JSONGrade(id, label);
        values.add(ret);
        return ret;
    }

}
