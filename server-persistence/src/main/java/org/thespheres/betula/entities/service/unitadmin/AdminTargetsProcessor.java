/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.unitadmin;

import java.time.ZonedDateTime;
import org.thespheres.betula.entities.service.*;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.thespheres.betula.assess.GradeFactory;
import org.thespheres.betula.Identity;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.TargetAssessment;
import org.thespheres.betula.services.jms.AbstractDocumentEvent;
import org.thespheres.betula.services.jms.MultiTargetAssessmentEvent;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.Document;
import org.thespheres.betula.document.DocumentEntry;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Documents;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.Template;
import org.thespheres.betula.document.Timestamp;
import org.thespheres.betula.document.util.GenericXmlDocument;
import org.thespheres.betula.document.util.MarkerAdapter;
import org.thespheres.betula.document.util.TargetAssessmentEntry;
import org.thespheres.betula.document.util.XmlDocumentEntry;
import org.thespheres.betula.document.util.XmlMarkerSet;
import org.thespheres.betula.entities.BaseChangeLog;
import org.thespheres.betula.entities.BaseTargetAssessmentEntity;
import org.thespheres.betula.entities.GradeTargetAssessmentEntity;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.SigneeInfoChangeLog;
import org.thespheres.betula.entities.RecordTargetAssessmentEntity;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.VersionChangeLog;
import org.thespheres.betula.entities.facade.GradeTargetDocumentFacade;
import org.thespheres.betula.entities.facade.SigneeFacade;
import org.thespheres.betula.entities.jmsimpl.DocumentsNotificator;
import org.thespheres.betula.niedersachsen.ASVAssessmentConvention;
import org.thespheres.betula.niedersachsen.Ersatzeintrag;
import org.thespheres.betula.services.ws.CommonDocuments;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.Paths;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.UnauthorizedException;
import org.thespheres.betula.util.GradeAdapter;
import org.thespheres.betula.util.GradeEntry;

/**
 *
 * @author boris.heithecker
 */
@Dependent
@Stateless
public class AdminTargetsProcessor extends AbstractAdminContainerProcessor {
    
    @EJB
    private GradeTargetDocumentFacade facade;
    @EJB
    private SigneeFacade signees;
    @PersistenceContext(unitName = "betula0")
    protected EntityManager em;
    @Inject
    protected DocumentsNotificator documentsNotificator;
    @Inject
    private CommonDocuments cd;
    
    private final static Grade PENDING = GradeFactory.find(Ersatzeintrag.NAME, "pending");
    private final static Grade AVC = GradeFactory.find(ASVAssessmentConvention.AV_NAME, "c");
    private final static Grade SVC = GradeFactory.find(ASVAssessmentConvention.SV_NAME, "c");
    
