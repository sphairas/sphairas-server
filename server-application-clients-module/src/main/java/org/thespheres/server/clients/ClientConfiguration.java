/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.json.bind.annotation.JsonbProperty;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Signee;
import org.thespheres.server.clients.model.Convention;
import org.thespheres.server.clients.model.Property;

/**
 *
 * @author boris.heithecker@gmx.net
 */
public class ClientConfiguration implements Serializable {

    @JsonbProperty("authority")
    private final String authority;
    @JsonbProperty("students-authority")
    private final String studentsAuthority;
    @JsonbProperty("term-authority")
    private final String termAuthority;
    @JsonbProperty("grade-convention")
    private String gradeConvention;
    private String suffix;
    @JsonbProperty("properties")
    private final Property.PropertyList properties = new Property.PropertyList();
    @JsonbProperty("conventions")
    private final List<Convention> conventions = new ArrayList<>();

    ClientConfiguration(final String authority, final String studentsAuthority, final String termAuthority) {
        this.authority = authority;
        this.studentsAuthority = studentsAuthority;
        this.termAuthority = termAuthority;
    }

    public String getAuthority() {
        return this.authority;
    }

    public String getStudentsAuthority() {
        return studentsAuthority;
    }

    public String getTermAuthority() {
        return termAuthority;
    }

    public String getGradeConvention() {
        return gradeConvention;
    }

    public void setGradeConvention(final String gradeConvention) {
        this.gradeConvention = gradeConvention;
    }

    public String getSigneeSuffix() {
        return suffix;
    }

    public void setSigneeSuffix(final String value) {
        this.suffix = value;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public List<Convention> getConventions() {
        return conventions;
    }

    public String toString(final DocumentId doc) {
        if (!Objects.equals(getAuthority(), doc.getAuthority()) || !DocumentId.Version.LATEST.equals(doc.getVersion())) {
            return doc.toString();
        }
        return doc.getId();
    }

    public String toString(final UnitId unit) {
        if (!Objects.equals(getAuthority(), unit.getAuthority())) {
            return unit.toString();
        }
        return unit.getId();
    }

    public String toString(final StudentId student) {
        if (!Objects.equals(getStudentsAuthority(), student.getAuthority())) {
            return student.toString();
        }
        return Long.toString(student.getId());
    }

    public String toString(final TermId term) {
        if (!Objects.equals(getTermAuthority(), term.getAuthority())) {
            return term.toString();
        }
        return Integer.toString(term.getId());
    }

    public String toString(final Signee signee) {
        if (!Objects.equals(getSigneeSuffix(), signee.getSuffix())) {
            return signee.toString();
        }
        return signee.getPrefix();
    }

    public String toString(final Grade grade) {
        if (!Objects.equals(getGradeConvention(), grade.getConvention())) {
            return grade.toString();
        }
        return grade.getId();
    }

}
