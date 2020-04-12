/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.localbeans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.NoSuchEntityException;
import javax.ejb.PostActivate;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.entities.BaseDocumentEntity;
import org.thespheres.betula.entities.BaseTargetAssessmentEntity;
import org.thespheres.betula.entities.BaseTicketEntity;
import org.thespheres.betula.entities.EmbeddableMarker;
import org.thespheres.betula.entities.EmbeddableSigneeInfo;
import org.thespheres.betula.entities.GradeTargetAssessmentEntity;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.TermAssessmentEntry2;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.TermTextAssessmentEntry2;
import org.thespheres.betula.entities.TermTextTargetAssessmentEntity;
import org.thespheres.betula.entities.UnitDocumentEntity;
import org.thespheres.betula.entities.config.AppProperties;
import org.thespheres.betula.entities.facade.GradeTargetDocumentFacade;
import org.thespheres.betula.entities.facade.TextTargetDocumentFacade;
import org.thespheres.betula.entities.facade.TicketFacade;
import org.thespheres.betula.entities.facade.UnitDocumentFacade;
import org.thespheres.betula.entities.facade.impl.SigneeFacadeImpl;
import org.thespheres.betula.entities.messaging.ChannelsLocalImpl;
import org.thespheres.betula.entities.saccess.SigneeEJBAccessException;
import org.thespheres.betula.server.beans.FastTargetDocuments2;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;
import org.thespheres.betula.server.beans.JoinedUnitsEntry;
import org.thespheres.betula.services.ws.CommonDocuments;
import org.thespheres.betula.util.CollectionUtil;

/**
 *
 * @author boris.heithecker
 */
@RolesAllowed({"signee", "unitadmin"})
public class FastTargetDocuments2Impl implements FastTargetDocuments2, Serializable {

    @EJB
    protected GradeTargetDocumentFacade facade;
    @EJB
    protected UnitDocumentFacade unitfacade;
    @EJB
    protected SigneeFacadeImpl login;
    @EJB
    protected TicketFacade tickets;
    @EJB
    protected TextTargetDocumentFacade textFacade;
    @EJB
    protected ChannelsLocalImpl channels;
    protected transient Set<DocumentId> targets;
    protected transient List<TermGradeTargetAssessmentEntity> resultList; //transient: not serializable named query result list
    protected final Map<UnitId, Set<DocumentId>> unitTargetDocs = new HashMap<>();
    protected final Map<UnitId, Map<TermId, Set<DocumentId>>> unitTermTargetDocs = new HashMap<>();
    protected UnitId[] primaryUnit;
    protected final Map<DocumentId, JoinedUnitsEntry> joined = new HashMap<>();
    @EJB
    FastTargetDocuments2Facade security;
    @Default
    @Inject
    protected transient DocumentsModel docModel;
    @Inject
    protected CommonDocuments cd;

    @PostConstruct
    public void logConstruct() {
        String sig = security.getName();
        String mess = resultList == null ? "null" : "size: " + resultList.size();
        Logger.getLogger(FastTargetDocuments2Impl.class.getName()).log(Level.INFO, "Constructed FastTargetDocuments2Impl for {0}; \"resultList\" is {1}.", new String[]{sig, mess});
    }

    @PostActivate
    public void logActivate() {
        String sig = security.getName();
        String mess = resultList == null ? "null" : "size: " + resultList.size();
        Logger.getLogger(FastTargetDocuments2Impl.class.getName()).log(Level.INFO, "Activated FastTargetDocuments2Impl for {0}; \"resultList\" is {1}.", new String[]{sig, mess});
    }

    //facade.findAll is an implicit security check
    protected List<TermGradeTargetAssessmentEntity> getTargetAssessmentDocumentResultList() {
        if (resultList == null) {
            final long start = System.currentTimeMillis();
            //this is a live list, returned form cached named query...
            final SigneeEntity se = login.getCurrent();
            if (se != null) {
                resultList = facade.findAll(se, TermGradeTargetAssessmentEntity.class, LockModeType.OPTIMISTIC);
                Logger.getLogger(FastTargetDocuments2Impl.class.getName()).log(Level.INFO, "Created List<TermGradeTargetAssessmentEntity>2 for {0}", se.getSignee().getId());
            } else {
                resultList = Collections.EMPTY_LIST;
            }
            String sig = security.getName();
            Logger.getLogger(FastTargetDocuments2Impl.class.getName()).log(Level.INFO, "Loaded \"resultList\"  in FastTargetDocuments2Impl for {0} in {1} ms.", new String[]{sig, Long.toString(System.currentTimeMillis() - start)});
        }
        return resultList;
    }

