/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service;

import java.util.List;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.DocumentId.Version;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
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
@WebService(serviceName = "BetulaService", portName = "BetulaServicePort", targetNamespace = "http://web.service.betula.thespheres.org/")
//@UsesJAXBContext
//@LocalBean
@Stateless
@DeclareRoles({"signee"})
@RolesAllowed({"signee"})
public class BetulaService implements BetulaWebService {

//    @Resource(name = "context")
//    private WebServiceContext context;
//    @EJB
//    private LoginBean login;
    @PersistenceContext(unitName = "betula0")
    private EntityManager em;
//    @EJB
//    private DocumentsNotificator documentsNotificator;
//    private Signee signee;
//    @Inject
//    private Instance<ContainerProcessor> processors;
//    @DefaultRoles
//    @Inject
//    private Instance<ContainerProcessor> defaultProcessors;
//    private final String[] PU_PARTICIPANTS_PATH = new String[]{"primary-units", "participants"};
    @Inject
    private UnitsProcessor unitsProcessor;
//    @Inject
//    private AdminSigneesProcessor signeesProcessor;
    @Inject
    private TargetsProcessor targetsProcessor;

    public BetulaService() {
    }

    @WebMethod(operationName = "fetch")
    @WebResult(targetNamespace = "http://www.thespheres.org/xsd/betula/container.xsd")
    @Override
    public Container fetch(@WebParam(name = "ticket") DocumentId ticket) {
        return new Container();
    }

    @WebMethod(operationName = "solicit")
    @WebResult(targetNamespace = "http://www.thespheres.org/xsd/betula/container.xsd")
    @RolesAllowed({"signee"})
    @Override
    public Container solicit(@WebParam(name = "container", targetNamespace = "http://www.thespheres.org/xsd/betula/container.xsd") Container container) throws UnauthorizedException, NotFoundException, SyntaxException {
        //        ContainerBuilder builder = new ContainerBuilder();
//        signee = login.getSigneePrincipal();
//        BindingProvider.
//        MessageContext.
//        JAXWSProperties.
//        context.getMessageContext().
//        if (request != null) {

        unitsProcessor.process(container);
//        signeesProcessor.process(container);

        targetsProcessor.process(container);

//        studentsProcessor.process(container);
//        processUnitsParticipants(request, builder);
//        processUnits(request, builder);
//        processTargets(request, builder);
//        processSignees(container, builder);
//        processStudentsMarkersValue(container, builder);
        if (false) {
            processPrimaryUnitsTermgradeDocuments(container);
        }

//        for (ContainerProcessor cp : processors) {
//            cp.process(container);
//        }
//        return builder.getContainer();
        return container;
    }

    //gehört wohl in den UnitsProcessor, später
    private void processPrimaryUnitsTermgradeDocuments(Container request) throws NotFoundException {
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
