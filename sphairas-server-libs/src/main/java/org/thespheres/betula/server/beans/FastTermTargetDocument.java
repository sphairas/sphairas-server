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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeTermTargetAssessment;
import org.thespheres.betula.assess.TargetDocument;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.Timestamp;
import org.thespheres.betula.document.model.UnitsModel;

/**
 *
 * @author boris.heithecker
 */
@Deprecated
public final class FastTermTargetDocument implements TargetDocument, GradeTermTargetAssessment, UnitsModel.UnitsModelDocument {

    private final Map<StudentId, Map<TermId, Entry>> values;
    private final Set<Marker> markers;
    private final String convention;
    private final DocumentId document;
    private final Map<String, Signee> matchingSigneeTypes;
    private final String targetType;
    private final ZonedDateTime expirationDate;
    private final String subjectAltName;

    @JsonbCreator
    public FastTermTargetDocument(
            @JsonbProperty("document") DocumentId id,
            @JsonbProperty("values") Map<StudentId, Map<TermId, Entry>> values,
            @JsonbProperty("markers") Set<Marker> markers,
            @JsonbProperty("convention") String convention,
            @JsonbProperty("matchingSigneeTypes") Map<String, Signee> signees,
            @JsonbProperty("targetType") String targetType,
            @JsonbProperty("subjectAltName") String subjectAltName,
            @JsonbProperty("expirationDate") ZonedDateTime expiration) {
        this.document = id;
        this.values = values;
        this.markers = markers;
        this.convention = convention;
        this.matchingSigneeTypes = signees;
        this.targetType = targetType;
        this.subjectAltName = subjectAltName;
        this.expirationDate = expiration;
    }

    @JsonbProperty("document")
    public DocumentId getDocument() {
        return document;
    }

    @JsonbTransient
    @Override
    public DocumentId getDocumentId() {
        return document;
    }

    @JsonbProperty("values")
    public Map<StudentId, Map<TermId, Entry>> getValues() {
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
    public Collection<StudentId> getStudents(TermId term) {
        return values.keySet().stream()
                .filter(s -> values.get(s)
                .containsKey(term))
                .distinct()
                .collect(Collectors.toSet());
    }

    @JsonbTransient
    public Collection<TermId> getTerms() {
        return values.values().stream().collect(Collector.of(HashSet::new, (s, m) -> s.addAll(m.keySet()), (s1, s2) -> {
            s1.addAll(s2);
            return s1;
        }));
    }

    @JsonbTransient
    public Entry selectEntry(StudentId student, TermId term) {
        return values.getOrDefault(student, (Map<TermId, Entry>) Collections.EMPTY_MAP).get(term);
    }

    @JsonbTransient
    @Override
    public Grade select(StudentId student, TermId term) {
        return Optional.ofNullable(selectEntry(student, term))
                .map(Entry::getGrade)
                .orElse(null);
    }

    @JsonbTransient
    @Override
    public Timestamp timestamp(StudentId student, TermId term) {
        return Optional.ofNullable(selectEntry(student, term))
                .map(Entry::getTimestamp)
                .map(Timestamp::new)
                .orElse(null);
    }

    @JsonbTransient
    @Override
    public Set<TermId> identities() {
        return values.entrySet().stream()
                .flatMap(e -> e.getValue().entrySet().stream())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @JsonbTransient
    @Override
    public Set<StudentId> students() {
        return values.keySet().stream()
                .collect(Collectors.toSet());
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

    @JsonbProperty("subjectAltName")
    public String getAltSubjectName() {
        return subjectAltName;
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
    public void submit(StudentId student, TermId gradeId, Grade grade, org.thespheres.betula.document.Timestamp timestamp) {
        throw new UnsupportedOperationException("Not permitted.");
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

    public static final class Entry {

        public final Grade grade;
        public final java.sql.Timestamp timestamp;

        @JsonbCreator
        public Entry(
                @JsonbProperty("grade") Grade grade,
                @JsonbProperty("timestamp") java.sql.Timestamp timestamp) {
            this.grade = grade;
            this.timestamp = timestamp;
        }

        @JsonbProperty("grade")
        public Grade getGrade() {
            return grade;
        }

        @JsonbProperty("timestamp")
        public java.sql.Timestamp getTimestamp() {
            return timestamp;
        }

    }

}
