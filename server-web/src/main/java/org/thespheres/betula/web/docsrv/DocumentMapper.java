/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import org.thespheres.betula.server.beans.AmbiguousResultException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.MarkerConvention;
import org.thespheres.betula.services.CommonTargetProperties;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.document.model.MultiSubject;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;
import org.thespheres.betula.server.beans.TargetDocumentsLocalBean;
import org.thespheres.betula.server.beans.annot.Arbeitsgemeinschaft;
import org.thespheres.betula.services.LocalProperties;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class DocumentMapper {

    @EJB(beanName = "TargetDocumentsLocalBeanImpl")
    private TargetDocumentsLocalBean mtad;
    @Default
    @Inject
    private DocumentsModel docModel;
    @Inject
    private CommonTargetProperties targetProps;
    @Arbeitsgemeinschaft
    @Inject
    private Marker ag;
    @Inject
    private LocalProperties properties;

    //TODO: statt collection<marker> uniquemarker set from markerdecoration
    public Map<String, Map<MultiSubject, Set<DocumentId>>> getDocMap(Collection<DocumentId> docs, boolean ignoreRealm) {
        final Map<DocumentId, Collection<Marker>> markerMap = docs.stream()
                .collect(Collectors.toMap(Function.identity(), mtad::getDocumentMarkers));
        return createTargetDocumentsMap(markerMap, ignoreRealm);
    }

    public Set<DocumentId> filterAGs(Collection<DocumentId> docs) {
        return docs.stream()
                .filter(d -> mtad.getDocumentMarkers(d).contains(ag))
                .collect(Collectors.toSet());
    }

    public MultiSubject getSubject(final DocumentId d) {
        final Collection<Marker> markerMap = mtad.getDocumentMarkers(d);
        return markerMap != null ? getSubjectImpl(markerMap, d, false) : null;
    }

    private MultiSubject getSubjectImpl(final Collection<Marker> markerMap, final DocumentId dod, final boolean ignoreWpk) {
        final Set<Marker> fach = new HashSet<>();
        Marker realm = null;
        for (final Marker m : markerMap) {
            if (isFachMarker(m)) {
                fach.add(m);
            } else if (!ignoreWpk && isRealmMarker(m)) {
                if (realm != null) {//error mehr als ein realm
                    fach.clear();
                    break;
                } else {
                    realm = m;
                }
            }
        }
        if (!fach.isEmpty()) {
            final MultiSubject ms = new MultiSubjectExt(realm, null);
            ms.getSubjectMarkerSet().addAll(fach);
            return ms;
        } else if (realm != null) {
            final String alt = mtad.getSubjectAlternativeName(dod);
            if (alt != null) {//TODO: Check properties if allowed
                return new MultiSubjectExt(realm, alt);
            }
        }
        return null;
    }

    //WebUIConfiguration
    private Map<String, Map<MultiSubject, Set<DocumentId>>> createTargetDocumentsMap(final Map<DocumentId, Collection<Marker>> markerMap, final boolean ignoreWpk) {
        final HashMap<String, Map<MultiSubject, Set<DocumentId>>> docTypes = new HashMap<>();
        final HashMap<DocumentId, MultiSubject> smap = new HashMap<>();
        for (final DocumentId d : markerMap.keySet()) {
            final MultiSubject ms = getSubjectImpl(markerMap.get(d), d, ignoreWpk);
            if (ms != null) {
                smap.put(d, ms);
            }
        }
        for (DocumentId d : markerMap.keySet()) {
            final String type = docModel.getSuffix(d);
            if (type == null) {
                continue;
            }
            final MultiSubject mar = smap.get(d);
            if (mar == null || (mar.getSubjectMarkerSet().isEmpty() && mar.getRealmMarker() == null)) {
                continue;
            }
            final Map<MultiSubject, Set<DocumentId>> dm = docTypes.computeIfAbsent(type, t -> new HashMap<>());
            dm.computeIfAbsent(mar, s -> new HashSet<>()).add(d);
        }
        return docTypes;
    }

    public boolean isFachMarker(Marker m) {
        final String cnv = m.getConvention();
        return Arrays.stream(targetProps.getSubjectMarkerConventions())
                .map(MarkerConvention::getName)
                .anyMatch(cnv::equals);
    }

    public boolean isRealmMarker(Marker m) {
        if (m == null) {
            return false;
        }
        final String cnv = m.getConvention();
        return Arrays.stream(targetProps.getRealmMarkerConventions())
                .map(MarkerConvention::getName)
                .anyMatch(cnv::equals);
    }

    public DocumentId find(final Collection<FastTermTargetDocument> docs, final StudentId student, final TermId term) throws AmbiguousDocumentCollectionException {
        final DocumentId[] ds = docs.stream()
                .filter(d -> d.select(student, term) != null)
                .map(FastTermTargetDocument::getDocument)
                .toArray(DocumentId[]::new);
        return checkResult(ds, student);
    }

    public DocumentId findTexts(final Collection<FastTextTermTargetDocument> docs, final StudentId student, final TermId term) throws AmbiguousResultException {
        final DocumentId[] ds = docs.stream()
                .filter(d -> !d.select(student, term).isEmpty())
                .map(FastTextTermTargetDocument::getDocument)
                .toArray(DocumentId[]::new);
        return checkResult(ds, student);
    }

    public static DocumentId checkResult(DocumentId[] ds, StudentId student) throws AmbiguousDocumentCollectionException {
        if (ds.length > 1) {
            AmbiguousDocumentCollectionException ex = new AmbiguousDocumentCollectionException(student);
            ex.setDocumentCollection(ds);
            throw ex;
        } else if (ds.length == 0) {
            return null;
        } else {
            return ds[0];
        }
    }
}
