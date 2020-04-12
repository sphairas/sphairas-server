/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.document.Marker;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "MARKER_VALUE_CHANGELOG")
@Access(AccessType.FIELD)
public class StudentMarkerMapChangeLog extends BaseChangeLog<Marker> implements Serializable {

    @Embedded
    protected EmbeddableMarker marker;
    @Embedded
    protected EmbeddableStudentId student;

    public StudentMarkerMapChangeLog() {
    }

    public StudentMarkerMapChangeLog(BaseDocumentEntity parent, StudentId student, Marker value, Action action) {
        super(parent, BaseIdentityMarkerMapDocumentEntity.BASE_IDENTITY_MARKER_MAP_DOCUMENT_VALUES, action);
        this.student = new EmbeddableStudentId(student);
        this.marker = value != null ? new EmbeddableMarker(value) : null;
    }

    @Override
    public Marker getValue() {
        return marker;
    }

    public StudentId getStudent() {
        return student.getStudentId();
    }

    @Override
    public String toString() {
        return StudentMarkerMapChangeLog.class.getName() + " : " + getProperty() + " : " + getAction().toString() + " : " + student.getStudentId().toString() + " : " + (marker != null ? marker.toString() : "null");
    }

}
