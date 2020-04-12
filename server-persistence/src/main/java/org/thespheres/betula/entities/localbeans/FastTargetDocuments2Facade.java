/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.localbeans;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.SessionContext;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.entities.BaseTicketEntity;
import org.thespheres.betula.entities.EmbeddableMarker;
import org.thespheres.betula.entities.GradeTargetAssessmentEntity;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.TermAssessmentEntry2;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.facade.GradeTargetDocumentFacade;
import org.thespheres.betula.entities.facade.TicketFacade;
import org.thespheres.betula.entities.facade.impl.SigneeFacadeImpl;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.services.ws.CommonDocuments;

/**
 *
 * @author boris.heithecker
 */
@RolesAllowed({"signee", "unitadmin"})
@Stateless
public class FastTargetDocuments2Facade {

    @EJB
    private GradeTargetDocumentFacade facade;
    @Resource
    private SessionContext session;
    @EJB
    private SigneeFacadeImpl login;
    @EJB
    private TicketFacade tickets;
    @Inject
    private CommonDocuments cd;

    public String getName() {
        return session.getCallerPrincipal().getName();
    }

    @RolesAllowed({"signee", "unitadmin"})
    public FastTermTargetDocument getFastTermTargetDocument(final DocumentId d) {
        GradeTargetAssessmentEntity e = facade.find(d, LockModeType.OPTIMISTIC);
        if (e != null && e instanceof TermGradeTargetAssessmentEntity) {
            return createFastTermTargetDocument((TermGradeTargetAssessmentEntity) e, login.getCurrent());
        }
        return null;
    }

    public Ticket[] getTickets(DocumentId docId, TermId termId, StudentId studId) {
        return tickets.getTickets(docId, termId, studId, "entitled.signee").stream()
                .map(BaseTicketEntity::getTicket)
                .toArray(Ticket[]::new);
    }

    public boolean submitSingle(DocumentId d, StudentId studId, TermId termId, Grade grade) {
        if (d != null && studId != null && termId != null && grade != null) {
            return facade.submit(d, studId, termId, grade, null);
        }
        throw new IllegalArgumentException("DocumentId, StudentId, TermId, and Grade cannot be null.");
    }

    FastTermTargetDocument createFastTermTargetDocument(TermGradeTargetAssessmentEntity e, SigneeEntity se) {
        final Map<String, Signee> signees = e.getEmbeddableSignees().entrySet().stream()
                .filter(en -> se != null && en.getValue().getSigneeEntity().equals(se))
                .collect(Collectors.toMap(Map.Entry::getKey, me -> me.getValue().getSignee()));
        final Set<TermAssessmentEntry2> ents = e.getEntries();
        Map<StudentId, Map<TermId, FastTermTargetDocument.Entry>> values2 = null;
        try {
            values2 = ents.stream()
                    .collect(Collectors.groupingBy(TermAssessmentEntry2::getStudentId,
                            Collectors.toMap(TermAssessmentEntry2::getGradeId, te -> new FastTermTargetDocument.Entry(te.getGrade(), te.getTimestamp()))));
        } catch (IllegalStateException illex) {
            System.err.println("DOCUMENTID : " + e.getDocumentId().toString());
            System.err.println(illex);
            values2 = Collections.EMPTY_MAP;
        }
        final Set<Marker> markers = e.getEmbeddableMarkers().stream()
                .map(EmbeddableMarker::getMarker)
                .collect(Collectors.toSet());
        final String targetType = e.getTargetType();
        String subjectAltName = null;
        final DocumentId sNames = cd.forName(CommonDocuments.SUBJECT_NAMES_DOCID);
        if (sNames != null) {
            subjectAltName = facade.getStringValue(sNames, e.getDocumentId());
        }
        return new FastTermTargetDocument(e.getDocumentId(), values2, markers, e.getPreferredConvention(), signees, targetType, subjectAltName, e.getExpirationDate());
    }
}
