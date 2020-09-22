/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.ExceptionMessage;
import org.thespheres.betula.document.Template;
import org.thespheres.betula.document.util.DocumentUtilities;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.UnitDocumentEntity;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.Paths;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.UnauthorizedException;

/**
 *
 * @author boris.heithecker@gmx.net
 */
public abstract class AbstractBetulaService {

    @PersistenceContext(unitName = "betula0")
    protected EntityManager em;
    @Inject
    protected UnitsProcessor unitsProcessor;
    @Inject
    protected TargetsProcessor targetsProcessor;
    @Inject
    protected TicketsProcessor ticketsProcessor;

    protected AbstractBetulaService() {
    }

    public Container fetch(final DocumentId ticket) {
        return new Container();
    }

    protected Container solicit(Container container) throws UnauthorizedException, SyntaxException, NotFoundException {
        for (String[] p : unitsProcessor.getPaths()) {
            final List<Envelope> l = DocumentUtilities.findEnvelope(container, p);
            for (final Envelope t : l) {
                try {
                    unitsProcessor.process(p, t);
                    //EJBTransactionRolledbackException problem: This exception is thrown to a remote client....
                } catch (EJBException | PersistenceException e) {
                    ExceptionMessage annot = ExceptionMessage.create(e, e.getLocalizedMessage(), e.getMessage(), LocalDateTime.now());
                    t.setException(annot);
                    Logger.getLogger("betula-service").log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
        for (String[] p : targetsProcessor.getPaths()) {
            final List<Envelope> l = DocumentUtilities.findEnvelope(container, p);
            for (final Envelope t : l) {
                try {
                    targetsProcessor.process(p, t);
                    //EJBTransactionRolledbackException problem: This exception is thrown to a remote client....
                } catch (EJBException | PersistenceException e) {
                    ExceptionMessage annot = ExceptionMessage.create(e, e.getLocalizedMessage(), e.getMessage(), LocalDateTime.now());
                    t.setException(annot);
                    Logger.getLogger("betula-service").log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
        for (String[] p : ticketsProcessor.getPaths()) {
            final List<Envelope> l = DocumentUtilities.findEnvelope(container, p);
            for (final Envelope t : l) {
                try {
                    ticketsProcessor.process(p, t);
                    //EJBTransactionRolledbackException problem: This exception is thrown to a remote client....
                } catch (EJBException | PersistenceException e) {
                    ExceptionMessage annot = ExceptionMessage.create(e, e.getLocalizedMessage(), e.getMessage(), LocalDateTime.now());
                    t.setException(annot);
                    Logger.getLogger("betula-service").log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
        if (false) {
            processPrimaryUnitsTermgradeDocuments(container);
        }
        return container;
    }

    //gehört wohl in den UnitsProcessor, später
    private void processPrimaryUnitsTermgradeDocuments(Container request) throws NotFoundException {
        List<Envelope> lt = DocumentUtilities.findEnvelope(request, Paths.PU_TERMGRADES_PATH);
        for (Envelope docNode : lt) {
            DocumentId docId = null;
            if (docNode instanceof Entry && ((Entry) docNode).getIdentity() instanceof UnitId) {
                UnitId unit = (UnitId) ((Entry) docNode).getIdentity();
                docId = new DocumentId(unit.getAuthority(), unit.getId() + "-" + "students", DocumentId.Version.LATEST);
            } else if (docNode instanceof Entry && ((Entry) docNode).getIdentity() instanceof DocumentId) {
                docId = (DocumentId) ((Entry) docNode).getIdentity();
            }
            UnitDocumentEntity ude = null;
            if (docId != null) {
                ude = em.find(UnitDocumentEntity.class, docId);
            }
            if (ude != null) {
                boolean udeChange = false;
                for (Template<?> n : docNode.getChildren()) {
                    if (n instanceof Entry && ((Entry) n).getIdentity() instanceof DocumentId) {
                        DocumentId target = (DocumentId) ((Entry) n).getIdentity();
                        TermGradeTargetAssessmentEntity tgtae = em.find(TermGradeTargetAssessmentEntity.class, target);
                        if (tgtae != null) {
                            tgtae.getUnitDocs().add(ude);
                            ude.getTargetAssessments().add(tgtae);
                            em.merge(tgtae);
                            udeChange = true;
                        }
                    }
                }
                if (udeChange) {
                    em.merge(ude);
                }
            }
        }
    }
}
