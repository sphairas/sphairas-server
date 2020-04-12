/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar;

import org.thespheres.ical.impl.ParameterList;
import java.io.Serializable;
import java.util.List;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.thespheres.ical.InvalidComponentException;
import org.thespheres.ical.Parameter;
import org.thespheres.ical.builder.AbstractComponentBuilder;
import org.thespheres.ical.builder.VCardBuilder;

/**
 * A property within an iCalendar component (e.g., DTSTART, DTEND, etc., within
 * a VEVENT).
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddableComponentProperty implements Serializable {

    private static final long serialVersionUID = 1L;
    @Column(name = "PROPERTY_NAME", length = 64)
    protected String name;
    @Column(name = "PROPERTY_VALUE")
    private String propValue;
    @Column(name = "PROPERTY_PARAMETERS")
    private final ParameterList propParameters = new ParameterList();

    public EmbeddableComponentProperty() {
    }

    public EmbeddableComponentProperty(String name) {
        this.name = name;
    }

    public EmbeddableComponentProperty(String name, String value) {
        this.name = name;
        this.propValue = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return propValue;
    }

    public List<Parameter> getParameters() {
        return propParameters.getList();
    }

    public void toString(StringBuilder sb) {
        propertyToString(sb, getName(), getValue(), propParameters);
    }

    public void toComponentProperty(AbstractComponentBuilder cb) throws InvalidComponentException {
        if (propParameters.getList().isEmpty()) {
            cb.addProperty(name, propValue);
        } else {
            Parameter[] pp = propParameters.getList().toArray(new Parameter[propParameters.getList().size()]);
            cb.addProperty(name, propValue, pp);
        }
    }

    public static void propertyToString(StringBuilder sb, String name, String value, ParameterList params) {
        sb.append(name);
        params.toString(sb);
        sb.append(":").append(value).append(VCardBuilder.NEWLINE);
    }

}
