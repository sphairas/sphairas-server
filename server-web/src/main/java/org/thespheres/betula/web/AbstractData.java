/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.component.UIComponent;
import javax.faces.event.ComponentSystemEvent;
import org.primefaces.component.datatable.DataTable;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeReference;
import org.thespheres.betula.services.jms.MultiTargetAssessmentEvent;
import org.thespheres.betula.services.jms.TicketEvent;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.services.ws.CommonDocuments;
import org.thespheres.betula.util.StudentComparator;
import org.thespheres.ical.VCard;

/**
 *
 * @author boris.heithecker
 * @param <C>
 */
public abstract class AbstractData<C> {

    protected final BetulaWebApplication application;
    private ArrayList<AvailableStudent> students;
    private final String displayTitle;
    private final Collator collator = Collator.getInstance(Locale.GERMANY);
    private boolean active;
    protected String dataTableClientId;
    private final DocumentId studentSGLMarkerDocId;
    boolean valid = true;
    private final Comparator<AvailableStudent> comparator = Comparator.comparing(l -> StudentComparator.sortStringFromDirectoryName(l.getDirectoryName()), collator);

    protected AbstractData(BetulaWebApplication app, String displayTitle) {
        this.application = app;
        this.displayTitle = displayTitle;
        this.studentSGLMarkerDocId = application.getCommonDocuments().forName(CommonDocuments.STUDENT_CAREERS_DOCID);
    }

    public String getDisplayTitle() {
        return displayTitle;
    }

    public boolean isActiveTab() {
        return this.active;
    }

    public void setActiveTab(boolean value) {
        this.active = value;
    }

    public boolean isValid() {
        return valid;
    }

    public List<AvailableStudent> getStudents() {
        if (students == null) {
            students = new ArrayList<>();
            for (final StudentId sid : createStudents()) {
                String dirName;
                VCard scv = application.getVCard(sid);
                if (scv != null) {
                    dirName = scv.getFN();
                } else {
                    dirName = sid.toString();
                    Logger.getLogger(getClass().getCanonicalName()).log(Level.WARNING, "No StudentCardBean entity existing for {0}", sid.toString());
                }
                Marker sgl = null;
                if (studentSGLMarkerDocId != null) {
                    sgl = application.getStudentMarkerEntry(sid, studentSGLMarkerDocId); //studentBeanRemote.getMarkerEntry(sid, studentSGLMarkerDocId);
                }
                AvailableStudent as = createAvailableStudent(sid, dirName, sgl);
                students.add(as);
            }
            Collections.sort(students, comparator);
        }
        return students;
    }

    protected AvailableStudent findStudent(StudentId sid) {
        return getStudents().stream().filter(as -> as.getId().equals(sid)).findAny().orElse(null);
    }

    protected AvailableStudent createAvailableStudent(StudentId sid, String dirName, Marker sgl) {
        return new AvailableStudent(sid, dirName, sgl);
    }

    protected abstract Set<StudentId> createStudents();

    public void onPreRenderComponent(ComponentSystemEvent evt) {
        final UIComponent uic = evt.getComponent();
        if (uic instanceof DataTable) {
            dataTableClientId = uic.getClientId();
        } else {
            dataTableClientId = null;
        }
    }

    public String resolveShortLabel(AvailableStudent stud, C column, final Grade original) {
        if (original instanceof GradeReference) {
            GradeReference proxy = (GradeReference) original;
            Grade gv = resolveReference(stud, column, proxy);
            if (gv != proxy) {
                return proxy.getLongLabel(gv);
            }
        }
        return original.getShortLabel();
    }

    protected void onDocumentEvent(MultiTargetAssessmentEvent<TermId> evt) {
    }

    protected void onTicketEvent(TicketEvent evt) {
    }

    protected abstract Grade resolveReference(AvailableStudent stud, C column, GradeReference proxy);
}
