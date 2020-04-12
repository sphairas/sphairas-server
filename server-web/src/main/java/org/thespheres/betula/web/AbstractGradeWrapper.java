/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Objects;
import org.thespheres.betula.assess.AssessmentConvention;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeReference;

/**
 *
 * @author boris.heithecker
 */
public abstract class AbstractGradeWrapper {

    private final AssessmentConvention[] conventions;
    private final String message;

    protected AbstractGradeWrapper(AssessmentConvention[] conventions, String message) {
        this.conventions = conventions;
        this.message = message;
    }

    protected AbstractGradeWrapper(AssessmentConvention convention) {
        this(new AssessmentConvention[]{convention}, null);
    }

    protected AssessmentConvention[] getConventions() {
        return conventions;
    }

    protected Grade findGrade(final String id) {
        return Arrays.stream(getConventions())
                .filter(Objects::nonNull)
                .map(ac -> ac.find(id))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public String getId() {
        return getGrade() != null ? getGrade().getId() : "";
    }

    public void setId(final String id) {
        if (id == null) {
            return;
        }
        Grade g;
        if ("".equals(id)) {
            g = null;
        } else {
            g = findGrade(id);
        }
        if (g == null) {
            g = resolveExtraId(id);
        }
        if (g != null) {
            setGrade(g);
        }
    }

    protected abstract Grade getGrade();

    protected abstract void setGrade(Grade g);

    protected abstract boolean invalidateGrade(Grade newValue, Timestamp time);

    protected abstract boolean invalidateTickets();

    public String getShortLabel() {
        return getLabel(false);
    }

    public String getLongLabel() {
        return getLabel(true);
    }

    protected String getLabel(final boolean lng) {
        if (message != null) {
            return message;
        }
        final Grade g = getGrade();
        if (g != null) {
            if (g instanceof GradeReference) {
                final Grade orig = resolveReference((GradeReference) g);
                if (orig != g) {
                    return g.getLongLabel(orig);
                }
            }
            return lng ? g.getLongLabel() : g.getShortLabel();
        }
        return "";
    }

    protected abstract Grade resolveReference(GradeReference proxy);

    protected Grade resolveExtraId(String gradeId) {
        return null;
    }
}
