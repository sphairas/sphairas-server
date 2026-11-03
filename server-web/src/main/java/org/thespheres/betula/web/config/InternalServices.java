package org.thespheres.betula.web.config;

import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.MarkerFactory;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeFactory;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;
import org.thespheres.betula.server.beans.ReportsBean;
import org.thespheres.betula.server.beans.TermReportDataException;
import org.thespheres.betula.server.beans.clients.BadRequest;
import org.thespheres.betula.server.beans.clients.ReportCustomNote;
import org.thespheres.betula.server.beans.clients.TargetDocumentMarkers;
import org.thespheres.betula.server.beans.AmbiguousDateException;
import org.thespheres.ical.InvalidComponentException;
import org.thespheres.ical.VCard;
import org.thespheres.ical.builder.VCardBuilder;
import org.thespheres.betula.web.ApplicationUser;

/**
 *
 * @author boris.heithecker
 */
@ApplicationScoped
public class InternalServices {

    @Inject
    private AppConfiguration config;
    @Inject
    private ApplicationUser user;

    public Ticket[] findApplicableTickets(DocumentId docId, TermId termId, StudentId studId) {
        final String t = config.getInternalClient().findApplicableTickets(docId, termId, studId);
        return Arrays.stream(t.split("\n"))
                .filter(StringUtils::isNotBlank)
                .map(Ticket::valueOf)
                .toArray(Ticket[]::new);
    }

    public VCard getStudentVCard(StudentId student) {
        try {
            final String c = config.getInternalClient().getStudentVCard(student);
            if (c == null) {
                return null;
            }
            return VCardBuilder.parseCards(new StringReader(c)).getFirst();
        } catch (IOException | ParseException | InvalidComponentException ex) {
            throw new IllegalStateException("Could not parse student vCard returned by the service.", ex);
        }
    }

    public Date getDate(String category, UnitId unit, TermId termId, DocumentId zgn, String moreCategories) throws AmbiguousDateException {
        try {
            return config.getInternalClient().getReportDate(category, unit, termId, zgn, moreCategories);
        } catch (BadRequest ex) {
            throw new IllegalStateException("Invalid report-date request", ex);
        }
    }

    public Marker getMarkerEntry(StudentId id, DocumentId document, Date asOf) {
        final String m = config.getInternalClient().getStudentMarkerEntry(id, document, asOf);
        return StringUtils.isNotBlank(m) ? MarkerFactory.resolve(m.trim()) : null;
    }

    public UnitId findPrimaryUnit(StudentId id, Date asOf) {
        final String u = config.getInternalClient().getStudentPrimaryUnit(id, asOf);
        return StringUtils.isNotBlank(u) ? UnitId.valueOf(u.trim()) : null;
    }

    public String getSubjectAlternativeName(DocumentId d) {
        return config.getInternalClient().getSubjectAlternativeName(d);
    }

    public FastTermTargetDocument getFastTermTargetDocument(final DocumentId document) {
        return config.getInternalClient().getFastTermTargetDocument(document, actingSignee(), actingRoles());
    }

    public FastTextTermTargetDocument getFastTextTermTargetDocument(final DocumentId document) {
        return config.getInternalClient().getFastTextTermTargetDocument(document, actingSignee(), actingRoles());
    }

    public Collection<DocumentId> getTargetAssessmentDocuments(final UnitId unit) {
        final String[] results = config.getInternalClient().getTargetAssessmentDocuments(unit, actingSignee(), actingRoles());
        return results == null ? null : Arrays.stream(results)
                .map(DocumentId::valueOf)
                .toList();
    }

    public Collection<DocumentId> getTargetAssessmentDocumentsForTerm(final UnitId unit, final TermId term) {
        final String[] results = config.getInternalClient().getTargetAssessmentDocumentsForTerm(unit, term, actingSignee(), actingRoles());
        return results == null ? null : Arrays.stream(results)
                .map(DocumentId::valueOf)
                .toList();
    }

