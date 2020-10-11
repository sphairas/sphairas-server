/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.unitadmin;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.Dependent;
import javax.persistence.LockModeType;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.TargetAssessment;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.Document;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Documents;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.Template;
import org.thespheres.betula.document.util.GenericXmlDocument;
import org.thespheres.betula.document.util.MarkerAdapter;
import org.thespheres.betula.document.util.TextAssessmentEntry;
import org.thespheres.betula.document.util.XmlMarkerSet;
import org.thespheres.betula.entities.BaseChangeLog;
import org.thespheres.betula.entities.BaseTargetAssessmentEntity;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.SigneeInfoChangeLog;
import org.thespheres.betula.entities.TermTextTargetAssessmentEntity;
import org.thespheres.betula.entities.VersionChangeLog;
import org.thespheres.betula.entities.facade.SigneeFacade;
import org.thespheres.betula.entities.facade.TextTargetDocumentFacade;
import org.thespheres.betula.entities.service.ServiceUtils;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.Paths;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.UnauthorizedException;

/**
 *
 * @author boris.heithecker
 */
@Dependent
@Stateless
public class AdminTextTargetsProcessor extends AbstractAdminContainerProcessor {

    @EJB
    private SigneeFacade signees;
    @EJB
    private TextTargetDocumentFacade facade;
//    @PersistenceContext(unitName = "betula0")
//    private EntityManager em;