    protected Set<DocumentId> getSigneeTargetAssessmentDocumentIds() {
        if (targets == null || resultList == null) {
            //TODO: check if targets transient suffices
            targets = getTargetAssessmentDocumentResultList().stream().map(TermGradeTargetAssessmentEntity::getDocumentId).collect(Collectors.toSet());
        }
        return targets;
    }

    @Override
    public Collection<DocumentId> getTargetAssessmentDocuments() {
        return getSigneeTargetAssessmentDocumentIds();
    }

    @Override
    public Collection<StudentId> getStudents() {
        return getTargetAssessmentDocumentResultList().stream().flatMap((TermGradeTargetAssessmentEntity tgtae) -> tgtae.students(null).stream()).distinct().collect(Collectors.toSet());
    }

    @RolesAllowed(value = {"signee", "unitadmin"})
    @Override
    public FastTermTargetDocument getFastTermTargetDocument(final DocumentId d) {
        if (getSigneeTargetAssessmentDocumentIds().contains(d)) {
            final TermGradeTargetAssessmentEntity s = getTargetAssessmentDocumentResultList().stream().filter((TermGradeTargetAssessmentEntity tgtae) -> tgtae.getDocumentId().equals(d)).collect(CollectionUtil.singleOrNull());
            if (s == null) {
                //Should not happen!
                Logger.getLogger(FastTargetDocuments2Impl.class.getName()).log(Level.INFO, "{0} was present in  \"targets\", but not in \"resultList\".", d);
                return loadCheckedTargetDocument(d);
            }
            final long start = System.currentTimeMillis();
            final FastTermTargetDocument ret = createFastTermTargetDocument(s, login.getCurrent());
            final String sig = security.getName();
            Logger.getLogger(FastTargetDocuments2Impl.class.getName()).log(Level.INFO, "Created FastTermTargetDocument {0} in FastTargetDocuments2Impl for {1} in {2} ms.", new String[]{d.getId(), sig, Long.toString(System.currentTimeMillis() - start)});
            return ret;
        } else {
            checkDocumentAccess(d);
            return loadCheckedTargetDocument(d);
        }
    }

    //No further access check
    protected FastTermTargetDocument loadCheckedTargetDocument(DocumentId d) {
        GradeTargetAssessmentEntity e = facade.find(d, LockModeType.OPTIMISTIC);
        if (e != null && e instanceof TermGradeTargetAssessmentEntity) {
            return createFastTermTargetDocument((TermGradeTargetAssessmentEntity) e, login.getCurrent());
        }
        return null;
    }

    protected void checkDocumentAccess(final DocumentId did) {
        //        if (!session.isCallerInRole("unitadmin")
        //                && !(getPrimaryUnit() != null
        //                && getTargetAssessmentDocuments(getPrimaryUnit()).contains(did))) {
        //            session.setRollbackOnly();
        //            throw new SigneeEJBAccessException(did, session.getCallerPrincipal());
        //        }
    }

