/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
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
@Deprecated
public final class FastTextTermTargetDocument implements TargetDocument, TextTermTargetAssessment<Entry> {

    private final Map<StudentId, Map<TermId, List<FastTextTermTargetDocument.Entry>>> values;
    private final Set<Marker> markers;
    private final String convention;
    private final DocumentId document;
    private final Map<String, Signee> matchingSigneeTypes;
    private final String targetType;
    private final ZonedDateTime expirationDate;

    @JsonbCreator
    public FastTextTermTargetDocument(
            @JsonbProperty("document") DocumentId id,
            @JsonbProperty("values") Map<StudentId, Map<TermId, List<FastTextTermTargetDocument.Entry>>> values,
            @JsonbProperty("markers") Set<Marker> markers,
            @JsonbProperty("convention") String convention,
            @JsonbProperty("matchingSigneeTypes") Map<String, Signee> signees,
            @JsonbProperty("targetType") String targetType,
            @JsonbProperty("expirationDate") ZonedDateTime expiration) {
        this.document = id;
        this.values = values;
        this.markers = markers;
        this.convention = convention;
        this.matchingSigneeTypes = signees;
        this.targetType = targetType;
        this.expirationDate = expiration;
    }

    @JsonbProperty("document")
    public DocumentId getDocument() {
        return document;
    }

    @JsonbProperty("values")
    public Map<StudentId, Map<TermId, List<FastTextTermTargetDocument.Entry>>> getValues() {
        return values;
    }

    @JsonbTransient
    @Override
    public boolean isFragment() {
        return true;
    }

    @JsonbProperty("markers")
    @Override
    public Marker[] markers() {
        return markers.stream().toArray(Marker[]::new);
    }

    @JsonbTransient
    public Set<StudentId> students(TermId term) {
        return values.keySet().stream()
                .filter(s -> values.get(s)
                .containsKey(term))
                .distinct()
                .collect(Collectors.toSet());
    }

    @JsonbTransient
    @Override
    public Set<StudentId> students() {
        return values.keySet().stream()
                .collect(Collectors.toSet());
    }

    @JsonbTransient
    @Override
    public Set<TermId> identities() {
        return values.values().stream().collect(Collector.of(HashSet::new, (s, m) -> s.addAll(m.keySet()), (s1, s2) -> {
            s1.addAll(s2);
            return s1;
        }));
    }

    @JsonbTransient
    @Override
    public List<Entry> select(final StudentId student, final TermId term) {
        return values.getOrDefault(student, (Map<TermId, List<Entry>>) Collections.EMPTY_MAP)
                .getOrDefault(term, (List<Entry>) Collections.EMPTY_LIST);

    }

    @JsonbProperty("convention")
    @Override
    public String getPreferredConvention() {
        return convention;
    }

    @JsonbProperty("targetType")
    @Override
    public String getTargetType() {
        return targetType;
    }

    @JsonbProperty("expirationDate")
    public ZonedDateTime getExpirationDate() {
        return expirationDate;
    }

    @JsonbProperty("matchingSigneeTypes")
    @Override
    public Map<String, Signee> getSignees() {
        return matchingSigneeTypes;
    }

    @JsonbTransient
    @Override
    public void submit(StudentId student, TermId gradeId, List<Entry> grade, Timestamp timestamp) {
        throw new UnsupportedOperationException("Not permitted.");
    }

    @JsonbTransient
    @Override
    public Validity getDocumentValidity() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @JsonbTransient
    @Override
    public SigneeInfo getCreationInfo() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @JsonbTransient
    @Override
    public void addListener(Listener listener) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @JsonbTransient
    @Override
    public void removeListener(Listener listener) {
        throw new UnsupportedOperationException("Not supported.");
    }

    public static final class Entry implements TextEntry {

        public final String text;
        public final java.sql.Timestamp timestamp;
        public final Marker section;

        @JsonbCreator
        public Entry(
                @JsonbProperty("section") final Marker section,
                @JsonbProperty("text") final String val,
                @JsonbProperty("timestamp") final java.sql.Timestamp timestamp) {
            this.section = section;
            this.text = val;
            this.timestamp = timestamp;
        }

        @JsonbProperty("text")
        @Override
        public String getText() {
            return text;
        }

        @JsonbProperty("timestamp")
        @Override
        public java.sql.Timestamp getTimestamp() {
            return timestamp;
        }

        @JsonbProperty("section")
        @Override
        public Marker getSection() {
            return section;
        }

    }

}
