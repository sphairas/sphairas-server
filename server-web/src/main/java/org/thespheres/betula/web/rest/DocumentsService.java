package org.thespheres.betula.web.rest;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.Template;
import org.thespheres.betula.document.Timestamp;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.document.util.DocumentUtilities;
import org.thespheres.betula.document.util.GenericXmlDocument;
import org.thespheres.betula.document.util.MarkerAdapter;
import org.thespheres.betula.document.util.TargetAssessmentEntry;
import org.thespheres.betula.document.util.TextAssessmentEntry;
import org.thespheres.betula.document.util.UnitEntry;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;
import org.thespheres.betula.server.beans.clients.ServiceInternalClient;
import org.thespheres.betula.server.beans.clients.UnitsServiceClient;
import org.thespheres.betula.services.ws.Paths;
import org.thespheres.betula.util.CollectionUtil;
import org.thespheres.betula.util.ContainerBuilder;
import org.thespheres.betula.util.GradeAdapter;

/**
 *
 * @author boris.heithecker
 */
@ApplicationScoped
public class DocumentsService {

    private UnitsServiceClient serviceClient;
    @Inject
    private DocumentsModel docModel;

    @PostConstruct
    public void initialize() {
        serviceClient = RestClientBuilder.newBuilder()
                .baseUri(URI.create(ServiceInternalClient.URI_SERVICE_API))
                .hostnameVerifier((hostname, session) -> true) // Optional: specific verifier
                .register(ContainerLegacyJAXBProvider.class)
                .build(UnitsServiceClient.class);
    }

    public UnitsServiceClient getServiceClient() {
        return serviceClient;
    }

    public Set<DocumentId> getDocuments(final Signee signee) {
        final ContainerBuilder builder = new ContainerBuilder();
        final String[] path = Paths.SIGNEES_TARGETS_PATH;
        builder.add(new Entry(Action.REQUEST_COMPLETION, signee), path);
        final Container response = serviceClient.solicit(builder.getContainer());
        final List<Envelope> l = DocumentUtilities.findEnvelope(response, path);
        final Set<DocumentId> ret = l.stream()
                .filter(n -> Entry.class.isAssignableFrom(n.getClass()) && ((Entry<?, ?>) n).getIdentity() instanceof Signee)
                .flatMap(n -> ((Entry<Signee, ?>) n).getChildren().stream())
                .filter(n -> Entry.class.isAssignableFrom(n.getClass()) && ((Entry<?, ?>) n).getIdentity() instanceof DocumentId)
                .map(n -> ((Entry<DocumentId, ?>) n).getIdentity())
                .collect(Collectors.toSet());
        return ret;
    }

    public Set<StudentId> getStudents(final UnitId unit) {
        final ContainerBuilder builder = new ContainerBuilder();
        final String[] path = Paths.UNITS_PARTICIPANTS_PATH;
        final Entry<UnitId, ?> root = new Entry<>(null, unit);
        final UnitEntry ue = new UnitEntry(docModel.convertToUnitDocumentId(unit), unit, Action.REQUEST_COMPLETION, true);
        root.getChildren().add(ue);
        builder.add(root, path);
        final Container response = serviceClient.solicit(builder.getContainer());
        final List<Envelope> l = DocumentUtilities.findEnvelope(response, path);
        final Set<StudentId> ret = l.stream()
                .filter(n -> Entry.class.isAssignableFrom(n.getClass()) && ((Entry<?, ?>) n).getIdentity() instanceof UnitId)
                .flatMap(n -> ((Entry<UnitId, ?>) n).getChildren().stream())
                .filter(n -> UnitEntry.class.isAssignableFrom(n.getClass()))
                .flatMap(n -> ((UnitEntry) n).getChildren().stream())
                .filter(n -> Entry.class.isAssignableFrom(n.getClass()) && ((Entry<?, ?>) n).getIdentity() instanceof StudentId)
                .map(n -> ((Entry<StudentId, ?>) n).getIdentity())
                .collect(Collectors.toSet());
        return ret;
    }

    public Collection<DocumentId> getTargetAssessmentDocuments(UnitId primaryUnit) {
        final ContainerBuilder builder = new ContainerBuilder();
        final String[] path = Paths.UNITS_TARGET_DOCUMENTS_PATH;
        final UnitEntry root = new UnitEntry(docModel.convertToUnitDocumentId(primaryUnit), primaryUnit, Action.REQUEST_COMPLETION, true);
        builder.add(root, path);
        final Container response = serviceClient.solicit(builder.getContainer());
        final List<Envelope> l = DocumentUtilities.findEnvelope(response, path);
        final Set<DocumentId> ret = l.stream()
                .filter(n -> UnitEntry.class.isAssignableFrom(n.getClass()))
                .flatMap(n -> ((UnitEntry) n).getChildren().stream())
                .filter(n -> Entry.class.isAssignableFrom(n.getClass()) && ((Entry<?, ?>) n).getIdentity() instanceof DocumentId)
                .map(n -> ((Entry<DocumentId, ?>) n).getIdentity())
                .collect(Collectors.toSet());
        return ret;
    }

    public FastTermTargetDocument getFastTermTargetDocument(DocumentId id) {
        final ContainerBuilder builder = new ContainerBuilder();
        final String[] path = Paths.UNITS_TARGETS_PATH;
        final TargetAssessmentEntry root = new TargetAssessmentEntry(id, Action.REQUEST_COMPLETION, false);
        builder.add(root, path);
        final Container response = serviceClient.solicit(builder.getContainer());
        final List<Envelope> l = DocumentUtilities.findEnvelope(response, path);
        final TargetAssessmentEntry<TermId> tae = l.stream()
                .filter(n -> TargetAssessmentEntry.class.isAssignableFrom(n.getClass()))
                .map(TargetAssessmentEntry.class::cast)
                .filter(t -> ((TargetAssessmentEntry) t).getIdentity().equals(id))
                .collect(CollectionUtil.requireSingleOrNull());
        return parseFastTermTargetDocument(tae);
    }

