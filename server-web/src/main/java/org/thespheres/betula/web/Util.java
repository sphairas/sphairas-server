/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.util.Comparator;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.model.MultiSubject;
import org.thespheres.betula.document.model.Subject;

/**
 *
 * @author boris.heithecker
 */
@Named("util")
@ApplicationScoped
public class Util {

    @Inject
    private Comparator<Subject> comp;

    public String label(MultiSubject ms) {
//        final Comparator<Subject> comp = zgnConfig.getSubjectComparator();
        final Comparator<Marker> mComp = Comparator.comparing(fm -> new Subject(fm, ms.getRealmMarker()), comp);
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
        final Comparator<Marker> mComp = Comparator.comparing(fm -> new Subject(fm, ms.getRealmMarker()), comp);
        String ret = ms.getSubjectMarkerSet().stream()
                .sorted(mComp)
                .map(Marker::getLongLabel)
                .collect(Collectors.joining(", ", "(", ")"));
        if (ms.getRealmMarker() != null) {
            ret += " [" + ms.getRealmMarker().getLongLabel() + "]";
        }
        return ret;
    }

    public static String trimToNull(final String str) {
        final String ts = str == null ? null : str.trim();
        return ts == null || ts.length() == 0 ? null : ts;
    }
}