    public AdminTargetsProcessor() {
        super(new String[][]{Paths.UNITS_RECORDS_PATH, Paths.UNITS_TARGETS_PATH, Paths.SIGNEES_TARGET_DOCUMENTS_PATH});
    }
    
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    @Override
    public void process(String[] path, Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException {
        if (Arrays.equals(path, Paths.UNITS_RECORDS_PATH)) {
            processTargetAssessments(template, RecordTargetAssessmentEntity.class);
        } else if (Arrays.equals(path, Paths.UNITS_TARGETS_PATH)) {
            processTargetAssessments(template, TermGradeTargetAssessmentEntity.class);
        } else if (Arrays.equals(path, Paths.SIGNEES_TARGET_DOCUMENTS_PATH)) {
            processSigneesTargetAssessments(template);
        }
    }
    
    private <I extends Identity> void processTargetAssessments(Envelope template, Class<? extends GradeTargetAssessmentEntity<I>> entityClass) throws NotFoundException, SyntaxException, UnauthorizedException {
        if (ServiceUtils.isEntryOf(template, UnitId.class)) {
            final Entry<UnitId, ?> entryNode = ServiceUtils.toEntry(template, UnitId.class);
            final UnitId unit = entryNode.getIdentity();
            for (Template<?> t : template.getChildren()) {
                final DocumentEntry de = ServiceUtils.toEntryType(t, DocumentEntry.class);
                processOneTargetAssessment(de, entityClass, unit);
            }
        } else if (ServiceUtils.isEntryTypeOf(template, DocumentEntry.class)) {
            final DocumentEntry de = ServiceUtils.toEntryType(template, DocumentEntry.class);
            processOneTargetAssessment(de, entityClass, null);
        }
    }
    
    private <I extends Identity> void processOneTargetAssessment(final DocumentEntry de, Class<? extends GradeTargetAssessmentEntity<I>> entityClass, final UnitId unit) throws UnauthorizedException, SyntaxException, NotFoundException {
        final DocumentId docId = de.getIdentity();
        final GenericXmlDocument xmlDoc;
        try {
            xmlDoc = GenericXmlDocument.class.cast(de.getValue());
        } catch (NullPointerException | ClassCastException cce) {
            throw ServiceUtils.createSyntaxException(cce);
        }
        final Action action = de.getAction();
        if (action == null && docId != null) {
            final GradeTargetAssessmentEntity<I> target = facade.find(docId, LockModeType.OPTIMISTIC);
            if (target != null) {
                final TargetAssessmentEntry<I> entry;
                try {
                    entry = (TargetAssessmentEntry<I>) de;
                } catch (ClassCastException e) {
                    throw ServiceUtils.createSyntaxException(e);
                }
                entry.getChildren().stream()
                        .filter(Entry.class::isInstance)
                        .map(Entry.class::cast)
                        .filter(e -> {
                            try {
                                I test = (I) e.getIdentity();
                                return true;
                            } catch (ClassCastException ex) {
                                return false;
                            }
                        })
                        .forEachOrdered(e -> {
                            final I tid = (I) e.getIdentity();
                            if (e.getAction() == null && !e.getChildren().isEmpty()) {
                                e.getChildren().stream()
                                        .filter(Entry.class::isInstance)
                                        .map(Entry.class::cast)
                                        .filter(es -> es.getIdentity() instanceof StudentId)
                                        .forEachOrdered(se -> {
                                            final StudentId sid = (StudentId) se.getIdentity();
                                            if (se.getAction().equals(Action.REQUEST_COMPLETION)) {
                                                final Grade g = target.select(sid, tid);
                                                se.setValue(g != null ? new GradeAdapter(g) : null);
                                                se.setAction(Action.RETURN_COMPLETION);
                                                final java.sql.Timestamp time = target.timestamp(sid, tid);
                                                se.setTimestamp(time != null ? new Timestamp(time) : null);
                                            } else if (se.getAction().equals(Action.FILE) && se.getValue() instanceof GradeAdapter) {
                                                final Entry<StudentId, GradeAdapter> sre = (Entry<StudentId, GradeAdapter>) se;
                                                java.sql.Timestamp ts = sre.getTimestamp() != null ? sre.getTimestamp().getValue() : null;
                                                final ProxyGrade proxy = ProxyGrade.create(sre.getValue());
                                                facade.submit(docId, sid, tid, proxy, ts);
                                                se.setAction(Action.CONFIRM);
                                                final java.sql.Timestamp time = target.timestamp(sid, tid);
                                                se.setTimestamp(time != null ? new Timestamp(time) : null);
                                            } else if (se.getAction().equals(Action.ANNUL)) {
                                                facade.submit(docId, sid, tid, null, null);
                                                se.setAction(Action.CONFIRM);
                                            }
                                        });
                            } else if (e.getAction().equals(Action.REQUEST_COMPLETION)) {
                                e.getChildren().clear();
                                target.students(tid).stream()
                                        .forEachOrdered(s -> {
                                            final Grade g = target.select(s, tid);
                                            final Entry<StudentId, GradeAdapter> ret = new Entry<>(null, s, g != null ? new GradeAdapter(g) : null);
                                            e.getChildren().add(ret);
                                        });
                                e.setAction(Action.RETURN_COMPLETION);
                            }
                        });
            }
        } else if (action.equals(Action.REQUEST_COMPLETION)) {
            final GradeTargetAssessmentEntity<I> target = facade.find(docId, LockModeType.OPTIMISTIC);
            if (target != null) {
                final boolean children = !Boolean.parseBoolean(de.getHints().getOrDefault("request-completion.no-children", "false"));
                final TargetAssessmentEntry<I> entry;
                try {
                    entry = (TargetAssessmentEntry<I>) de;
                } catch (ClassCastException e) {
                    throw ServiceUtils.createSyntaxException(e);
                }
                //TODO: populate Document
//                        entry.setPreferredConvention(con);
                entry.setAction(Action.RETURN_COMPLETION);
                //Markers
                xmlDoc.getMarkerSet().clear();
                Arrays.stream(target.markers())
                        .forEach(m -> xmlDoc.getMarkerSet().add(m));
                //PreferredConvention
                entry.setPreferredConvention(target.getPreferredConvention());
                //Targettype
                entry.setTargetType(target.getTargetType());
                //ExDate
                entry.setDocumentValidity(target.getExpirationDate());
                //signeeInfo
                xmlDoc.getSigneeInfos().clear();
                target.getSignees()
                        .forEach((s, sig) -> xmlDoc.addSigneeInfo(s, sig));
                //Custom subject name
                final DocumentId sNames = cd.forName(CommonDocuments.SUBJECT_NAMES_DOCID);
                if (sNames != null) {
                    final String sn = facade.getStringValue(sNames, docId);
                    entry.setSubjectAlternativeName(sn);
                }
                //Add all entries
                entry.getChildren().clear();
                if (children) {
                    target.getEntries().stream()
                            //TODO: remove, only to fix db
                            //                            .peek(ae -> {
                            //                                final Grade dup = entry.select(ae.getStudentId(), ae.getGradeId());
                            //                                if(dup != null) {
                            //                                    Logger.getLogger("FIX-DATABASE").log(Level.WARNING, "About to submit duplicate entry for {0} {1} in {2}", new Object[]{ae.getStudentId(), ae.getGradeId(), target.getDocumentId()});
                            //                                }
                            //                            })
                            .forEach(ae -> entry.submit(ae.getStudentId(), ae.getGradeId(), ae.getGrade(), new Timestamp(ae.getTimestamp())));
                }
                //Not a fragment returned
                xmlDoc.setFragment(false);
            }
        } else if (action.equals(Action.FILE)) {
            
            @Deprecated
            boolean bulk = ServiceUtils.getBulkProcessValue(de, context);
            
            String hint2 = de.getHints().get("update-pu-links");
            boolean updatePULinks = hint2 != null && hint2.equals("true");
            
            final String keepAfterValue = de.getHints().get("keep.old.target.entries.after");
            Date d = null;
            if (keepAfterValue != null) {
                try {
                    final long l = Long.parseLong(keepAfterValue);
                    if (l > 0) {
                        d = new Date(l);
                    }
                } catch (NumberFormatException e) {
                    Logger.getLogger(AdminTargetsProcessor.class.getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
                }
            }
            final Date keepAfterDate = d;
            
            final Set<StudentId> updatePULinksSet = new HashSet<>();
            GradeTargetAssessmentEntity<I> tae = facade.find(docId, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            
            DocumentId.Version ov = null;
            if (tae == null) {
                if (UnitId.isNull(unit)) {
                    final String msg = "No enclosing unit entry found for non-existing target entity {0}. Creating the target entity with no linked unit entity.";
                    Logger.getLogger(AdminTargetsProcessor.class.getName()).log(Level.INFO, msg, docId);
                }
                tae = facade.create(entityClass, docId, unit);
            } else {
                ov = tae.getCurrentVersion();
            }
            final GradeTargetAssessmentEntity target = tae;
            boolean notifyDocUpdate = false;
            if (xmlDoc != null) {
                //TODO :check access: only unitadmin can initialize document
                if (!xmlDoc.isFragment() && target.markers().length != 0) {
                    ServiceUtils.checkUnitAdmin(context);
                    target.clearMarkerSet();
                    notifyDocUpdate = true;
                }
                //Avoid calling Tags.find....
                for (final MarkerAdapter m : ((XmlMarkerSet) xmlDoc.getMarkerSet()).getAdapterSet()) {
                    final Action markerAction = m.getAction();
                    if (markerAction == null || markerAction.equals(Action.FILE)) {
                        target.addMarker(m.getConvention(), m.getId(), m.getSubset());//TODO: remove marker if file document not fragment
                    } else if (markerAction.equals(Action.ANNUL)) {
                        target.removeMarker(m.getConvention(), m.getId(), m.getSubset());
                    }
                    notifyDocUpdate = true;
                }
                final String pC = xmlDoc.getContentString(TargetAssessment.PROP_PREFERRED_CONVENTION);
                if (!Objects.equals(target.getPreferredConvention(), pC)) {
                    ServiceUtils.checkUnitAdmin(context);
                    notifyDocUpdate = true;
                    if (pC != null) {
                        target.setPreferredConvention(pC);
                    } else if (!xmlDoc.isFragment()) {
                        target.setPreferredConvention(null);
                    }
                }
                final String tType = xmlDoc.getContentString(TargetAssessment.PROP_TARGETTYPE);
                if (!Objects.equals(target.getTargetType(), tType)) {
                    ServiceUtils.checkUnitAdmin(context);
                    notifyDocUpdate = true;
                    if (tType != null) {
                        target.setTargetType(tType);
                    } else if (!xmlDoc.isFragment()) {
                        target.setTargetType(null);
                    }
                }
                //Custom subject name
                final String subjectName = xmlDoc.getContentString(TargetAssessment.PROP_SUBJECT_NAME);
                final DocumentId sNames = cd.forName(CommonDocuments.SUBJECT_NAMES_DOCID);
                if (sNames != null && !Objects.equals(facade.getStringValue(sNames, docId), subjectName)) {
                    ServiceUtils.checkUnitAdmin(context);
                    notifyDocUpdate = true;
                    if (subjectName != null) {
                        facade.setStringValue(sNames, docId, subjectName);
                    } else if (!xmlDoc.isFragment()) {
                        facade.setStringValue(sNames, docId, null);
                    }
                }
                //
                final ZonedDateTime expiry = xmlDoc.getDocumentValidity().getExpirationDate();
                if (!Objects.equals(target.getExpirationDate(), expiry)) {
                    ServiceUtils.checkUnitAdmin(context);
                    notifyDocUpdate = true;
                    if (expiry != null) {
                        target.setExpirationDate(expiry);
                    } else if (!xmlDoc.isFragment()) {
                        target.setExpirationDate(null);
                    }
                }
                for (Map.Entry<String, Document.SigneeInfo> me : xmlDoc.getSigneeInfos().entrySet()) {
                    final SigneeInfoChangeLog log = putSigneeToTAE(target, me.getValue().getSignee(), me.getKey());
                    if (log != null && ov != null) {
                        final DocumentId.Version nv = Documents.inc(ov, 2);
                        target.setCurrentVersion(nv);
                        target.addChangeLog(new VersionChangeLog(target, ov, me.getValue().getTimestamp().getDate()));
                        target.addChangeLog(log);
                        notifyDocUpdate = true;
                    }
                }
                em.merge(target);
            }
            for (Template<?> st : de.getChildren()) {
                I term;
                try {
                    term = (I) ((Entry) st).getIdentity();
                } catch (ClassCastException cce) {
                    throw ServiceUtils.createSyntaxException(cce);
                }

                //better     bulk = st.getAction().equals(Action.FILE);
                if (bulk) {
                    final Map<StudentId, Grade> grades = new HashMap<>();
                    final Map<StudentId, java.sql.Timestamp> timestamps = new HashMap<>();
                    final Set<StudentId> toClear = new HashSet<>();
                    
                    final Map<StudentId, Grade> current = facade.findAll(docId, term);
                    final Map<StudentId, GradeEntry> currentEntries = facade.findAllEntries(docId, term);
                    
                    for (final Template<?> sr : st.getChildren()) {
                        final Entry<StudentId, GradeAdapter> sre = ServiceUtils.toEntry(sr, StudentId.class, GradeAdapter.class);
                        final StudentId sfrid = sre.getIdentity();
                        if (sre.getValue() != null) {
//                            final Grade grade = sre.getValue().getGrade();
                            final ProxyGrade grade = ProxyGrade.create(sre.getValue());
                            final java.sql.Timestamp ts = sre.getTimestamp() != null ? sre.getTimestamp().getValue() : null;
                            grades.put(sfrid, grade);
                            current.remove(sfrid);
                            currentEntries.remove(sfrid);
                            if (ts != null) {
                                timestamps.put(sfrid, ts);
                            }
                        } else if (sre.getAction().equals(Action.ANNUL)) {
                            toClear.add(sfrid);
                            current.remove(sfrid);
                            currentEntries.remove(sfrid);
                        }
                    }
                    if (!current.isEmpty()) {
                        current.forEach((sid, g) -> {
                            if (g.equals(PENDING) || g.equals(AVC) || g.equals(SVC)) {
                                toClear.add(sid);
                            }
                        });
                    }
                    if (!currentEntries.isEmpty() && keepAfterDate != null) {
                        currentEntries.forEach((sid, g) -> {
                            final Timestamp ts = g.getTimestamp();
                            //Also see AppProperties.REPLACE_IF_EQUAL_TIMESTAMP property
                            if (ts == null || !ts.getDate().after(keepAfterDate)) {
//                                        toClear.add(sid);
                            }
//                                    if (g.equals(PENDING) || g.equals(AVC) || g.equals(SVC)) {
//                                        toClear.add(sid);
//                                    }
                        });
                    }
                    if (!toClear.isEmpty()) {
                        facade.clearAll(docId, term, toClear);
                    }
                    if (!grades.isEmpty()) {
                        facade.submitAll(docId, term, grades, timestamps);
                    }
                } else {//Default is to submit single by single
                    for (final Template<?> sr : st.getChildren()) {
                        final Entry<StudentId, GradeAdapter> sre = (Entry<StudentId, GradeAdapter>) sr;
                        final StudentId sfrid = sre.getIdentity();
                        if (sre.getValue() != null) {
//                            final Grade grade = sre.getValue().getGrade();
                            final ProxyGrade grade = ProxyGrade.create(sre.getValue());
                            java.sql.Timestamp ts = sre.getTimestamp() != null ? sre.getTimestamp().getValue() : null;
                            facade.submit(docId, sfrid, term, grade, ts);
                        } else if (sre.getAction().equals(Action.ANNUL)) {
                            facade.submit(docId, sfrid, term, null, null);
                        }
                    }
                }
                if (updatePULinks) {
                    st.getChildren().stream()
                            .map(sr -> (Entry<StudentId, GradeAdapter>) sr)
                            .filter(e -> e.getValue() != null)
                            .map(Entry::getIdentity)
                            .forEach(updatePULinksSet::add);
                }
            }
            if (updatePULinks) {
                final StudentId[] studs = updatePULinksSet.stream()
                        .toArray(StudentId[]::new);
                
                notifyDocUpdate = notifyDocUpdate | facade.linkPrimaryUnits(docId, studs);
            }
            if (notifyDocUpdate) {
                documentsNotificator.notityConsumers(new MultiTargetAssessmentEvent(docId, AbstractDocumentEvent.DocumentEventType.CHANGE, login.getSigneePrincipal(false)));//ZGN
            }
        } else if (action.equals(Action.ANNUL)) {
            final boolean result = facade.remove(docId);
            de.setValue(null);
            if (result) {
                de.setAction(Action.CONFIRM);
            }
        }
    }
    
    private void processSigneesTargetAssessments(final Envelope template) throws NotFoundException, SyntaxException {
        final Entry<Signee, ?> entryNode = ServiceUtils.toEntry(template, Signee.class);
        final Action action = entryNode.getAction();
        final Signee signee;
        if (action != null && action.equals(Action.REQUEST_COMPLETION) && (signee = entryNode.getIdentity()) != null) {
            entryNode.getChildren().clear();
            entryNode.setAction(Action.RETURN_COMPLETION);
            final SigneeEntity se = signees.find(signee);
            if (se != null) {
                facade.findAll(se, TermGradeTargetAssessmentEntity.class, LockModeType.OPTIMISTIC).stream()
                        .map(bte -> createSigneeEntry(bte, signee))
                        .forEach(entryNode.getChildren()::add);
            } else {
                throw ServiceUtils.createNotFoundException(signee);
            }
        }
    }
    
    private DocumentEntry createSigneeEntry(final TermGradeTargetAssessmentEntity bte, final Signee se) {
        final XmlDocumentEntry ret = new XmlDocumentEntry(bte.getDocumentId(), null, true);
        bte.getSignees().entrySet().stream()
                .filter(e -> e.getValue().equals(se))
                .map(e -> e.getKey())
                .forEach(s -> ret.getValue().addSigneeInfo(s, se));
        return ret;
    }
    
    private SigneeInfoChangeLog putSigneeToTAE(final BaseTargetAssessmentEntity tae, final Signee sig, String type) throws NotFoundException {
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