    /**
     * Fetches multiple FastTermTargetDocuments in a single round-trip by
     * wrapping all document requests under a UnitId entry, which the server-side
     * TargetsProcessor already handles in batch.
     */
    public Map<DocumentId, FastTermTargetDocument> getFastTermTargetDocuments(final UnitId unit, final Collection<DocumentId> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        final ContainerBuilder builder = new ContainerBuilder();
        final String[] path = Paths.UNITS_TARGETS_PATH;
        final Entry<UnitId, Object> root = new Entry<>(null, unit);
        for (final DocumentId id : ids) {
            root.getChildren().add(new TargetAssessmentEntry(id, Action.REQUEST_COMPLETION, false));
        }
        builder.add(root, path);
        final Container response = serviceClient.solicit(builder.getContainer());
        final List<Envelope> l = DocumentUtilities.findEnvelope(response, path);
        final Map<DocumentId, FastTermTargetDocument> result = new HashMap<>();
        l.stream()
                .filter(n -> Entry.class.isAssignableFrom(n.getClass()) && ((Entry<?, ?>) n).getIdentity() instanceof UnitId)
                .flatMap(n -> ((Entry<UnitId, ?>) n).getChildren().stream())
                .filter(n -> TargetAssessmentEntry.class.isAssignableFrom(n.getClass()))
                .map(n -> (TargetAssessmentEntry<TermId>) n)
                .forEach(tae -> result.put(tae.getIdentity(), parseFastTermTargetDocument(tae)));
        return result;
    }

    private FastTermTargetDocument parseFastTermTargetDocument(final TargetAssessmentEntry<TermId> tae) {
        final Map<StudentId, Map<TermId, FastTermTargetDocument.Entry>> values = new HashMap<>();
        for (Template tc : tae.getChildren()) {
            final TermId tid = ((Entry<TermId, ?>) tc).getIdentity();
            if (tid != null) {
                for (Template ec : tc.getChildren()) {
                    final Entry<StudentId, GradeAdapter> e = (Entry<StudentId, GradeAdapter>) ec;
                    final GradeAdapter v = e.getValue();
                    final Timestamp ts = e.getTimestamp();
                    if (v != null && e.getIdentity() != null) {
                        final FastTermTargetDocument.Entry entry = new FastTermTargetDocument.Entry(v.getGrade(), ts != null ? ts.getValue() : null);
                        values.computeIfAbsent(e.getIdentity(), k -> new HashMap<>()).put(tid, entry);
                    }
                }
            }
        }
        final GenericXmlDocument xmlDoc = (GenericXmlDocument) tae.getValue();
        final Map<String, Signee> s = xmlDoc.getSigneeInfos().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getSignee()));
        return new FastTermTargetDocument(tae.getIdentity(), values, xmlDoc.getMarkerSet(), tae.getPreferredConvention(), s, tae.getTargetType(), tae.getSubjectAlternativeName(), tae.getDocumentValidity());
    }

    public FastTextTermTargetDocument getFastTextTermTargetDocument(DocumentId id) {
        final ContainerBuilder builder = new ContainerBuilder();
        final String[] path = Paths.TEXT_UNITS_TARGETS_PATH;
        final TextAssessmentEntry root = new TextAssessmentEntry(id, Action.REQUEST_COMPLETION, false);
        builder.add(root, path);
        final Container response = serviceClient.solicit(builder.getContainer());
        final List<Envelope> l = DocumentUtilities.findEnvelope(response, path);
        final TextAssessmentEntry tae = l.stream()
                .filter(n -> TextAssessmentEntry.class.isAssignableFrom(n.getClass()))
                .map(TextAssessmentEntry.class::cast)
                .filter(t -> ((TextAssessmentEntry) t).getIdentity().equals(id))
                .collect(CollectionUtil.requireSingleOrNull());
        Map<StudentId, Map<TermId, List<FastTextTermTargetDocument.Entry>>> values = new HashMap<>();
        for (Template tc : tae.getChildren()) {
            final TermId tid = ((Entry<TermId, ?>) tc).getIdentity();
            if (tid != null) {
                for (Template sc : tc.getChildren()) {
                    final MarkerAdapter section = ((Entry<StudentId, MarkerAdapter>) sc).getValue();
                    if (section != null) {
                        for (Template ec : tc.getChildren()) {
                            final Entry<StudentId, String> e = (Entry<StudentId, String>) ec;
                            final String v = e.getValue();
                            final Timestamp ts = e.getTimestamp();
                            if (v != null && e.getIdentity() != null) {
                                final FastTextTermTargetDocument.Entry entry = new FastTextTermTargetDocument.Entry(section.getMarker(), v, ts != null ? ts.getValue() : null);
                                values.computeIfAbsent(e.getIdentity(), k -> new HashMap<>()).computeIfAbsent(tid, k -> new ArrayList<>()).add(entry);
                            }
                        }
                    }
                }
            }
        }
        final GenericXmlDocument xmlDoc = (GenericXmlDocument) tae.getValue();
        final Map<String, Signee> s = xmlDoc.getSigneeInfos().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getSignee()));
        final FastTextTermTargetDocument ret = new FastTextTermTargetDocument(tae.getIdentity(), values, xmlDoc.getMarkerSet(), null, s, tae.getTargetType(), tae.getDocumentValidity());
        return ret;
    }
}