    public AdminTextTargetsProcessor() {
        super(new String[][]{Paths.TEXT_UNITS_TARGETS_PATH, Paths.TEXT_TARGETS_SIGNEES_PATH});
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    @Override
    public void process(String[] path, Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException {
        if (Arrays.equals(path, Paths.TEXT_UNITS_TARGETS_PATH)) {
            processTargetAssessments(template);
        } else if (Arrays.equals(path, Paths.TEXT_TARGETS_SIGNEES_PATH)) {
//            processTargetAssessmentSignees(template);
        }
    }

    private void processTargetAssessments(Envelope template) throws NotFoundException, SyntaxException, UnauthorizedException {
        if (ServiceUtils.isEntryOf(template, UnitId.class)) {
            final Entry<UnitId, ?> entryNode = ServiceUtils.toEntry(template, UnitId.class);
            final UnitId unit = entryNode.getIdentity();
            for (Template<?> t : template.getChildren()) {
                final TextAssessmentEntry de = ServiceUtils.toEntryType(t, TextAssessmentEntry.class);
                processTargetAssessment(de, unit);
            }
        } else if (ServiceUtils.isEntryTypeOf(template, TextAssessmentEntry.class)) {
            final TextAssessmentEntry de = ServiceUtils.toEntryType(template, TextAssessmentEntry.class);
            processTargetAssessment(de, null);
        }
    }

    private void processTargetAssessment(final TextAssessmentEntry entry, final UnitId unit) throws NotFoundException, SyntaxException, UnauthorizedException {
        final DocumentId docId = entry.getIdentity();
        final GenericXmlDocument xmlDoc;
        try {
            xmlDoc = GenericXmlDocument.class.cast(entry.getValue());
        } catch (NullPointerException | ClassCastException cce) {
            throw ServiceUtils.createSyntaxException(cce);
        }
        final Action action = entry.getAction();
        if (action != null) {
            if (action.equals(Action.REQUEST_COMPLETION)) {
                TermTextTargetAssessmentEntity tae = facade.find(docId, LockModeType.OPTIMISTIC);
                if (tae != null) {
                    entry.setAction(Action.RETURN_COMPLETION);
                    entry.getChildren().clear();
                    tae.getEntries().stream().forEach(assess -> {
                        org.thespheres.betula.document.Timestamp ts = assess.getTimestamp() != null ? new org.thespheres.betula.document.Timestamp(assess.getTimestamp()) : null;
                        entry.submit(assess.getStudentId(), assess.getTermId(), assess.getSection(), assess.getText(), ts, null);
                    });
                }
            } else if (action.equals(Action.FILE)) {
                TermTextTargetAssessmentEntity tae = facade.find(docId, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                DocumentId.Version ov = null;
                if (tae == null) {
                    if (UnitId.isNull(unit)) {
                        throw ServiceUtils.createNotFoundException(unit);
                    }
                    tae = facade.create(docId, unit);
                } else {
                    ov = tae.getCurrentVersion();
                }
                final TermTextTargetAssessmentEntity target = tae;
                if (xmlDoc != null) { //Avoid calling Tags.find....
                    ((XmlMarkerSet) xmlDoc.getMarkerSet()).getAdapterSet().stream()
                            .forEach(m -> target.addMarker(m.getConvention(), m.getId(), m.getSubset()));
                    String pC = xmlDoc.getContentString(TargetAssessment.PROP_PREFERRED_CONVENTION);
                    if (pC != null) {
                        target.setPreferredConvention(pC);
                    }
                    String tType = xmlDoc.getContentString(TargetAssessment.PROP_TARGETTYPE);
                    if (tType != null) {
                        target.setTargetType(tType);
                    }
                    ZonedDateTime expiry = xmlDoc.getDocumentValidity().getExpirationDate();
                    if (expiry != null) {
                        target.setExpirationDate(expiry);
                    }
                    for (Map.Entry<String, Document.SigneeInfo> me : xmlDoc.getSigneeInfos().entrySet()) {
                        SigneeInfoChangeLog log = putSigneeToTAE(target, me.getValue().getSignee(), me.getKey());
                        if (log != null && ov != null) {
                            DocumentId.Version nv = Documents.inc(ov, 2);
                            target.setCurrentVersion(nv);
                            target.addChangeLog(new VersionChangeLog(tae, ov, me.getValue().getTimestamp().getDate()));
                            target.addChangeLog(log);
                        }
                    }
                    facade.edit(target);
                }
                if (!entry.getChildren().isEmpty()) {
                    long lock = facade.getLock(docId, 1000);
                    if (lock == -1) {
                        throw ServiceUtils.createUnauthorizedException();
                    }
                    for (final Template<?> te : entry.getChildren()) {
                        final Entry<TermId, ?> termEntry = ServiceUtils.toEntry(te, TermId.class);
                        for (final Template<?> se : termEntry.getChildren()) {
                            if (se.getValue() instanceof MarkerAdapter) {
                                final Marker section = ((MarkerAdapter) se.getValue()).getMarker();
                                for (final Template<?> ses : se.getChildren()) {
                                    final Entry<StudentId, String> sre = ServiceUtils.toEntry(ses, StudentId.class, String.class);
                                    final StudentId sfrid = sre.getIdentity();
                                    if (sre.getValue() != null) {
                                        final String text = sre.getValue();
                                        final java.sql.Timestamp ts = sre.getTimestamp() != null ? sre.getTimestamp().getValue() : null;
                                        facade.submit(docId, sfrid, termEntry.getIdentity(), section, text, ts, lock);
                                    } else if (sre.getAction().equals(Action.ANNUL)) {
                                        facade.submit(docId, sfrid, termEntry.getIdentity(), section, null, null, lock);
                                    }
                                }
                            } else {
                                final Entry<StudentId, String> sre = ServiceUtils.toEntry(se, StudentId.class, String.class);
                                final StudentId sfrid = sre.getIdentity();
                                if (sre.getValue() != null) {
                                    final String text = sre.getValue();
                                    final java.sql.Timestamp ts = sre.getTimestamp() != null ? sre.getTimestamp().getValue() : null;
                                    facade.submit(docId, sfrid, termEntry.getIdentity(), null, text, ts, lock);
                                } else if (sre.getAction().equals(Action.ANNUL)) {
                                    facade.submit(docId, sfrid, termEntry.getIdentity(), null, null, null, lock);
                                }
                            }
                        }
                    }
                    facade.releaseLock(docId, lock);
                }
            }
        }
//        }
    }
//
//    private void processTargetAssessmentSignees(Envelope node) throws NotFoundFault {
////        UnitId unit = (UnitId) entryNode.getIdentity();
//        for (Template<?> t : entryNode.getChildren()) {
//            DocumentId docId = (DocumentId) ((Entry) t).getIdentity();
//            Action action = t.getAction();
//            if (action != null && docId != null) {
//                if (action.equals(Action.FILE)) {
//                    TermTextTargetAssessmentEntity tae = facade.find(docId, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
//                    if (tae == null) {
//                        throw new NotFoundFault();
//                    }
//                    DocumentId.Version ov = tae.getCurrentVersion();
//                    //TODO: check, ob die tae -> unitdocs ein doch mit der unit enthalten, oder gleich andere JPQL suche
//                    signees:
//                    for (Template<?> st : t.getChildren()) {
//                        if (st != null && st instanceof Entry && ((Entry) st).getIdentity() instanceof Signee) {
//                            Signee sig = (Signee) ((Entry) st).getIdentity();
//                            SigneeInfoChangeLog log;
//                            if ((log = putSigneeToTAE(tae, sig, "entitled.signee")) != null) {
//                                if (ov != null) {
//                                    DocumentId.Version nv = Documents.inc(ov, 2);
//                                    tae.setCurrentVersion(nv);
//                                    tae.addChangeLog(new VersionChangeLog(tae, ov, new Date(System.currentTimeMillis())));
//                                    tae.addChangeLog(log);
//                                }
//                                facade.edit(tae);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }

    private SigneeInfoChangeLog putSigneeToTAE(final TermTextTargetAssessmentEntity tae, final Signee sig, String type) throws NotFoundException {
        SigneeEntity se = null;
        if (sig != null && (se = signees.find(sig)) == null) {
            throw ServiceUtils.createNotFoundException(sig);
        }
        SigneeEntity prev = tae.addSigneeInfo(se, type);
        if (Objects.equals(se, prev)) {
            return null;
        }
        BaseChangeLog.Action a;
        if (prev == null) {
            a = BaseChangeLog.Action.ADD;
        } else if (se == null) {
            a = BaseChangeLog.Action.REMOVE;
        } else {
            a = BaseChangeLog.Action.UPDATE;
        }
        return new SigneeInfoChangeLog(tae, BaseTargetAssessmentEntity.BASE_TARGETASSESSMENT_DOCUMENT_SIGNEEINFO, type, prev, a);
    }

}