    public Collection<DocumentId> getTargetAssessmentDocumentsForTerm(final UnitId unit, final TermId term, final Map<DocumentId, FastTermTargetDocument> documents) {
        final Map<String, FastTermTargetDocument> payload = new HashMap<>();
        documents.forEach((id, document) -> payload.put(id.toString(), document));
        final String[] results = config.getInternalClient().getTargetAssessmentDocumentsForTermWithMap(unit, term, payload, actingSignee(), actingRoles());
        if (results == null) {
            return null;
        }
        final List<DocumentId> ids = Arrays.stream(results).map(DocumentId::valueOf).toList();
        ids.forEach(d -> documents.computeIfAbsent(d, this::getFastTermTargetDocument));
        return ids;
    }

    public Collection<DocumentId> getTextTargetAssessmentDocumentsForTerm(final UnitId unit, final TermId term, final Map<DocumentId, FastTextTermTargetDocument> documents) {
        final Map<String, FastTextTermTargetDocument> payload = new HashMap<>();
        documents.forEach((id, document) -> payload.put(id.toString(), document));
        final String[] results = config.getInternalClient().getTextTargetAssessmentDocumentsForTerm(unit, term, payload, actingSignee(), actingRoles());
        if (results == null) {
            return null;
        }
        final List<DocumentId> ids = Arrays.stream(results).map(DocumentId::valueOf).toList();
        ids.forEach(d -> documents.computeIfAbsent(d, this::getFastTextTermTargetDocument));
        return ids;
    }

    public Collection<Marker> getDocumentMarkers(final DocumentId document) {
        final Marker[] results = config.getInternalClient().getTargetDocumentMarkers(document, actingSignee(), actingRoles());
        return results == null ? null : Arrays.asList(results);
    }

    public Map<DocumentId, Collection<Marker>> getDocumentMarkers(final Collection<DocumentId> documents) {
        if (documents.isEmpty()) {
            return Map.of();
        }
        final String[] payload = documents.stream().map(DocumentId::toString).toArray(String[]::new);
        final TargetDocumentMarkers[] results = config.getInternalClient().getTargetDocumentMarkers(payload, actingSignee(), actingRoles());
        final Map<DocumentId, Collection<Marker>> markers = new HashMap<>();
        for (final TargetDocumentMarkers result : results) {
            markers.put(DocumentId.valueOf(result.document),
                    result.markers.stream().map(MarkerFactory::resolve).toList());
        }
        return markers;
    }

    public Collection<StudentId> getStudents(final UnitId unit, final Date asOf) {
        final String[] results = config.getInternalClient().getTargetDocumentStudents(unit, asOf, actingSignee(), actingRoles());
        return results == null ? null : Arrays.stream(results)
                .map(StudentId::valueOf)
                .toList();
    }

    public Grade findSingle(final StudentId student, final TermId term, final Marker subject, final String suffix) {
        final String value = config.getInternalClient().findSingle(student, term, subject, suffix, actingSignee(), actingRoles());
        return StringUtils.isBlank(value) ? null : GradeFactory.resolve(value.trim());
    }

    public Grade[] findSingleChecked(final UnitId unit, final TermId term, final StudentId student, final Collection<DocumentId> documents) {
        final String[] payload = documents.stream().map(DocumentId::toString).toArray(String[]::new);
        return config.getInternalClient().findSingleChecked(unit, term, student, payload, actingSignee(), actingRoles());
    }

    private Signee actingSignee() {
        final Signee signee = user.getSignee();
        return signee;
    }

    private String actingRoles() {
        return user.getServiceRoleHeader();
    }

    public DocumentId[] findTermReports(final StudentId student, final TermId term, final boolean create) {
        final String[] ids = create
                ? config.getInternalClient().findOrCreateReports(student, term)
                : config.getInternalClient().findReports(student, term);
        return Arrays.stream(ids).map(DocumentId::valueOf).toArray(DocumentId[]::new);
    }

