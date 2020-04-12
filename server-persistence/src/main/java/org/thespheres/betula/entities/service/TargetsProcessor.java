/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service;

import java.time.ZonedDateTime;
import java.util.Arrays;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.Container;
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
    protected void process(Container container, String[] path, Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException {
        Entry<UnitId, ?> entry = toEntry(template, UnitId.class);
        if (Arrays.equals(path, Paths.UNITS_RECORDS_PATH)) {
//            processTargetAssessment(container, entry, RecordTargetAssessmentEntity.class);
        } else if (Arrays.equals(path, Paths.UNITS_TARGETS_PATH)) {
            processTargetAssessment(container, entry);
        } else if (Arrays.equals(path, Paths.SIGNEES_TARGET_DOCUMENTS_PATH)) {
//            processTargetAssessmentSignees(entry);
        }
    }

    private void processTargetAssessment(Container request, Entry<UnitId, ?> entryNode) throws NotFoundException, SyntaxException, UnauthorizedException {
        UnitId unit = entryNode.getIdentity();
        for (Template<?> t : entryNode.getChildren()) {
            DocumentEntry de = toEntryType(t, DocumentEntry.class);
            DocumentId docId = de.getIdentity();
            final GenericXmlDocument xmlDoc;
            try {
                xmlDoc = GenericXmlDocument.class.cast(de.getValue());
            } catch (NullPointerException | ClassCastException cce) {
                throw ServiceUtils.createSyntaxException(cce);
            }
            Action action = de.getAction();
            if (action != null && unit != null) {
                if (action.equals(Action.REQUEST_COMPLETION)) {
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
                } else if (action.equals(Action.FILE)) {
                    for (Template<?> st : de.getChildren()) {
                        TermId gradeId;
                        try {
                            gradeId = (TermId) ((Entry) st).getIdentity();
                        } catch (ClassCastException cce) {
                            throw ServiceUtils.createSyntaxException(cce);
                        }
                        {//Default is to submit single by single
                            st.getChildren().stream().forEach(sr -> {
                                final Entry<StudentId, GradeAdapter> sre = (Entry<StudentId, GradeAdapter>) sr;
                                final StudentId sfrid = sre.getIdentity();
                                if (sre.getValue() != null) {
                                    Grade grade = sre.getValue().getGrade();
                                    final TermId tid = gradeId;
                                    Ticket[] tickets = targets.getTickets(docId, tid, sfrid);
                                    if (tickets.length != 0) {
                                        targets.submitSingle(docId, sfrid, tid, grade);
                                    }
                                } else if (sre.getAction().equals(Action.ANNUL)) {
//                                    facade.submit(docId, sfrid, gradeId, null, null);
//                                    targets.submitSingle(docId, sfrid, (TermId) gradeId, null);
                                }
                            });
                        }
                    }
                }
            }
        }
//        }
    }

}
