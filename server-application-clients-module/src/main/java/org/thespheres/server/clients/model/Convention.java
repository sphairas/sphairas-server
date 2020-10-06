/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.config.PropertyOrderStrategy;
import org.thespheres.betula.assess.AssessmentConvention;

/**
 *
 * @author boris.heithecker
 */
@JsonbPropertyOrder(PropertyOrderStrategy.ANY)
public class Convention {

    private final String name;
    private String display;
    private final List<Grade> values = new ArrayList<>();

    @JsonbCreator
    public Convention(@JsonbProperty("name") String name) {
        this.name = name;
    }

    @SuppressWarnings({"OverridableMethodCallInConstructor"})
    public Convention(final AssessmentConvention cnv) {
        this(cnv.getName());
        setDisplay(cnv.getDisplayName());
        Arrays.stream(cnv.getAllGrades())
                .map(m -> new Grade(m))
                .forEach(values::add);
    }

    @JsonbProperty("name")
    public String getName() {
        return name;
    }

    @JsonbProperty("display")
    public String getDisplay() {
        return display;
    }

    @JsonbProperty("display")
    public void setDisplay(final String display) {
        this.display = display;
    }

    @JsonbProperty("grades")
    public List<Grade> getGrades() {
        return Collections.unmodifiableList(values);
    }

    @JsonbProperty("grades")
    public void setGrades(final List<Grade> l) {
        l.stream().forEach(this.values::add);
    }

}
