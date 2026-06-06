/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.util.Comparator;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ResourceBundle;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeFactory;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.model.MultiSubject;
import org.thespheres.betula.document.model.Subject;
import org.thespheres.betula.niedersachsen.zeugnis.NdsReportBuilderFactory;

/**
 *
 * @author boris.heithecker
 */
@Named("util")
@ApplicationScoped
public class Util {

    @Inject
    private NdsReportBuilderFactory reportBuilderFactory;

    public String label(MultiSubject ms) {
//        final Comparator<Subject> comp = zgnConfig.getSubjectComparator();
        final Comparator<Marker> mComp = Comparator.comparing(fm -> new Subject(fm, ms.getRealmMarker()), getSubjectComparator());
        String ret = ms.getSubjectMarkerSet().stream()
                .sorted(mComp)
                .map(Marker::getShortLabel)
                .collect(Collectors.joining("-"));
        if (ms.getRealmMarker() != null) {
            ret += " [" + ms.getRealmMarker().getShortLabel() + "]";
        }
        return ret;
    }

    public String tooltip(MultiSubject ms) {
//        final Comparator<Subject> comp = zgnConfig.getSubjectComparator();
        final Comparator<Marker> mComp = Comparator.comparing(fm -> new Subject(fm, ms.getRealmMarker()), getSubjectComparator());
        String ret; 
        if(ms.getSubjectMarkerSet().size() == 1) {
            ret = ms.getSubjectMarkerSet().iterator().next().getLongLabel();
        } else
            ret = ms.getSubjectMarkerSet().stream()
                .sorted(mComp)
                .map(Marker::getLongLabel)
                .collect(Collectors.joining(", ", "(", ")"));
        if (ms.getRealmMarker() != null) {
            ret += " (" + ms.getRealmMarker().getLongLabel() + ")";
        }
        return ret;
    }

    public static String trimToNull(final String str) {
        final String ts = str == null ? null : str.trim();
        return ts == null || ts.length() == 0 ? null : ts;
    }

    private Comparator<Subject> getSubjectComparator() {
        return (s1, s2) -> reportBuilderFactory.forCareer(null).compare(s1.getSubjectMarker(), s2.getSubjectMarker());
    }

    public static Grade find(final String representation) {
        if (representation != null && !representation.isEmpty()) {
            final int i = representation.indexOf('#');
            if (i != -1) {
                final String cnv = representation.substring(0, i);
                final String id = representation.substring(i + 1);
                return GradeFactory.find(cnv, id);
            }
        }
        return null;
    }

    static String getBundleValue(final String key) {
        FacesContext context = FacesContext.getCurrentInstance();
        ResourceBundle bundle = context.getApplication().getResourceBundle(context, "bundle");
        return bundle.getString(key);
    }

}