    public StudentId getReportStudent(final DocumentId document) {
        final String student = config.getInternalClient().getReportStudent(document);
        return StringUtils.isBlank(student) ? null : StudentId.valueOf(student.trim());
    }

    public TermId getReportTerm(final DocumentId document) {
        final String term = config.getInternalClient().getReportTerm(document);
        return StringUtils.isBlank(term) ? null : TermId.valueOf(term.trim());
    }

    public String[] getReportAgs(final StudentId student, final TermId term) {
        return config.getInternalClient().getReportAgs(student, term);
    }

    public Marker[] getReportMarkers(final DocumentId document) {
        return Arrays.stream(config.getInternalClient().getReportMarkers(document))
                .map(MarkerFactory::resolve)
                .toArray(Marker[]::new);
    }

    public Map<DocumentId, Marker[]> getReportMarkers(final Collection<DocumentId> documents) {
        if (documents.isEmpty()) {
            return Map.of();
        }
        return Arrays.stream(config.getInternalClient().getReportMarkers(new ArrayList<>(documents)))
                .collect(java.util.stream.Collectors.toMap(
                        result -> DocumentId.valueOf(result.document),
                        result -> result.markers.stream().map(MarkerFactory::resolve).toArray(Marker[]::new)));
    }

    public boolean addReportMarker(final DocumentId document, final Marker marker) {
        return Boolean.parseBoolean(config.getInternalClient().addReportMarker(document, marker.toString()));
    }

    public boolean removeReportMarker(final DocumentId document, final Marker marker) {
        return Boolean.parseBoolean(config.getInternalClient().removeReportMarker(document, marker.toString()));
    }

    public Grade getKopfnote(final DocumentId document, final String convention) {
        final String value = config.getInternalClient().getReportKopfnote(document, convention);
        return StringUtils.isBlank(value) ? null : GradeFactory.resolve(value.trim());
    }

    public boolean setKopfnote(final DocumentId document, final String convention, final Grade grade) throws TermReportDataException {
        final String response = config.getInternalClient().setReportKopfnote(document, convention, grade == null ? null : grade.toString());
        return Boolean.parseBoolean(response);
    }

    public Integer getReportInteger(final DocumentId document, final String type) {
        final String value = config.getInternalClient().getReportInteger(document, type);
        return StringUtils.isBlank(value) ? null : Integer.valueOf(value.trim());
    }

    public boolean setReportInteger(final DocumentId document, final String type, final Integer value) throws TermReportDataException {
        final String response = config.getInternalClient().setReportInteger(document, type, value == null ? null : value.toString());
        return Boolean.parseBoolean(response);
    }

    public String getReportNote(final DocumentId document, final String key) {
        return config.getInternalClient().getReportNote(document, key);
    }

    public Map<String, String> getReportNotes(final DocumentId document, final Collection<String> keys) {
        if (keys.isEmpty()) {
            return Map.of();
        }
        return config.getInternalClient().getReportNotes(document, new ArrayList<>(keys));
    }

    public void setReportNote(final DocumentId document, final String key, final String value) {
        try (final Response response = config.getInternalClient().setReportNote(document, key, value)) {
            requireSuccess(response);
        }
    }

    public ReportsBean.CustomNote[] getReportCustomNotes(final DocumentId document) {
        return Arrays.stream(config.getInternalClient().getReportCustomNotes(document))
                .map(note -> new ReportsBean.CustomNote(note.position, note.value))
                .toArray(ReportsBean.CustomNote[]::new);
    }

    public void setReportCustomNotes(final DocumentId document, final ReportsBean.CustomNote[] notes) {
        final ReportCustomNote[] request = Arrays.stream(notes)
                .map(note -> new ReportCustomNote(note.getPosition(), note.getValue()))
                .toArray(ReportCustomNote[]::new);
        try (final Response response = config.getInternalClient().setReportCustomNotes(document, request)) {
            requireSuccess(response);
        }
    }

    private static void requireSuccess(final Response response) {
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            throw new jakarta.ws.rs.WebApplicationException(response);
        }
    }

}
