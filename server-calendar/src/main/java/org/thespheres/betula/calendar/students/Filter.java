/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.students;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

/**
 *
 * @author boris.heithecker
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Filter {

    @XmlEnum
    public enum FilterType {

        @XmlEnumValue(value = "allof")
        ALLOF,
        @XmlEnumValue(value = "anyof")
        ANYOF

    }

    @XmlAttribute(name = "test", required = true)
    private FilterType test = FilterType.ANYOF;
    @XmlElement(name = "prop-filter", required = true)
    protected List<PropFilter> response = new ArrayList<>();
}
