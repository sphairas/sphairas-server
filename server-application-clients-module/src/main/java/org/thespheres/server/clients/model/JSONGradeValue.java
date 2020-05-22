/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.config.PropertyOrderStrategy;

/**
 *
 * @author boris.heithecker
 */
@JsonbPropertyOrder(PropertyOrderStrategy.ANY)
public class JSONGradeValue extends JSONGrade {

    @JsonbProperty("convention")
    private String convention;
    @JsonbProperty("column-ref")
    private String columnRef;
    @JsonbProperty("editable")
    private Boolean editable;
    @JsonbProperty("options")
    private List<Option> options;

    public JSONGradeValue(final String columnRef, final String convention, final String id) {
        super(id);
        this.columnRef = columnRef;
        this.convention = convention;
    }

    public JSONGradeValue(final String convention, final String id) {
        this(null, convention, id);
    }

    @JsonbCreator
    public JSONGradeValue(@JsonbProperty("id") final String id) {
        this(null, null, id);
    }

    public String getColumnRef() {
        return columnRef;
    }

    public void setColumnRef(String columnRef) {
        this.columnRef = columnRef;
    }

    public String getConvention() {
        return convention;
    }

    public boolean isEditable() {
        return editable != null ? editable : false;
    }

    public void setEditable(final boolean editable) {
        this.editable = editable ? true : null;
    }

    public List<Option> getOptions() {
        return options != null ? Collections.unmodifiableList(options) : Collections.EMPTY_LIST;
    }

    public void addOption(final Option option) {
        if (options == null) {
            options = new ArrayList<>();
        }
        options.add(option);
    }

    @JsonbPropertyOrder(PropertyOrderStrategy.ANY)
    public static class Option {

    }
}
