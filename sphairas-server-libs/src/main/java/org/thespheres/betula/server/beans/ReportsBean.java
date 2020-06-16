/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import javax.ejb.Local;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.util.Ordered;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface ReportsBean {

    public static final String TYPE_FEHLTAGE = "fehltage";
    public static final String TYPE_UNENTSCHULDIGT = "unentschuldigt";

    public DocumentId[] findTermReports(StudentId student, TermId term, boolean create);

    public StudentId getStudent(DocumentId zgnId);

    public TermId getTerm(DocumentId zgnId);

    public Marker[] getMarkers(DocumentId zeugnis);

    public boolean addMarker(DocumentId zeugnis, Marker m);

    public boolean removeMarker(DocumentId zeugnis, Marker m);

    public Grade getKopfnote(DocumentId zeugnis, String convention);

    public boolean setKopfnote(DocumentId zeugnis, String convention, Grade grade) throws TermReportDataException;

    public Integer getIntegerValue(DocumentId zeugnis, String type);

    public boolean setIntegerValue(DocumentId zeugnis, String type, Integer value) throws TermReportDataException;

    public CustomNote[] getCustomNotes(DocumentId zeugnis);

    public void setCustomNotes(DocumentId zeugnis, final CustomNote[] notes);
    
    public String getNote(DocumentId zgn, String key);
    
    public void setNote(DocumentId zgn, String key, String value);

    public String[] getAGs(StudentId student, TermId term);

    public static class CustomNote implements Ordered, Externalizable {

        private int position;
        private String value;

        public CustomNote() {
        }

        public CustomNote(int position, String value) {
            this.position = position;
            this.value = value;
        }

        @Override
        public int getPosition() {
            return position;
        }

        public String getValue() {
            return value;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(position);
            out.writeUTF(value);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            position = in.readInt();
            value = in.readUTF();
        }

    }
}