    FastTermTargetDocument createFastTermTargetDocument(final TermGradeTargetAssessmentEntity e, final SigneeEntity se) {
        final Map<String, Signee> signees = e.getEmbeddableSignees().entrySet().stream().filter((Map.Entry<String, EmbeddableSigneeInfo> en) -> se != null && en.getValue().getSigneeEntity().equals(se)).collect(Collectors.toMap(Map.Entry::getKey, (Map.Entry<String, EmbeddableSigneeInfo> me) -> me.getValue().getSignee()));
        final Set<TermAssessmentEntry2> ents = e.getEntries();
        Map<StudentId, Map<TermId, FastTermTargetDocument.Entry>> values2 = null;
        try {
            values2 = ents.stream().collect(Collectors.groupingBy(TermAssessmentEntry2::getStudentId, Collectors.toMap(TermAssessmentEntry2::getGradeId, te -> new FastTermTargetDocument.Entry(te.getGrade(), te.getTimestamp()))));
        } catch (IllegalStateException illex) {
            System.err.println("DOCUMENTID : " + e.getDocumentId().toString());
            System.err.println(illex);
            values2 = Collections.EMPTY_MAP;
        }
        final Set<Marker> markers = e.getEmbeddableMarkers().stream().map(EmbeddableMarker::getMarker).collect(Collectors.toSet());
        final String targetType = e.getTargetType();
        String subjectAltName = null;
        final DocumentId sNames = cd.forName(CommonDocuments.SUBJECT_NAMES_DOCID);
        if (sNames != null) {
            subjectAltName = facade.getStringValue(sNames, e.getDocumentId());
        }
        return new FastTermTargetDocument(e.getDocumentId(), values2, markers, e.getPreferredConvention(), signees, targetType, subjectAltName, e.getExpirationDate());
    }

    @RolesAllowed(value = {"signee", "unitadmin"})
    @Override
    public FastTextTermTargetDocument getFastTextTermTargetDocument(DocumentId d) {
        TermTextTargetAssessmentEntity e = textFacade.find(d, LockModeType.OPTIMISTIC);
        if (e != null) {
            final Set<TermTextAssessmentEntry2> ents = e.getEntries();
            final Map<StudentId, Map<TermId, List<FastTextTermTargetDocument.Entry>>> values2 = ents.stream().collect(Collectors.groupingBy(TermTextAssessmentEntry2::getStudentId, Collectors.groupingBy(TermTextAssessmentEntry2::getTermId, Collectors.mapping((TermTextAssessmentEntry2 te) -> new FastTextTermTargetDocument.Entry(te.getSection(), te.getText(), te.getTimestamp()), Collectors.toList()))));
            final Set<Marker> markers = e.getEmbeddableMarkers().stream().map(EmbeddableMarker::getMarker).collect(Collectors.toSet());
            return new FastTextTermTargetDocument(e.getDocumentId(), values2, markers, e.getPreferredConvention(), e.getSignees(), e.getTargetType(), e.getExpirationDate());
        }
        return null;
    }

