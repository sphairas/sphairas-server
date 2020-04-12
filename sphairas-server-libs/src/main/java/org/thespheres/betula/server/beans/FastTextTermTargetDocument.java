/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.TargetDocument;
import org.thespheres.betula.assess.TextTermTargetAssessment;
import org.thespheres.betula.assess.TextTermTargetAssessment.TextEntry;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.Timestamp;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument.Entry;

/**
 *
 * @author boris.heithecker
 */
public final class FastTextTermTargetDocument implements TargetDocument, TextTermTargetAssessment<Entry> {

    private final Map<StudentId, Map<TermId, List<FastTextTermTargetDocument.Entry>>> values;
    private final Set<Marker> markers;
    private final String convention;
    private final DocumentId document;
    private final Map<String, Signee> matchingSigneeTypes;
    private final String targetType;
    private final ZonedDateTime expirationDate;

    public FastTextTermTargetDocument(DocumentId id, Map<StudentId, Map<TermId, List<FastTextTermTargetDocument.Entry>>> values, Set<Marker> markers, String convention, Map<String, Signee> signees, String targetType, ZonedDateTime expiration) {
        this.document = id;
        this.values = values;
        this.markers = markers;
        this.convention = convention;
        this.matchingSigneeTypes = signees;
        this.targetType = targetType;
        this.expirationDate = expiration;
    }

    public DocumentId getDocument() {
        return document;
    }

    @Override
    public boolean isFragment() {
        return true;
    }

    @Override
    public Marker[] markers() {
        return markers.stream().toArray(Marker[]::new);
    }

    public Set<StudentId> students(TermId term) {
        return values.keySet().stream()
                .filter(s -> values.get(s)
                .containsKey(term))
                .distinct()
                .collect(Collectors.toSet());
    }

    @Override
    public Set<StudentId> students() {
        return values.keySet().stream()
                .collect(Collectors.toSet());
    }

    @Override
    public Set<TermId> identities() {
        return values.values().stream().collect(Collector.of(HashSet::new, (s, m) -> s.addAll(m.keySet()), (s1, s2) -> {
            s1.addAll(s2);
            return s1;
        }));
    }

    @Override
    public List<Entry> select(final StudentId student, final TermId term) {
        return values.getOrDefault(student, (Map<TermId, List<Entry>>) Collections.EMPTY_MAP)
                .getOrDefault(term, (List<Entry>) Collections.EMPTY_LIST);

    }

    @Override
    public String getPreferredConvention() {
        return convention;
    }

    @Override
    public String getTargetType() {
        return targetType;
    }

    public ZonedDateTime getExpirationDate() {
        return expirationDate;
    }

    @Override
    public Map<String, Signee> getSignees() {
        return matchingSigneeTypes;
    }

    @Override
    public void submit(StudentId student, TermId gradeId, List<Entry> grade, Timestamp timestamp) {
        throw new UnsupportedOperationException("Not permitted.");
    }

    @Override
    public Validity getDocumentValidity() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public SigneeInfo getCreationInfo() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void addListener(Listener listener) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void removeListener(Listener listener) {
        throw new UnsupportedOperationException("Not supported.");
    }

    public static final class Entry implements TextEntry {

        public final String text;
        public final java.sql.Timestamp timestamp;
        public final Marker section;

        public Entry(final Marker section, final String val, final java.sql.Timestamp timestamp) {
            this.section = section;
            this.text = val;
            this.timestamp = timestamp;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public java.sql.Timestamp getTimestamp() {
            return timestamp;
        }

        @Override
        public Marker getSection() {
            return section;
        }

    }

}
