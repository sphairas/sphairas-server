/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.openide.util.NbBundle;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.DocumentEntry;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.Template;
import org.thespheres.betula.document.Timestamp;
import org.thespheres.betula.document.util.GenericXmlDocument;
import org.thespheres.betula.document.util.TargetAssessmentEntry;
import org.thespheres.betula.entities.localbeans.FastTargetDocuments2Facade;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.Paths;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.UnauthorizedException;
import org.thespheres.betula.services.ws.UnauthorizedFault;
import org.thespheres.betula.util.GradeAdapter;

/**
 *
 * @author boris.heithecker
 */
//@Dependent
@Stateless
public class TargetsProcessor extends AbstractContainerProcessor {

    @EJB//Injet doesnt work --> no session
    private FastTargetDocuments2Facade targets;

    public TargetsProcessor() {
        super(new String[][]{Paths.UNITS_TARGETS_PATH}); //Paths.UNITS_RECORDS_PATH, Paths.SIGNEES_TARGET_DOCUMENTS_PATH
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    @Override
    public void process(final String[] path, final Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException {
        if (ServiceUtils.isEntryOf(template, UnitId.class)) {
            final Entry<UnitId, ?> entryNode = ServiceUtils.toEntry(template, UnitId.class);
            final UnitId unit = entryNode.getIdentity();
            for (final Template<?> t : template.getChildren()) {
                final DocumentEntry de = ServiceUtils.toEntryType(t, DocumentEntry.class);
                processTargetAssessment(de);
            }
        } else if (ServiceUtils.isEntryTypeOf(template, DocumentEntry.class)) {
            final DocumentEntry de = ServiceUtils.toEntryType(template, DocumentEntry.class);
            processTargetAssessment(de);
        }
    }

    @NbBundle.Messages("TargetsProcessor.processTargetAssessment.noTickets=Keine Berechtigungen/Tickets")
    private void processTargetAssessment(final DocumentEntry de) throws NotFoundException, SyntaxException, UnauthorizedException {
        final DocumentId docId = de.getIdentity();
        final GenericXmlDocument xmlDoc;
        try {
            xmlDoc = GenericXmlDocument.class.cast(de.getValue());
        } catch (NullPointerException | ClassCastException cce) {
            throw ServiceUtils.createSyntaxException(cce);
        }
        final Action action = de.getAction();
        if (Objects.equals(action, Action.REQUEST_COMPLETION)) {
            final FastTermTargetDocument fttd = targets.getFastTermTargetDocument(docId);
            final TargetAssessmentEntry<TermId> entry2 = (TargetAssessmentEntry<TermId>) de;
            entry2.setAction(Action.RETURN_COMPLETION);
            entry2.setTime(ZonedDateTime.now());
            //Markers
            xmlDoc.getMarkerSet().clear();
            Arrays.stream(fttd.markers())
                    .forEach(m -> xmlDoc.getMarkerSet().add(m));
            //PreferredConvention
            entry2.setPreferredConvention(fttd.getPreferredConvention());
            //TT
            entry2.setTargetType(fttd.getTargetType());
            //ExDate
            entry2.setDocumentValidity(fttd.getExpirationDate());
            //TODO: Signeeinfo if admin
            entry2.getChildren().clear();

            fttd.getTerms().stream()
                    .forEach(tid -> {
                        fttd.getStudents(tid).stream()
                                .forEach(sid -> {
                                    final FastTermTargetDocument.Entry e = fttd.selectEntry(sid, tid);
                                    if (e != null) {
                                        entry2.submit(sid, tid, e.grade, new Timestamp(e.timestamp));
                                    }
                                });
                    });
        } else if (action == null) {
            for (final Template<?> st : de.getChildren()) {
                final TermId gradeId;
                try {
                    gradeId = (TermId) ((Entry) st).getIdentity();
                } catch (final ClassCastException cce) {
                    throw ServiceUtils.createSyntaxException(cce);
                }//Default is to submit single by single
                for (final Template<?> sr : st.getChildren()) {
                    final Entry<StudentId, GradeAdapter> sre;
                    try {
                        sre = (Entry<StudentId, GradeAdapter>) sr;
                    } catch (final ClassCastException cce) {
                        throw ServiceUtils.createSyntaxException(cce);
                    }
                    if (!(Action.FILE.equals(sre.getAction()) || Action.ANNUL.equals(sre.getAction()))) {
                        continue;
                    }
                    final StudentId sfrid = sre.getIdentity();
                    final Grade grade = sre.getValue().getGrade();
                    final TermId tid = gradeId;
                    final Ticket[] tickets = targets.getTickets(docId, tid, sfrid);
                    if (tickets.length != 0) {
                        if (sre.getValue() != null && sre.getAction().equals(Action.FILE)) {
                            targets.submitSingle(docId, sfrid, tid, grade);
                        } else if (sre.getAction().equals(Action.ANNUL)) {
                            targets.submitSingle(docId, sfrid, gradeId, null);
                        }
                        sre.setAction(Action.CONFIRM);
                    } else {
                        final UnauthorizedFault fault = new UnauthorizedFault();
                        final String message = NbBundle.getMessage(TargetsProcessor.class, "TargetsProcessor.processTargetAssessment.noTickets");
                        fault.setMessage(message);
                        throw new UnauthorizedException(message, fault);
                    }
                }
            }
        }
    }
//        }
}
