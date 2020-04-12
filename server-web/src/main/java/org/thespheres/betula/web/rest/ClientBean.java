/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.rest;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import org.thespheres.betula.TermId;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.ExceptionMessage;
import org.thespheres.betula.document.util.DocumentUtilities;
import org.thespheres.betula.document.util.TargetAssessmentEntry;
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
@RolesAllowed("signee")
@SessionScoped
public class ClientBean implements Serializable {

    private Set<DocumentId> documents;
    //java:global/Betula_Server/Betula_Persistence/BetulaService!org.thespheres.betula.services.ws.BetulaWebService
    @EJB(beanName = "BetulaService")
    private BetulaWebService service;

    @PostConstruct
    void load() {
        final ContainerBuilder builder = new ContainerBuilder();
        final String[] path = Paths.TARGETS_PATH;
        builder.createTemplate(path, null, Action.REQUEST_COMPLETION);
        try {
            final Container response = service.solicit(builder.getContainer());
            final List<Envelope> l = DocumentUtilities.findEnvelope(response, path);
            documents = l.stream()
                    .filter(n -> Objects.equals(n.getAction(), Action.RETURN_COMPLETION))
                    .flatMap(e -> e.getChildren().stream())
                    .filter(Entry.class::isInstance)
                    .map(Entry.class::cast)
                    .map(Entry::getIdentity)
                    .filter(DocumentId.class::isInstance)
                    .map(DocumentId.class::cast)
                    .collect(Collectors.toSet());
        } catch (NotFoundException | SyntaxException | UnauthorizedException ex) {
        }
    }

    public Set<DocumentId> getDocuments() {
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

}
