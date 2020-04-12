/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.unitadmin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.DocumentId.Version;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.ExceptionMessage;
import org.thespheres.betula.document.Template;
import org.thespheres.betula.document.util.DocumentUtilities;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.UnitDocumentEntity;
import org.thespheres.betula.services.ws.BetulaWebService;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.Paths;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.UnauthorizedException;

/**
 *
 * @author boris.heithecker
 */
//@WebService(serviceName = "BetulaAdminService")
@WebService(serviceName = "BetulaService", portName = "BetulaServicePort", targetNamespace = "http://web.service.betula.thespheres.org/")
@Stateless
@DeclareRoles({"unitadmin"})
@RolesAllowed({"unitadmin"})
public class BetulaAdminService implements BetulaWebService {

    @PersistenceContext(unitName = "betula0")
    private EntityManager em;
    @Inject
    private AdminUnitsProcessor unitsProcessor;
    @Inject
    private AdminSigneesProcessor signeesProcessor;
    @Inject
    private AdminTargetsProcessor targetsProcessor;
    @Inject
    private AdminStudentsProcessor studentsProcessor;
    @Inject
    private AdminTextTargetsProcessor textTargetsProcessor;
    @Inject
    private AdminTicketsProcessor ticketsProcessor;
    @Inject
    private AdminReportsProcessor reportsProcessor;
    @Inject
    private AdminDocumentsProcessor documentsProcessor;

    public BetulaAdminService() {
    }

    @WebMethod(operationName = "fetch")
    @WebResult(targetNamespace = "http://www.thespheres.org/xsd/betula/container.xsd")
    public Container fetch(@WebParam(name = "ticket") DocumentId ticket) {
        return new Container();
    }

    @WebMethod(operationName = "solicit")
    @WebResult(targetNamespace = "http://www.thespheres.org/xsd/betula/container.xsd")
    @RolesAllowed({"unitadmin"})
    public Container solicit(@WebParam(name = "container", targetNamespace = "http://www.thespheres.org/xsd/betula/container.xsd") Container container) throws UnauthorizedException, NotFoundException, SyntaxException {
        //Must (!) be processed BEFORE  targetsProcessor !
        for (String[] p : unitsProcessor.getPaths()) {
            final List<Envelope> l = DocumentUtilities.findEnvelope(container, p);
            for (final Envelope t : l) {
                try {
                    unitsProcessor.process(container, p, t);
                } catch (EJBException | PersistenceException e) {
                    ExceptionMessage annot = ExceptionMessage.create(e, e.getLocalizedMessage(), e.getMessage(), LocalDateTime.now());
                    t.setException(annot);
                    Logger.getLogger("betula-admin-service").log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
        
        for (String[] p : signeesProcessor.getPaths()) {
            final List<Envelope> l = DocumentUtilities.findEnvelope(container, p);
            for (final Envelope t : l) {
                try {
                    signeesProcessor.process(p, t);
                    //EJBTransactionRolledbackException problem: This exception is thrown to a remote client....
                } catch (EJBException | PersistenceException e) {
                    ExceptionMessage annot = ExceptionMessage.create(e, e.getLocalizedMessage(), e.getMessage(), LocalDateTime.now());
                    t.setException(annot);
                    Logger.getLogger("betula-admin-service").log(Level.SEVERE, e.getMessage(), e);
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
                    Logger.getLogger("betula-admin-service").log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }

        for (String[] p : textTargetsProcessor.getPaths()) {
            final List<Envelope> l = DocumentUtilities.findEnvelope(container, p);
            for (final Envelope t : l) {
                try {
                    textTargetsProcessor.process(p, t);
                    //EJBTransactionRolledbackException problem: This exception is thrown to a remote client....
                } catch (EJBException | PersistenceException e) {
                    ExceptionMessage annot = ExceptionMessage.create(e, e.getLocalizedMessage(), e.getMessage(), LocalDateTime.now());
                    t.setException(annot);
                    Logger.getLogger("betula-admin-service").log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }

        for (String[] p : studentsProcessor.getPaths()) {
            final List<Envelope> l = DocumentUtilities.findEnvelope(container, p);
            for (final Envelope t : l) {
                try {
                    studentsProcessor.process(p, t);
                } catch (EJBException | PersistenceException e) {
                    ExceptionMessage annot = ExceptionMessage.create(e, e.getLocalizedMessage(), e.getMessage(), LocalDateTime.now());
                    t.setException(annot);
                    Logger.getLogger("betula-admin-service").log(Level.SEVERE, e.getMessage(), e);
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
                    Logger.getLogger("betula-admin-service").log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }

        for (String[] p : reportsProcessor.getPaths()) {
            final List<Envelope> l = DocumentUtilities.findEnvelope(container, p);
            for (final Envelope t : l) {
                try {
                    reportsProcessor.process(p, t);
                    //EJBTransactionRolledbackException problem: This exception is thrown to a remote client....
                } catch (EJBException | PersistenceException e) {
                    ExceptionMessage annot = ExceptionMessage.create(e, e.getLocalizedMessage(), e.getMessage(), LocalDateTime.now());
                    t.setException(annot);
                    Logger.getLogger("betula-admin-service").log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }

        for (String[] p : documentsProcessor.getPaths()) {
            final List<Envelope> l = DocumentUtilities.findEnvelope(container, p);
            for (final Envelope t : l) {
                try {
                    documentsProcessor.process(p, t);
                    //EJBTransactionRolledbackException problem: This exception is thrown to a remote client....
                } catch (EJBException | PersistenceException e) {
                    ExceptionMessage annot = ExceptionMessage.create(e, e.getLocalizedMessage(), e.getMessage(), LocalDateTime.now());
                    t.setException(annot);
                    Logger.getLogger("betula-admin-service").log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }

        processPrimaryUnitsTermgradeDocuments(container);
        return container;
    }

    //gehört wohl in den UnitsProcessor, später
    private void processPrimaryUnitsTermgradeDocuments(Container request) {
        List<Envelope> lt = DocumentUtilities.findEnvelope(request, Paths.PU_TERMGRADES_PATH);
        for (Envelope docNode : lt) {
            DocumentId docId = null;
            if (docNode instanceof Entry && ((Entry) docNode).getIdentity() instanceof UnitId) {
                UnitId unit = (UnitId) ((Entry) docNode).getIdentity();
                docId = new DocumentId(unit.getAuthority(), unit.getId() + "-" + "students", Version.LATEST);
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