    @RolesAllowed(value = {"signee", "unitadmin"})
    @Override
    public Collection<Marker> getDocumentMarkers(DocumentId d) {
        //TODO: securityCheck
        BaseDocumentEntity e = facade.findBaseDocumentEntity(d, LockModeType.OPTIMISTIC);
        if (e != null) {
            return e.getEmbeddableMarkers().stream().map(EmbeddableMarker::getMarker).collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public Grade selectSingle(DocumentId d, StudentId student, TermId term) {
        GradeTargetAssessmentEntity e = facade.find(d, LockModeType.OPTIMISTIC);
        if (e != null) {
            return e.select(student, term);
        }
        return null;
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    @Override
    public boolean submitSingle(DocumentId d, StudentId studId, TermId termId, Grade grade) {
        if (d != null && studId != null && termId != null && grade != null) {
            return facade.submit(d, studId, termId, grade, null);
        }
        throw new IllegalArgumentException("DocumentId, StudentId, TermId, and Grade cannot be null.");
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    @Override
    public Collection<DocumentId> getTargetAssessmentDocumentsForTerm(final UnitId unit, final TermId term) {
        //        checkUnitAccess(unit);
        return unitTermTargetDocs.computeIfAbsent(unit, (UnitId u) -> new HashMap<>()).computeIfAbsent(term, (TermId t) -> fetchTargetAssessmentDocumentsForTerm(unit, term));
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    @RolesAllowed(value = {"signee", "unitadmin"})
    @Override
    public Collection<DocumentId> getTargetAssessmentDocumentsForTerm(final UnitId unit, final TermId term, final Map<DocumentId, FastTermTargetDocument> map) {
        final Collection<DocumentId> coll = getTargetAssessmentDocumentsForTerm(unit, term);
        return coll.stream().peek((DocumentId d) -> {
            if (map != null) {
                map.computeIfAbsent(d, this::getFastTermTargetDocument);
            }
        }).collect(Collectors.toSet());
    }

    protected Set<DocumentId> fetchTargetAssessmentDocumentsForTerm(UnitId unit, TermId term) {
        DocumentId docId = docModel.convertToUnitDocumentId(unit);
        UnitDocumentEntity ude = unitfacade.find(docId, LockModeType.OPTIMISTIC);
        if (ude == null) {
            throw new NoSuchEntityException();
        }
        return facade.findForUnitDocument(ude, term).stream().map(TermGradeTargetAssessmentEntity::getDocumentId).collect(Collectors.toSet());
    }

    @RolesAllowed(value = {"signee", "unitadmin"})
    @Override
    public Collection<DocumentId> getTargetAssessmentDocuments(UnitId unit) {
        //        checkUnitAccess(unit);
        return unitTargetDocs.computeIfAbsent(unit, this::fetchPrimaryUnitTargetDocuments);
    }

    protected Set<DocumentId> fetchPrimaryUnitTargetDocuments(UnitId unit) {
        //        DocumentId docId = ContainerBuilder.findUnitDocumentId(unit);
        final DocumentId docId = docModel.convertToUnitDocumentId(unit);
        final UnitDocumentEntity ude = unitfacade.find(docId, LockModeType.OPTIMISTIC);
        if (ude == null) {
            throw new NoSuchEntityException();
        }
        final boolean useLinked = Boolean.getBoolean(AppProperties.WEB_USE_LINKED_PU_LISTS);
        final Set<DocumentId> ret;
        if (!useLinked) {
            //findTermGradeTargetAssessmentsForUnitEntityStudents
            ret = facade.findForUnitDocument(ude, null).stream().map(BaseTargetAssessmentEntity::getDocumentId).collect(Collectors.toSet());
        } else {
            ret = ude.getTargetAssessments().stream().map(BaseTargetAssessmentEntity::getDocumentId).collect(Collectors.toSet());
        }
        if (true) {
            textFacade.findForPrimaryUnit(unit, LockModeType.OPTIMISTIC).stream().map(BaseTargetAssessmentEntity::getDocumentId).forEach(ret::add);
        }
        return ret;
    }

    @Override
    public Ticket[] getTickets(DocumentId docId, TermId termId, StudentId studId) {
        return tickets.getTickets(docId, termId, studId, "entitled.signee").stream().map(BaseTicketEntity::getTicket).toArray(Ticket[]::new);
    }

    @RolesAllowed(value = {"signee", "unitadmin"})
    @Override
    public Collection<StudentId> getStudents(UnitId unit, Date asOf) {
        //        DocumentId docId = ContainerBuilder.findUnitDocumentId(unit);
        DocumentId docId = docModel.convertToUnitDocumentId(unit);
        UnitDocumentEntity ude = unitfacade.find(docId, LockModeType.OPTIMISTIC);
        if (ude != null) {
            Set<StudentId> ret = ude.getStudentIds().stream().collect(Collectors.toSet());
            if (asOf != null) {
                unitfacade.adoptStudentsToVersionAsOf(ude, asOf, ret);
            }
            return ret;
        }
        return null;
    }

    @Override
    public Collection<StudentId> getPrimaryUnitStudents() {
        UnitId uid = getPrimaryUnit();
        if (uid != null) {
            return getStudents(uid, null);
        } else {
            return null;
        }
    }

    @Override
    public UnitId getPrimaryUnit() {
        if (primaryUnit == null) {
            final SigneeEntity se = login.getCurrent();
            if (se != null) {
                final DocumentId ct = cd.forName(CommonDocuments.PRIMARY_UNIT_HEAD_TEACHERS_DOCID);
                primaryUnit = ct != null ? new UnitId[]{unitfacade.getPrimaryUnit(ct, se.getSignee())} : new UnitId[1];
            } else {
                primaryUnit = new UnitId[1];
            }
        }
        return primaryUnit[0];
    }

    @Override
    public Grade findSingle(StudentId student, TermId term, Marker fach, String suffix) {
        final DocumentId[] d = facade.findDocument(student, term, fach);
        DocumentId[] md = Arrays.stream(d).filter((DocumentId did) -> did.getId().endsWith("-" + suffix)).toArray(DocumentId[]::new);
        if (md.length == 1) {
            return selectSingle(md[0], student, term);
        }
        return null;
    }

    @Override
    public Grade[] findSingleChecked(UnitId unit, TermId term, StudentId student, Collection<DocumentId> selectFrom) {
        checkUnitAccess(unit);
        return //Second security check
                selectFrom.stream().filter((DocumentId did) -> getTargetAssessmentDocumentsForTerm(unit, term).contains(did)) //Second security check
                        .map((DocumentId did) -> facade.find(did, LockModeType.OPTIMISTIC)).filter(TermGradeTargetAssessmentEntity.class::isInstance).map(TermGradeTargetAssessmentEntity.class::cast).map((TermGradeTargetAssessmentEntity tgtae) -> tgtae.select(student, term)).filter(Objects::nonNull).toArray(Grade[]::new);
    }

    protected void checkUnitAccess(UnitId unit) throws IllegalStateException, SigneeEJBAccessException {
        //        if (!session.isCallerInRole("unitadmin") && !Objects.equals(unit, getPrimaryUnit())) { //TODO: remove, implemented in unitfacade
        //            session.setRollbackOnly();
        //            throw new SigneeEJBAccessException(unit, session.getCallerPrincipal());
        //        }
    }

    @Override
    public StudentId[] getIntersection(UnitId unit) {
        DocumentId docId = docModel.convertToUnitDocumentId(unit);
        if (docId != null) {
            StudentId[] my = getStudents().stream().toArray(StudentId[]::new);
            return unitfacade.getIntersection(docId, my);
        }
        return new StudentId[0];
    }

    @Override
    public StudentId[] getIntersection(StudentId[] student) {
        final Collection<StudentId> my = getStudents();
        return Arrays.stream(student).filter(my::contains).toArray(StudentId[]::new);
    }

    @Override
    public Collection<UnitId> getUnits() {
        //TODO add pu
        Set<UnitId> set = getTargetAssessmentDocumentResultList().stream().flatMap((TermGradeTargetAssessmentEntity tgtae) -> tgtae.getUnitDocs().stream()).map(UnitDocumentEntity::getUnitId).distinct().collect(Collectors.toSet());
        if (getPrimaryUnit() != null) {
            set.add(getPrimaryUnit());
        }
        return set;
    }

    @Override
    public Collection<String> getPatternChannels() {
        String[] ids = getTargetAssessmentDocumentResultList().stream().map(TermGradeTargetAssessmentEntity::getDocumentId).distinct().map(DocumentId::getId).toArray(String[]::new);
        return channels.getPatternChannels(ids);
    }

//    //TODO: check access permission
//    @Override
//    public JoinedUnitsEntry getJoinedUnits(final DocumentId base) {
//        JoinedUnitsEntry ret = joined.computeIfAbsent(base, this::loadJoinedUnits);
//        return ret == JoinedUnitsEntry.EMPTY ? null : ret;
//    }
//
//    protected JoinedUnitsEntry loadJoinedUnits(DocumentId base) {
//        //Todo: result may be erroneous and cause SigneeAccessExceptions
//        final UnitId unit = docModel.convertToUnitId(base);
//        final UnitJoinDocumentEntity[] res = facade.findJoinedUnits(unit);
//        return //                    UnitId ju = ContainerBuilder.findUnitIdFromUnitDocumentId(ujde.getDocumentId());
//                Arrays.stream(res).collect(CollectionUtil.singleton()).map((UnitJoinDocumentEntity ujde) -> {
//                    UnitId ju = docModel.convertToUnitId(ujde.getDocumentId());
//                    //                    UnitId ju = ContainerBuilder.findUnitIdFromUnitDocumentId(ujde.getDocumentId());
//                    UnitId[] units = ujde.getJoined().stream().map(UnitDocumentEntity::getUnitId).distinct().toArray(UnitId[]::new);
//                    return new JoinedUnitsEntry(ju, units);
//                }).orElse(JoinedUnitsEntry.EMPTY);
//    }

}
