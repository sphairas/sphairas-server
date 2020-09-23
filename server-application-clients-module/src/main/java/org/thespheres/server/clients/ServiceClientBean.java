/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.ExceptionMessage;
import org.thespheres.betula.document.Template;
import org.thespheres.betula.document.util.DocumentUtilities;
import org.thespheres.betula.document.util.TargetAssessmentEntry;
import org.thespheres.betula.document.util.TicketEntry;
import org.thespheres.betula.services.ws.BetulaWebService;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.Paths;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.UnauthorizedException;
import org.thespheres.betula.util.CollectionUtil;
import org.thespheres.betula.util.ContainerBuilder;

/**
 *
 * @author boris.heithecker
 */
//@RolesPermitted("signee")
@SessionScoped
public class ServiceClientBean implements Serializable {

    private final Map<DocumentId, Set<UnitId>> documents = new HashMap<>();
    @Inject
    private BetulaWebService service;

    @PostConstruct
    void load() {
        final ContainerBuilder builder = new ContainerBuilder();
        final String[] path = Paths.TARGETS_PATH;
        builder.createTemplate(path, null, Action.REQUEST_COMPLETION);
        documents.clear();
        try {
            final Container response = service.solicit(builder.getContainer());
            final List<Envelope> l = DocumentUtilities.findEnvelope(response, path);
//            documents = l.stream()
//                    .filter(n -> Objects.equals(n.getAction(), Action.RETURN_COMPLETION))
//                    .flatMap(e -> e.getChildren().stream())
//                    .filter(Entry.class::isInstance)
//                    .map(Entry.class::cast)
//                    .map(Entry::getIdentity)
//                    .filter(DocumentId.class::isInstance)
//                    .map(DocumentId.class::cast)
//                    .collect(Collectors.toSet());
            //final Map<DocumentId, Set<UnitId>> map = new HashMap<>();
            l.stream()
                    .filter(n -> Objects.equals(n.getAction(), Action.RETURN_COMPLETION))
                    .map(ServiceClientBean::extractMap)
                    .forEach(m -> documents.putAll(m));
        } catch (NotFoundException | SyntaxException | UnauthorizedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Map<DocumentId, Set<UnitId>> extractMap(final Envelope node) {
        return node.getChildren().stream()
                .filter(e -> e instanceof Entry && ((Entry) e).getIdentity() instanceof DocumentId)
                .collect(Collectors.toMap(e -> (DocumentId) ((Entry) e).getIdentity(),
                        e -> e.getChildren().stream()
                                .filter(Entry.class::isInstance)
                                .map(Entry.class::cast)
                                .map(Entry::getIdentity)
                                .filter(UnitId.class::isInstance)
                                .map(UnitId.class::cast)
                                .collect(Collectors.toSet())));
    }

    public Map<DocumentId, Set<UnitId>> getDocuments() {
        return documents;
    }

    public TargetAssessmentEntry<TermId> getDocument(final DocumentId d) throws IOException {
        final ContainerBuilder builder = new ContainerBuilder();
        final String[] path = Paths.UNITS_TARGETS_PATH;
        final TargetAssessmentEntry<TermId> tae = builder.createTargetAssessmentAction(null, d, Paths.UNITS_TARGETS_PATH, null, Action.REQUEST_COMPLETION, true);
        final List<Envelope> l;
        try {
            final Container response = service.solicit(builder.getContainer());
            l = DocumentUtilities.findEnvelope(response, path);
        } catch (NotFoundException | SyntaxException | UnauthorizedException ex) {
            throw new IOException(ex);
        }

        final TargetAssessmentEntry<TermId> ret;
        try {
            ret = l.stream()
                    .filter(TargetAssessmentEntry.class::isInstance)
                    .map(TargetAssessmentEntry.class::cast)
                    .filter(t -> t.getIdentity().equals(d))
                    .collect(CollectionUtil.requireSingleOrNull());
        } catch (Exception e) {
            throw new IOException(e);
        }
        final ExceptionMessage pre = ret.getException();
        if (pre != null) {
            throw new IOException("Logged exception " + pre.getLogMessage());
        }
        return ret;
    }

    public Map<TermId, Map<StudentId, List<TicketEntry>>> getTargetDocumentTickets(final DocumentId d) throws IOException {
        final ContainerBuilder builder = new ContainerBuilder();
        builder.createTemplate(null, d, null, Paths.TARGETS_TICKETS_PATH, null, Action.REQUEST_COMPLETION);
        final List<Envelope> l;
        try {
            final Container response = service.solicit(builder.getContainer());
            l = DocumentUtilities.findEnvelope(response, Paths.TARGETS_TICKETS_PATH);
        } catch (NotFoundException | SyntaxException | UnauthorizedException ex) {
            throw new IOException(ex);
        }
        try {
            return processTicketsResult(l, d);
        } catch (final ClassCastException | IllegalStateException e) {
            throw new IOException(e);
        }
    }

    private Map<TermId, Map<StudentId, List<TicketEntry>>> processTicketsResult(final List<Envelope> l, final DocumentId doc) {
        final Map<TermId, Map<StudentId, List<TicketEntry>>> ret = new HashMap<>();
        final Entry<DocumentId, ?> docEntry = l.stream()
                .filter(Entry.class::isInstance)
                .map(Entry.class::cast)
                .filter(e -> doc.equals(e.getIdentity()))
                .collect(CollectionUtil.requireSingleOrNull());
        for (final Template<?> c : docEntry.getChildren()) {
            final Entry<TermId, ?> ce = (Entry<TermId, ?>) c;
            final TermId tid = ce.getIdentity();
            for (final Template<?> cc : ce.getChildren()) {
                final Entry<StudentId, ?> cce = (Entry<StudentId, ?>) cc;
                final StudentId sid = cce.getIdentity();
                for (final Template<?> ccc : cce.getChildren()) {
                    final TicketEntry te = (TicketEntry) ccc;
                    ret.computeIfAbsent(tid, k -> new HashMap<>())
                            .computeIfAbsent(sid, k -> new ArrayList<>())
                            .add(te);
                }
            }
        }
        return ret;
    }

    public void submitValue(final DocumentId doc, final TermId tid, final StudentId sid, final Grade grade) throws IOException {
        final ContainerBuilder builder = new ContainerBuilder();
        final TargetAssessmentEntry<TermId> tae = builder.createTargetAssessmentAction(null, doc, Paths.UNITS_TARGETS_PATH, null, null, true);
        tae.submit(sid, tid, grade, null);
        final List<Envelope> l;
        try {
            final Container response = service.solicit(builder.getContainer());
            l = DocumentUtilities.findEnvelope(response, Paths.UNITS_TARGETS_PATH);
        } catch (final NotFoundException | SyntaxException | UnauthorizedException ex) {
            throw new IOException(ex);
        }
        final TargetAssessmentEntry<TermId> ret;
        try {
            ret = l.stream()
                    .filter(TargetAssessmentEntry.class::isInstance)
                    .map(TargetAssessmentEntry.class::cast)
                    .filter(t -> t.getIdentity().equals(doc))
                    .collect(CollectionUtil.requireSingleOrNull());
        } catch (final Exception e) {
            throw new IOException(e);
        }
        final ExceptionMessage pre = ret.getException();
        if (pre != null) {
            throw new IOException("Logged exception " + pre.getLogMessage());
        }
    }

}
