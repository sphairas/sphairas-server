/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.Tag;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.niedersachsen.zeugnis.TermReportNoteSetTemplate;

/**
 *
 * @author boris.heithecker
 */
class CurrentStudentsNotesSelection {

    private final StudentId student;
    private final DocumentId report;
    private final UnitId unit;
    private final List<ElementSelection> selection = new ArrayList<>();
    Object[] formatArgs;
    String stufe = "";

    CurrentStudentsNotesSelection(StudentId student, DocumentId report, UnitId unit) {
        this.student = student;
        this.report = report;
        this.unit = unit;
    }

    public StudentId getStudent() {
        return student;
    }

    public DocumentId getReport() {
        return report;
    }

    public UnitId getUnit() {
        return unit;
    }

    ElementSelection addElement(final TermReportNoteSetTemplate.Element e) {
        ElementSelection add = new ElementSelection(e);
        selection.add(add);
        return add;
    }

    List<ElementSelection> getSelectedElements() {
        return selection;
    }

    public static class ElementSelection {

        final TermReportNoteSetTemplate.Element el;
        private final VetoableChangeSupport pSupport = new VetoableChangeSupport(this);
        private List<String> selectedMultiple = new ArrayList<>();
        private String selected;

        private ElementSelection(TermReportNoteSetTemplate.Element el) {
            this.el = el;
        }

        public boolean isMultiple() {
            return el.isMultiple();
        }

        public String getElementDisplayName() {
            return el.getElementDisplayName();
        }

        public boolean isHidden() {
            return el.isHidden();
        }

        public List<TermReportNoteSetTemplate.MarkerItem> getMarkers() {
            return el.getMarkers();
        }

        Marker forId(String id) {
            return el.forId(id);
        }

        public String getSelectedItem() {
            return selected;
        }

        public List<String> getSelected() {
            return selectedMultiple;
        }

        public void setSelectedItem(String value) {
            String old = selected;
            try {
                selected = value;
                pSupport.fireVetoableChange("selectedItem", old, value);
            } catch (PropertyVetoException ex) {
                selected = old;
            }
        }

        public void setSelected(List<String> value) {
            if (value == null) {
                value = Collections.EMPTY_LIST;
            }
            List<String> old = getSelected();
            try {
                selectedMultiple = value;
                pSupport.fireVetoableChange("selected", old, value);
            } catch (PropertyVetoException ex) {
                selectedMultiple = old;
            }
        }

        public List<Tag> getDisplayHints() {
            return el.getDisplayHints();
        }

        void addVetoableChangeListener(VetoableChangeListener l) {
            pSupport.addVetoableChangeListener(l);
        }

    }
}
