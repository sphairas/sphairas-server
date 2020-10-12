/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import org.openide.util.NbBundle;
import org.thespheres.betula.Identity;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.services.jms.AbstractDocumentEvent;
import org.thespheres.betula.services.jms.AbstractDocumentEvent.DocumentEventType;
import org.thespheres.betula.services.jms.MultiTargetAssessmentEvent;
import org.thespheres.betula.services.jms.MultiTargetAssessmentEvent.Update;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.entities.BaseAssessmentEntry;
import org.thespheres.betula.entities.DocumentStringMapDocumentEntity;
import org.thespheres.betula.entities.EmbeddableStudentId;
import org.thespheres.betula.entities.EmbeddableTermId;
import org.thespheres.betula.entities.GradeTargetAssessmentEntity;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.TermTextTargetAssessmentEntity;
import org.thespheres.betula.entities.UnitDocumentEntity;
import org.thespheres.betula.entities.config.AppProperties;
import org.thespheres.betula.entities.facade.GradeTargetDocumentFacade;
import org.thespheres.betula.entities.facade.UnitDocumentFacade;
import org.thespheres.betula.entities.service.BetulaService;
import org.thespheres.betula.entities.localbeans.StudentsListsLocalBeanImpl;
import org.thespheres.betula.util.GradeEntry;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class GradeTargetDocumentFacadeImpl extends BaseDocumentFacade<GradeTargetAssessmentEntity> implements GradeTargetDocumentFacade {

    @EJB
    private UnitDocumentFacade unitDocumentFacade;

    public GradeTargetDocumentFacadeImpl() {
        super(GradeTargetAssessmentEntity.class);
    }

    //Must be overriden otherwise decorator is not called!
    @Override
    public <T extends GradeTargetAssessmentEntity> List<T> findAll(LockModeType lmt, Class<T> type) {
        return (List<T>) findAllEntities(lmt, type);
    }

    @Override
    public <T extends GradeTargetAssessmentEntity> List<T> findAll(final SigneeEntity signee, final Class<T> type, final LockModeType lmt) {
        if (!type.equals(TermGradeTargetAssessmentEntity.class)) {
            throw new IllegalArgumentException("Type must be TermGradeTargetAssessmentEntity");
        }
        if (signee == null) {
            return findAll(lmt, type);
        }
        return (List<T>) em.createNamedQuery("findTermGradeTargetAssessmentsSignees", TermGradeTargetAssessmentEntity.class)
                .setParameter("signee", signee)
                .setParameter("types", AppProperties.secureSigneeTypes())
                .setLockMode(lmt)
                .getResultList();
    }

    @Override
    public GradeTargetAssessmentEntity find(DocumentId id, LockModeType lmt) {
        try {
            return super.findEntity(id, lmt);
        } catch (ClassCastException e) {
            return null;
        }
    }

    @Deprecated
    @RolesAllowed("unitadmin")
    @Override
    public <I extends Identity> Map<StudentId, Grade> findAll(DocumentId id, final I gradeId) {
        final GradeTargetAssessmentEntity target = findEntity(id, LockModeType.OPTIMISTIC);
        final Set<BaseAssessmentEntry<I>> entries = target.getEntries();
        return entries.stream()
                .filter(bae -> bae.getGradeId().equals(gradeId))
                .collect(Collectors.toMap(BaseAssessmentEntry::getStudentId, BaseAssessmentEntry::getGrade));
    }

    @Override
    public <I extends Identity> Map<StudentId, GradeEntry> findAllEntries(DocumentId id, I gradeId) {
        final GradeTargetAssessmentEntity target = findEntity(id, LockModeType.OPTIMISTIC);
        final Set<BaseAssessmentEntry<I>> entries = target.getEntries();
        class GradeValue implements GradeEntry {

            private final Grade grade;
            private final org.thespheres.betula.document.Timestamp timestamp;

            public GradeValue(Grade grade, Timestamp timestamp) {
                this.grade = grade;
                this.timestamp = new org.thespheres.betula.document.Timestamp(timestamp);
            }

            @Override
            public Grade getGrade() {
                return grade;
            }

            @Override
            public org.thespheres.betula.document.Timestamp getTimestamp() {
                return timestamp;
            }

        }
        return entries.stream()
                .filter(bae -> bae.getGradeId().equals(gradeId))
                .collect(Collectors.toMap(BaseAssessmentEntry::getStudentId, bae -> new GradeValue(bae.getGrade(), bae.getTimestamp())));
    }

    private <I extends Identity> List<BaseAssessmentEntry<I>> findEntries(final GradeTargetAssessmentEntity<I> target, final StudentId student, final I gradeId) {
        return target.getEntries().stream()
                .filter(e -> e.getStudentId().equals(student))
                .filter(e -> Objects.equals(e.getGradeId(), gradeId))
                .collect(Collectors.toList());
    }

    @Override
    public <I extends Identity> boolean submit(DocumentId id, StudentId student, I term, Grade grade, Timestamp timestamp) {
        final Map<String, Object> props = new HashMap<>();
        props.put("javax.persistence.cache.retrieveMode", "REFRESH");
//        final GradeTargetAssessmentEntity<I> target = findEntity(id, LockModeType.OPTIMISTIC_FORCE_INCREMENT); 
        final GradeTargetAssessmentEntity<I> target = em.find(entityClass, id, LockModeType.OPTIMISTIC_FORCE_INCREMENT, props);
        if (false) {
            return submitOld(target, student, term, grade, timestamp, id);
        } else {
            final BaseAssessmentEntry<I> ae = target.findAssessmentEntry(student, term);
//        Date ts = timestamp != null ? timestamp.getDate() : null;
            final boolean changed;
            final Grade old;
            final Grade nv;
            final Timestamp nt;
            if (ae == null && grade != null) {
                old = null;
                final BaseAssessmentEntry<I> added = target.addEntry(student, term, grade, timestamp);
                em.persist(added);
                nv = added.getGrade();
                nt = added.getTimestamp();
                changed = true;
            } else if (ae != null) {
                old = ae.getGrade();
                if (grade != null) {
                    changed = ae.setGrade(grade, timestamp); //ts);
                    nv = ae.getGrade();
                    nt = ae.getTimestamp();
                } else {
                    changed = target.getEntries().remove(ae);
                    nv = null;
                    nt = null;
                }
            } else {
                changed = false;
                old = null;
                nv = null;
                nt = null;
            }
            if (changed) {
                em.merge(target);
                final Update<I> tu = new Update<>(student, term, old, nv, nt != null ? new org.thespheres.betula.document.Timestamp(nt) : null);
                documentsNotificator.notityConsumers(new MultiTargetAssessmentEvent<>(id, new Update[]{tu}, login.getSigneePrincipal(false)));//ZGN
                return true;
            }
            return false;
        }
    }

    private <I extends Identity> boolean submitOld(final GradeTargetAssessmentEntity target, StudentId student, I gradeId, Grade grade, Timestamp timestamp, DocumentId id) {
        final BaseAssessmentEntry<I> ae = target.findAssessmentEntry(student, gradeId);
//        Date ts = timestamp != null ? timestamp.getDate() : null;
        if (ae != null) {
            boolean changed;
            Grade old = ae.getGrade();
            Grade nv;
            if (grade != null) {
                changed = ae.setGrade(grade, timestamp); //ts);
                nv = ae.getGrade();
            } else {
                changed = target.getEntries().remove(ae);
                nv = null;
            }
            if (changed) {
                em.merge(target);
                final Update<I> tu = new Update<>(student, gradeId, old, nv, new org.thespheres.betula.document.Timestamp(ae.getTimestamp()));
                documentsNotificator.notityConsumers(new MultiTargetAssessmentEvent<>(id, new Update[]{tu}, login.getSigneePrincipal(false)));//ZGN
            }
            return true;
        }
        return false;
    }

    @RolesAllowed("unitadmin")
    @Override
    public <I extends Identity> boolean submitAll(final DocumentId id, final I gradeId, final Map<StudentId, Grade> grades, final Map<StudentId, Timestamp> timestamps) {
        GradeTargetAssessmentEntity target = findEntity(id, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        boolean changed = false;
        final List<Update<I>> updates = new ArrayList<>();
        for (final Map.Entry<StudentId, Grade> e : grades.entrySet()) {
            final Grade grade = e.getValue();
            final BaseAssessmentEntry<I> ae = target.findAssessmentEntry(e.getKey(), gradeId);
            boolean res = false;
            final Timestamp timestamp = timestamps.get(e.getKey());
            final Timestamp newTime;
            final Grade ov;
            final Grade nv;
            if (ae == null && grade != null) {
                final BaseAssessmentEntry<I> added = target.addEntry(e.getKey(), gradeId, grade, timestamp);
                em.persist(added);
                nv = added.getGrade();
                ov = null;
                newTime = added.getTimestamp();
                res = true;
            } else {
                ov = ae.getGrade();
                if (grade != null) {
                    res = ae.setGrade(grade, timestamp); //ts);
                    nv = ae.getGrade();
                    newTime = ae.getTimestamp();
                } else {
                    res = target.getEntries().remove(ae);
                    nv = null;
                    newTime = null;
                }
            }
            if (res) {
                final Update<I> u = new Update<>(e.getKey(), gradeId, ov, nv, newTime != null ? new org.thespheres.betula.document.Timestamp(newTime) : null);
                updates.add(u);
            }
            changed = changed || res;
        }
        if (changed) {
            em.merge(target);
            documentsNotificator.notityConsumers(new MultiTargetAssessmentEvent(id, updates.stream().toArray(Update[]::new), login.getSigneePrincipal(false)));//ZGN
        }
        return true;
    }

    @Override
    public <I extends Identity> boolean clearAll(DocumentId id, I gradeId, Set<StudentId> students) {
        final GradeTargetAssessmentEntity target = findEntity(id, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        boolean changed = false;
        final List<Update<I>> updates = new ArrayList<>();
        for (StudentId s : students) {
            final BaseAssessmentEntry<I> ae = target.findAssessmentEntry(s, gradeId);
            boolean res = false;
            if (ae != null) {
                final Grade ov = ae.getGrade();
                res = target.getEntries().remove(ae);
                final Update<I> u = new Update<>(s, gradeId, ov, null, null);
                updates.add(u);
            }
            changed = changed || res;
        }
        if (changed) {
            em.merge(target);
            documentsNotificator.notityConsumers(new MultiTargetAssessmentEvent(id, updates.stream().toArray(Update[]::new), login.getSigneePrincipal(false)));//ZGN
        }
        return true;
    }

    @Override
    public <I extends Identity> GradeTargetAssessmentEntity<I> create(Class<? extends GradeTargetAssessmentEntity<I>> entityClass, final DocumentId docId, final UnitId unit) {
        final GradeTargetAssessmentEntity<I> tae;
        try {
            Constructor c = entityClass.getConstructor(DocumentId.class, SigneeEntity.class);
            tae = (GradeTargetAssessmentEntity) c.newInstance(docId, null);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }

        if (!UnitId.isNull(unit)) {
            final DocumentId unitDocId = documentsModel.convertToUnitDocumentId(unit);
            final UnitDocumentEntity ude = em.find(UnitDocumentEntity.class, unitDocId);
            if (ude == null) {
                Logger.getLogger(BetulaService.class.getName()).log(Level.SEVERE, "No UnitDocumentEntity for unit: {0}. Cannot add related TargetDocumentEntity + {1} to UnitDocumentEntity.", new Object[]{unitDocId.toString(), docId.toString()});
            } else {
                tae.getUnitDocs().add(ude);
                ude.getTargetAssessments().add(tae);
                em.merge(ude);
            }
        }
        em.persist(tae);
        documentsNotificator.notityConsumers(new MultiTargetAssessmentEvent(tae.getDocumentId(), AbstractDocumentEvent.DocumentEventType.ADD, login.getSigneePrincipal(false)));//ZGN
        return tae;
    }

//    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public DocumentId[] findDocument(StudentId student, TermId term, Marker fach) {
        if (fach == null || fach.getSubset() != null) {
            //TODO: query api for alternative ODER: subset=null ->> "" empty string in db, default value.... for column
            throw new IllegalArgumentException("This query can handle only cases where Marker.subset is null.");

        }
        return em.createNamedQuery("findTermGradeTargetAssessmentsForSubjectMarkerWithNullSubsetOnly", TermGradeTargetAssessmentEntity.class
        )
                .setParameter("student", new EmbeddableStudentId(student))
                .setParameter("term", new EmbeddableTermId(term))
                .setParameter("markerConvention", fach.getConvention())
                .setParameter("markerId", fach.getId())
                //                .setParameter("markerSubset", fach.getSubset())
                .setLockMode(LockModeType.OPTIMISTIC)
                .getResultList().stream()
                .map(TermGradeTargetAssessmentEntity::getDocumentId)
                .toArray(DocumentId[]::new);
    }

    @Override
    public List<TermGradeTargetAssessmentEntity> findForUnitDocument(final UnitDocumentEntity related, final TermId term) {
        if (term == null) {//Causes deadlock.....
            return em.createNamedQuery("TermGradeTargetAssessmentEntity.findAllTermGradeTargetAssessmentsForUnitEntityStudents", TermGradeTargetAssessmentEntity.class
            )
                    .setParameter("unit", related)
                    .setLockMode(LockModeType.OPTIMISTIC)
                    .getResultList();

        } else {
            return em.createNamedQuery("findTermGradeTargetAssessmentsForUnitEntityStudents", TermGradeTargetAssessmentEntity.class
            )
                    .setParameter("unit", related)
                    .setParameter("term", new EmbeddableTermId(term))
                    .setLockMode(LockModeType.OPTIMISTIC)
                    .getResultList();
        }
    }

    @Override
    public List<TermTextTargetAssessmentEntity> findTextsForUnitDocument(final UnitDocumentEntity related, final TermId term) {
        return em.createNamedQuery("findTermTextTargetAssessmentsForUnitEntityStudents", TermTextTargetAssessmentEntity.class)
                .setParameter("unit", related)
                .setParameter("term", new EmbeddableTermId(term))
                .setLockMode(LockModeType.OPTIMISTIC)
                .getResultList();
    }

    @Override
    public Collection<TermGradeTargetAssessmentEntity> findForStudent(final StudentId related) {
        return em.createNamedQuery("TermGradeTargetAssessmentEntity.findTermGradeTargetAssessmentsForStudent", TermGradeTargetAssessmentEntity.class
        )
                .setParameter("studentId", related.getId())
                .setParameter("studentAuthority", related.getAuthority())
                .setLockMode(LockModeType.OPTIMISTIC)
                .getResultList();
    }

    @Override
    public Collection<TermGradeTargetAssessmentEntity> findForStudents(Set<StudentId> related, TermId term) {

        final Set<EmbeddableStudentId> set = related.stream()
                .map(EmbeddableStudentId::new)
                .collect(Collectors.toSet());
//        CriteriaBuilder cb = em.getCriteriaBuilder();
//        CriteriaQuery<TermGradeTargetAssessmentEntity> cq = cb.createQuery(TermGradeTargetAssessmentEntity.class);
//        Root<TermGradeTargetAssessmentEntity> tgtae = cq.from(TermGradeTargetAssessmentEntity.class);
//        cb.in(cb.parameter(entityClass, null));
////        cb.in(tgtae.join("entries"));
//
////        CriteriaBuilder.In<StudentId> in = cb.in(cb.parameter(StudentId.class, "students"));
//
//
//        Join<Object, Object> join = tgtae.join("entries");
//        
////        Predicate equal = cb.equal(join.get("studentId"), null);
//        CriteriaBuilder.In<Object> in = cb.in(tgtae.join("entries").get("student"));
//        Path<Object> get = tgtae.join("entries").get("student");
//        
//        for(EmbeddableStudentId e : set) {
//            in.value(e);
//        }
//
//       cb.equal(tgtae.join("markerSet").get("convention"), "betula-db");
//
//        
//        cq.select(tgtae).where(in);
//        return em.createQuery(cq).getResultList();

        if (false) {
            return em.createNamedQuery("TermGradeTargetAssessmentEntity.findTermGradeTargetAssessmentsForStudents", TermGradeTargetAssessmentEntity.class
            )
                    .setParameter("students", set)
                    .setLockMode(LockModeType.OPTIMISTIC)
                    .getResultList();
        }

        final Map<String, List<Long>> m = related.stream()
                .collect(Collectors.groupingBy(s -> s.getAuthority(), Collectors.mapping(s -> s.getId(), Collectors.toList())));
        final Set<TermGradeTargetAssessmentEntity> ret = new HashSet<>();
        m
                .forEach((a, l) -> {
                    List<TermGradeTargetAssessmentEntity> res = em.createNamedQuery("TermGradeTargetAssessmentEntity.findTermGradeTargetAssessmentsForStudentsHelper", TermGradeTargetAssessmentEntity.class
                    )
                            .setParameter("studentIds", l)
                            .setParameter("authority", a)
                            .setLockMode(LockModeType.OPTIMISTIC)
                            .getResultList();
                    ret.addAll(res);
                });
        return ret;
    }

    @Override
    public boolean linkPrimaryUnits(DocumentId id, StudentId[] studs) {
        final GradeTargetAssessmentEntity target = findEntity(id, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
//        final GradeTargetAssessmentEntity target = em.merge(t);

//        Set<UnitDocumentEntity> s = Arrays.stream(studs)
//                .map(sid -> unitDocumentFacade.findPrimaryUnitForStudent(sid, LockModeType.OPTIMISTIC_FORCE_INCREMENT))
//                .filter(l -> l.size() == 1)
//                .map(l -> l.get(0))
//                .collect(Collectors.toSet());
//
//        for (UnitDocumentEntity ude : s) {
//            target.getUnitDocs().add(ude);
//            ude.getTargetAssessments().add(target);
//            em.merge(target);
//            em.merge(ude);
//        }
        final Set<DocumentId> s = Arrays.stream(studs)
                .map(sid -> findPrimaryUnits(sid, LockModeType.OPTIMISTIC))
                .filter(Objects::nonNull)
                //                .filter(l -> l.size() == 1)
                //                .map(l -> l.get(0).getDocumentId())
                .collect(Collectors.toSet());
        boolean changed = false;
        for (DocumentId ud : s) {
            final UnitDocumentEntity ude = unitDocumentFacade.find(ud, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            changed = changed | target.getUnitDocs().add(ude);
            ude.getTargetAssessments().add(target);
            em.merge(target);
            em.merge(ude);
        }
        return changed;

    }

    //Causes GC outofmemory
    private DocumentId findPrimaryUnits(StudentId sid, LockModeType lmt) {
        try {
            return em.createNamedQuery("findPrimaryUnitForStudent", UnitDocumentEntity.class)
                    .setParameter("studentId", sid.getId())
                    .setParameter("studentAuthority", sid.getAuthority())
                    .setLockMode(lmt)
                    .getSingleResult()
                    .getDocumentId();
        } catch (NoResultException nex) {
            return null;
        } catch (NonUniqueResultException nuex) {
            String msg = NbBundle.getMessage(UnitDocumentFacadeImpl.class,
                    "UnitDocumentFacadeImpl.NonUniqueResult", sid);
            Logger
                    .getLogger(StudentsListsLocalBeanImpl.class
                            .getName()).log(Level.SEVERE, msg);
//            throw new IllegalBeanStateException(msg, nuex);
            return null;
        }
    }

//    @Messages("GradeTargetDocumentFacadeImpl.findJoinedUnits.UnitDocumentEntityNotFound=UnitDocumentEntity not found for {0}")
//    @Override
//    public UnitJoinDocumentEntity[] findJoinedUnits(UnitId unit) {
////        DocumentId unitDoc = ContainerBuilder.findUnitDocumentId(unit);
//        final DocumentId unitDoc = documentsModel.convertToUnitDocumentId(unit);
//        UnitDocumentEntity ude = unitDocumentFacade.find(unitDoc, LockModeType.OPTIMISTIC);
//
//        if (ude != null && ude.getUnitId().equals(unit)) {
//            return em.createNamedQuery("findUnitJoinDocumentEntity", UnitJoinDocumentEntity.class
//            )
//                    .setParameter("joined", ude)
//                    .setLockMode(LockModeType.OPTIMISTIC)
//                    .getResultList().stream()
//                    .toArray(UnitJoinDocumentEntity[]::new);
//        }
//        return new UnitJoinDocumentEntity[0];
////        String message = NbBundle.getMessage(GradeTargetDocumentFacadeImpl.class, "GradeTargetDocumentFacadeImpl.findJoinedUnits.UnitDocumentEntityNotFound", unit.getId());
////        throw new IllegalArgumentException(message);
//    }
    @RolesAllowed("unitadmin")
    @Override
    public boolean remove(DocumentId id) {
        final GradeTargetAssessmentEntity t = findEntity(id, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (t == null) {
            return false;
        }
        final Set<UnitDocumentEntity> ud = t.getUnitDocs();
        final MultiTargetAssessmentEvent<TermId> event = new MultiTargetAssessmentEvent<>(id, DocumentEventType.REMOVE, null);
        //Cascade.REMOVE not working properly with MySql?
//        t.getEntries()
//                .forEach(em::remove);
        em.remove(em.merge(t));
        //TODO: test....
        ud.stream()
                .forEach(unit -> em.refresh(unit, LockModeType.OPTIMISTIC_FORCE_INCREMENT));
        documentsNotificator.notityConsumers(event);
        return true;
    }

    @Override
    public String getStringValue(DocumentId d, DocumentId did) {
        final DocumentStringMapDocumentEntity entity = findDocumentStringMapDocumentEntity(d, LockModeType.OPTIMISTIC);
        assert entity != null;
        if (did != null) {
            return entity.get(did);
        }
        return null;
    }

    @RolesAllowed("unitadmin")
    @Override
    public void setStringValue(DocumentId d, DocumentId did, String value) {
        final DocumentStringMapDocumentEntity entity = findDocumentStringMapDocumentEntity(d, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        assert entity != null;
        if (did != null) {
            entity.put(did, value);
        }
    }

    private DocumentStringMapDocumentEntity findDocumentStringMapDocumentEntity(final DocumentId d, final LockModeType lmt) {
        DocumentStringMapDocumentEntity ret = em.find(DocumentStringMapDocumentEntity.class,
                d, lmt);
        if (ret == null) {
            ret = new DocumentStringMapDocumentEntity(d, null);
            em.persist(ret);
            em.lock(ret, lmt);
        }
        return ret;
    }
}
