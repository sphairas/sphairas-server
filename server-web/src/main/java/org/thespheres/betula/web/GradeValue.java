/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.assess.AssessmentConvention;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeReference;
import org.thespheres.betula.assess.TargetDocument;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.model.MultiSubject;
import org.thespheres.betula.niedersachsen.Uebertrag;
import org.thespheres.betula.niedersachsen.vorschlag.AVSVVorschlag;
import org.thespheres.betula.niedersachsen.vorschlag.VorschlagDecoration;
import org.thespheres.betula.server.beans.FastTermTargetDocument.Entry;
import org.thespheres.betula.util.CollectionUtil;
import org.thespheres.betula.web.config.ExtraAnnotation;

/**
 *
 * @author boris.heithecker
 */
public class GradeValue extends AbstractGradeWrapper {

    private final StudentId studId;
    private final DocumentId doc;
    private final TermId term;
    private final BetulaWebApplication application;
    private Grade grade;
    private boolean invalidated = false;
    private final Object INVALIDATED_LOCK = new Object();
    private Ticket[] tickets;
    private final AtomicReference<Grade> gradeSubmitting = new AtomicReference<>();
    private final boolean baseEditableProperty;
    private java.sql.Timestamp timestamp;
    private final String targetType;
    private final MultiSubject subject;

    public GradeValue(BetulaWebApplication app, DocumentId doc, TermId term, StudentId studId, AssessmentConvention[] convention, Entry initial, String message, boolean editable, String target, MultiSubject subject) {
        super(convention, message);
        this.application = app;
        this.term = term;
        this.doc = doc;
        this.studId = studId;
        this.baseEditableProperty = editable;
        this.grade = initial != null ? initial.grade : null;
        this.timestamp = initial != null ? initial.timestamp : null;
        this.targetType = target;
        this.subject = subject;
    }

    public DocumentId getDocument() {
        return doc;
    }

    public StudentId getStudent() {
        return studId;
    }

    boolean usesTicket(Ticket ticket) {
        if (tickets != null) {
            for (Ticket t : tickets) {
                if (t.equals(ticket)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected Grade getGrade() {
        if (invalidated && doc != null) {
            synchronized (INVALIDATED_LOCK) {
                IOException ioex = null;
                try {
                    //message = "FEHLER"
                    grade = application.selectGrade(doc, term, studId);
                } catch (IOException ex) {
                    ioex = ex;
                }
                if (baseEditableProperty) {
                    tickets = application.findApplicableTickets(doc, term, studId);
                }
                if (ioex == null) {
                    invalidated = false;
                }
            }
        }
        return grade;
    }

    @Override
    protected Grade resolveExtraId(final String gradeId) {
        final ExtraAnnotation extra = new ExtraAnnotation(targetType);
        final VorschlagDecoration deco = application.getAssessmentDecoration(extra);
        if (deco != null) {
            final List<AssessmentConvention> l = deco.getDecoration(doc, (TargetDocument) application.getFastDocument(doc));
            final Grade v = l.stream()
                    .map(ac -> ac.find(gradeId))
                    .filter(Objects::nonNull)
                    .collect(CollectionUtil.singleOrNull());
            if (v != null) {
                return v;
            }
        }
        return application.getExtraGrades().stream()
                .filter(g -> g.getId().equals(gradeId))
                .collect(CollectionUtil.singleOrNull());
    }

    @Override
    protected Grade resolveReference(GradeReference proxy) {
        if (AVSVVorschlag.NAME.equals(proxy.getConvention())) {
            final ExtraAnnotation extra = new ExtraAnnotation(targetType);
            VorschlagDecoration deco = application.getAssessmentDecoration(extra);
            if (deco != null) {
                return deco.resolveReference(proxy, studId, term, null);
            }
        } else if (Uebertrag.NAME.equals(proxy.getConvention()) && subject != null) {
            final PrimaryUnit pu = application.getUser().getCurrentPrimaryUnit();
            final GradeValue gvref = pu.gradeValueForDocType(studId, subject, "vorzensuren");//TODO: get for TermId
            if (gvref != null) {
                return gvref.getGrade();
            }
        }
        return proxy;
    }

    private Ticket[] getTickets() {
        if (tickets == null) {
            synchronized (INVALIDATED_LOCK) {
                tickets = new Ticket[0];
                if (baseEditableProperty) {
                    tickets = application.findApplicableTickets(doc, term, studId);
                }
            }
        }
        return tickets;
    }

    public boolean isEditable() {
        final Grade g = getGrade();
        final Ticket[] t = getTickets();
        return g != null
                && t.length > 0
                && (isPending() || isEditableGrade(g));
    }

    private boolean isEditableGrade(final Grade g) {
        return Arrays.stream(getConventions())
                .map(AssessmentConvention::getName)
                .anyMatch(g.getConvention()::equals)
                || application.getExtraGrades().contains(g);
    }

    @Override
    protected void setGrade(Grade g) {
        if (g != null && !Objects.equals(g, this.grade)) { // && !Objects.equals(g, gradeToUpdate[0])) {
            gradeSubmitting.set(g);
            boolean result = false;
            try {
                result = application.submitGrade(doc, term, studId, g);
            } catch (IOException ex) {
            }
            if (result) {
                this.grade = gradeSubmitting.getAndSet(null);
            } else {//message = "FEHLER"
                gradeSubmitting.lazySet(null);
            }
        }
    }

    @Override
    protected boolean invalidateGrade(final Grade newValue, java.sql.Timestamp time) {
        synchronized (INVALIDATED_LOCK) {
            if (timestamp == null || !timestamp.equals(time)) {
                timestamp = time;
            }
            if (!(Objects.equals(newValue, getGrade()))) {
                final Grade submitting = gradeSubmitting.getAndSet(newValue);
                if (submitting != null && submitting.equals(newValue)) {
                    return false;
                } else {
                    boolean test = gradeSubmitting.compareAndSet(newValue, null);
                    if (test) {
                        grade = newValue;
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Override
    protected boolean invalidateTickets() {
        synchronized (INVALIDATED_LOCK) {
            tickets = null;
            return true;
        }
    }

    public String getStyle() {
        if (isPending()) {
            return "display: block; background-color: #F5DEB3;";
        } else if (getShortLabel().startsWith("5") || getShortLabel().startsWith("6")) {
            return "color: red";
        } else {
            return "";
        }
    }

    private boolean isPending() {
        return "pending".equals(getId());
    }

}
