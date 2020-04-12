/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.niedersachsen.zeugnis.Constants;
import org.thespheres.betula.niedersachsen.zeugnis.TermReportNoteSetTemplate;
import org.thespheres.betula.niedersachsen.zeugnis.TermReportNoteSetTemplate.MarkerItem;
import org.thespheres.betula.server.beans.ReportsBean.CustomNote;
import org.thespheres.betula.services.LocalProperties;
import org.thespheres.betula.util.CollectionUtil;

/**
 *
 * @author boris.heithecker
 */
@RequestScoped
public class BemerkungenBean {

    @EJB(beanName = "ReportsBeanImpl")
    private org.thespheres.betula.server.beans.ReportsBean zeugnisBean;
    @EJB
    private ZeugnisArguments zeugnisArguments;
    @Inject
    private TermReportNoteSetTemplate reportNoteTemplate;
    @Inject
    private LocalProperties properties;

    public String createBemerkungen(DocumentId zgnId, UnitId pu) {
        final Object[] args = zeugnisArguments.getFormatArgs(zgnId, pu); //new Object[]{vorname, gen, possesiv, kk, nStufe, nSJ};
        final Marker[] arr = zeugnisBean.getMarkers(zgnId);
        //filter markers
        final Set<String> cnv = conventions();
        final Set<Marker> markers = Arrays.stream(arr)
                .filter(m -> cnv.contains(m.getConvention()))
                .collect(Collectors.toSet());
        //sort markers
//        final int size = reportNoteTemplate.getElements().size();
//        final List[] notes = new List[size + 2];
        final SortedMap<Integer, List> notes2 = new TreeMap<>();
        //Custom notes
        for (final CustomNote cn : zeugnisBean.getCustomNotes(zgnId)) {
            final int pos = cn.getPosition();
            if (pos != -1) {
                notes2.computeIfAbsent(pos, p -> new ArrayList()).add(cn);
            }
//            if (pos == -1) {
//                addToList(notes, 0, cn);
//            } else if (pos >= size) {
//                addToList(notes, size + 1, cn);
//            } else {
//                addToList(notes, pos + 1, cn);
//            }
        }
        //Marker notes
        markers.forEach(m -> {
            final int pos = positionOf(m);
            notes2.computeIfAbsent(pos, p -> new ArrayList()).add(m);
//            if (pos == Integer.MAX_VALUE) {
//                addToList(notes, size + 1, m);
//            } else {
//                addToList(notes, pos + 1, m);
//            }
        });
        //Create string
        final StringJoiner bemerkungen = new StringJoiner(" ");
        notes2.values().forEach(l -> {
            l.forEach(o -> {
                if (o instanceof Marker) {
                    final Marker m = (Marker) o;
                    bemerkungen.add(m.getLongLabel(args));
                } else if (o instanceof CustomNote) {
                    final CustomNote cn = (CustomNote) o;
                    bemerkungen.add(cn.getValue());
                }
            });
        });
//        for (List l : notes) {
//            if (l == null) {
//                continue;
//            }
//            l.forEach(o -> {
//                if (o instanceof Marker) {
//                    final Marker m = (Marker) o;
//                    bemerkungen.add(m.getLongLabel(args));
//                } else if (o instanceof CustomNote) {
//                    final CustomNote cn = (CustomNote) o;
//                    bemerkungen.add(cn.getValue());
//                }
//            });
//        }
        return bemerkungen.toString();
    }

//    private void addToList(final List[] notes, final int indexs, final Object v) {
//        if (notes[indexs] == null) {
//            notes[indexs] = new ArrayList();
//        }
//        notes[indexs].add(v);
//    }
    private Set<String> conventions() {
        final Set<String> ret = new HashSet<>();
        final String cnv = properties.getProperty("report.notes.conventions");
        if (cnv != null) {
            Arrays.stream(cnv.split(","))
                    .map(String::trim)
                    .forEach(ret::add);
        }
        final String customcnv = properties.getProperty("custom.report.notes.conventions");
        if (customcnv != null) {
            Arrays.stream(customcnv.split(","))
                    .map(String::trim)
                    .forEach(ret::add);
        }
        return ret;
    }

    //returns 0...reportNoteTemplate.getElements().size()-1 || Integer.MAX_VALUE
    private int positionOf(final Marker item) {
        return reportNoteTemplate.getElements().stream()
                .filter(el -> el.getMarkers().stream().map(MarkerItem::getMarker).anyMatch(item::equals))
                .map(e -> e.getDisplayHints().contains(Constants.SCHLUSS_BEMERKUNG) ? Integer.MAX_VALUE : reportNoteTemplate.getElements().indexOf(e))
                //                .filter(Objects::nonNull)
                .collect(CollectionUtil.singleton())
                .orElse(Integer.MAX_VALUE);
    }
}
