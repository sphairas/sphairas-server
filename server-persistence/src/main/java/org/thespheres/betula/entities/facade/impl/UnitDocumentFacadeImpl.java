/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.openide.util.NbBundle;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.services.jms.AbstractDocumentEvent;
import org.thespheres.betula.services.jms.UnitDocumentEvent;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.entities.BaseTargetAssessmentEntity;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.SigneeUnitMapDocumentEntity;
import org.thespheres.betula.entities.StudentIdCollectionChangeLog;
import org.thespheres.betula.entities.UnitDocumentEntity;
import org.thespheres.betula.entities.UnitStringMapDocumentEntity;
import org.thespheres.betula.entities.config.IllegalServiceArgumentException;
import org.thespheres.betula.entities.facade.UnitDocumentFacade;
import org.thespheres.betula.server.beans.StudentsLocalBean;
import org.thespheres.ical.VCard;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class UnitDocumentFacadeImpl extends BaseDocumentFacade<UnitDocumentEntity> implements UnitDocumentFacade {

    @EJB(beanName = "StudentVCardsImpl")
    private StudentsLocalBean slb;

    public UnitDocumentFacadeImpl() {
        super(UnitDocumentEntity.class);
    }

    @Override
    public void edit(UnitDocumentEntity entity, boolean notifyDocumentUpdate) {
        em.merge(entity);
        if (notifyDocumentUpdate) {
            documentsNotificator.notityConsumers(new UnitDocumentEvent(entity.getDocumentId(), entity.getUnitId(), UnitDocumentEvent.DocumentEventType.CHANGE, login.getSigneePrincipal(false)));//ZGN
        }
    }

    @Override
    public UnitDocumentEntity create(UnitId unit, String suffix, SigneeEntity creator) {
        final UnitDocumentEntity u = new UnitDocumentEntity(unit, suffix, creator);
        em.persist(u);
        documentsNotificator.notityConsumers(new UnitDocumentEvent(u.getDocumentId(), unit, UnitDocumentEvent.DocumentEventType.ADD, login.getSigneePrincipal(false)));//ZGN
        return u;
    }

    @Override
    public List<UnitDocumentEntity> findAll(LockModeType lmt) {
//        return findAllEntities(lmt, entityClass);
        return em.createNamedQuery("UnitDocumentEntity.findAll", UnitDocumentEntity.class)
                .setLockMode(lmt)
                .getResultList();
    }

    @RolesAllowed({"signee", "unitadmin"})
    @Override  //TODO security check!!!!  must be: primaryunit or unittarget
    public UnitDocumentEntity find(DocumentId id, LockModeType lmt) {
        return super.findEntity(id, lmt);
    }

    @NbBundle.Messages("UnitDocumentFacadeImpl.NonUniqueResult=Found multiple primary units for student {0}. Please fix database!")
    @Override
    public UnitId findPrimaryUnitForStudent(StudentId sid, Date asOf, LockModeType lmt) {
        try {
            final UnitDocumentEntity ude = em.createNamedQuery("findPrimaryUnitForStudent", UnitDocumentEntity.class)
                    .setParameter("studentId", sid.getId())
                    .setParameter("studentAuthority", sid.getAuthority())
                    .setLockMode(lmt)
                    .getSingleResult();
            if (asOf != null) {
                final Set<StudentId> ret = ude.getStudentIds().stream().collect(Collectors.toSet());
                adoptStudentsToVersionAsOf(ude, asOf, ret);
                if (!ret.contains(sid)) {
                    final List<StudentIdCollectionChangeLog> sccl = em.createNamedQuery("findPrimaryUnitChangeLogForStudent", StudentIdCollectionChangeLog.class)
                            .setParameter("studentId", sid.getId())
                            .setParameter("studentAuthority", sid.getAuthority())
                            //                            .setLockMode(lmt)
                            .getResultList();
                    UnitId found = null;
                    for (StudentIdCollectionChangeLog log : sccl) {
                        final UnitDocumentEntity test = (UnitDocumentEntity) log.getBaseDocumentEntity();
                        final Set<StudentId> ret2 = test.getStudentIds().stream().collect(Collectors.toSet());
                        adoptStudentsToVersionAsOf(test, asOf, ret2);
                        if (ret2.contains(sid)) {
                            if (found == null) {
                                found = test.getUnitId();
                            } else {
                                return warnNonUniqueResultException(sid);
                            }
                        }
                    }
                    return found;
                }
            }
            return ude.getUnitId();
        } catch (NoResultException nex) {
            return null;
        } catch (NonUniqueResultException nuex) {
            return warnNonUniqueResultException(sid);
        }
    }

    private UnitId warnNonUniqueResultException(StudentId sid) {
        String fn = Optional.ofNullable(slb.get(sid)).map(VCard::getFN).orElse(Long.toString(sid.getId()));
        String msg = NbBundle.getMessage(UnitDocumentFacadeImpl.class, "UnitDocumentFacadeImpl.NonUniqueResult", fn);
        Logger.getLogger(UnitDocumentFacadeImpl.class.getName()).log(Level.WARNING, msg);
//            throw new IllegalBeanStateException(msg, nuex);
        return null;
    }

    @Override
    public StudentId[] getIntersection(DocumentId unit, StudentId[] my) {
        UnitDocumentEntity ude = find(unit, LockModeType.OPTIMISTIC);
        if (ude != null) {
            final Set<StudentId> ss = ude.getStudentIds();
            return Arrays.stream(my)
                    .filter(ss::contains)
                    .toArray(StudentId[]::new);
        }
        return new StudentId[0];
    }

    private SigneeUnitMapDocumentEntity findKlassenlehrerDoc(DocumentId document, LockModeType lmt) {
        SigneeUnitMapDocumentEntity sude = em.find(SigneeUnitMapDocumentEntity.class, document, lmt);
        if (sude == null) {
            sude = new SigneeUnitMapDocumentEntity(document, null);
            em.persist(sude);
            em.lock(sude, lmt);
        }
        return sude;
    }

//    @Override
//    public UnitId getPrimaryUnit(Signee signee) {
//        SigneeUnitMapDocumentEntity sude = findKlassenlehrerDoc(LockModeType.OPTIMISTIC);
//        assert sude != null;
//        SigneeEntity se = em.find(SigneeEntity.class, signee);
//        if (se != null) {
//            return sude.get(se);
//        }
//        throw new RuntimeException();
//    }
    @Override
    public UnitId getPrimaryUnit(DocumentId document, Signee signee) {
        final SigneeUnitMapDocumentEntity sude = findKlassenlehrerDoc(document, LockModeType.OPTIMISTIC);
        assert sude != null;
        final SigneeEntity se = em.find(SigneeEntity.class, signee);
        if (se != null) {
            return sude.get(se);
        }
        return null;
    }

    @Override
    public Map<UnitId, List<Signee>> getPrimaryUnitsSignees(DocumentId document) {
        final SigneeUnitMapDocumentEntity sude = findKlassenlehrerDoc(document, LockModeType.OPTIMISTIC);
        assert sude != null;
        return sude.map().entrySet().stream()
                .collect(Collectors.groupingBy(e -> e.getValue().getUnitId(), Collectors.mapping(e -> e.getKey().getSignee(), Collectors.toList())));
    }

    @RolesAllowed("unitadmin")
    @Override
    public void setPrimaryUnit(final DocumentId document, final Signee signee, final UnitId unit, final String propagationId) {
        final SigneeUnitMapDocumentEntity sude = findKlassenlehrerDoc(document, LockModeType.OPTIMISTIC);
        assert sude != null;
        final SigneeEntity se = em.find(SigneeEntity.class, signee);
        if (se != null) {
            sude.put(se, unit);
            em.merge(sude);
            final AbstractDocumentEvent event = new AbstractDocumentEvent(document, AbstractDocumentEvent.DocumentEventType.CHANGE, null, propagationId);
            documentsNotificator.notityConsumers(event);
        } else {
            throw new IllegalServiceArgumentException();
        }
    }

    @RolesAllowed({"remoteadmin", "unitadmin"})
    @Override
    public void remove(final UnitDocumentEntity ude) {
        final Set<BaseTargetAssessmentEntity> ta = ude.getTargetAssessments();
        final UnitDocumentEvent event = new UnitDocumentEvent(ude.getDocumentId(), ude.getUnitId(), AbstractDocumentEvent.DocumentEventType.REMOVE, null);
        ta.stream().forEach(t -> {
            t.getUnitDocs().remove(ude);
            em.merge(t);
        });
        super.remove(ude);
        documentsNotificator.notityConsumers(event);
    }

    @Override
    public String getCommonName(DocumentId d, UnitId uid) {
        UnitStringMapDocumentEntity usm = findCommonNameDocument(d, LockModeType.OPTIMISTIC);
        assert usm != null;
        if (uid != null) {
            return usm.get(uid);
        }
        return null;
    }

    @RolesAllowed("unitadmin")
    @Override
    public void setCommonName(DocumentId d, UnitId uid, String cn) {
        UnitStringMapDocumentEntity usm = findCommonNameDocument(d, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        assert usm != null;
        if (uid != null) {
            usm.put(uid, cn);
        }
    }

    private UnitStringMapDocumentEntity findCommonNameDocument(DocumentId d, LockModeType lmt) {
        UnitStringMapDocumentEntity usm = em.find(UnitStringMapDocumentEntity.class, d, LockModeType.OPTIMISTIC);
        if (usm == null) {
            usm = new UnitStringMapDocumentEntity(d, null);
            em.persist(usm);
            em.lock(usm, lmt);
        }
        return usm;
    }

    @RolesAllowed({"remoteadmin", "unitadmin"})
    @Override
    public List<UnitDocumentEntity> getAllPrimaryUnits(final java.sql.Timestamp expringBefore) {
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<UnitDocumentEntity> cq = cb.createQuery(UnitDocumentEntity.class);
        final Root<UnitDocumentEntity> pet = cq.from(UnitDocumentEntity.class);

        final List<Predicate> list = new ArrayList<>();
        list.add(cb.equal(pet.join("markerSet").get("convention"), "betula-db"));
        list.add(cb.equal(pet.join("markerSet").get("markerId"), "primary-unit"));
        list.add(cb.isNull(pet.join("markerSet").get("subset")));

        if (expringBefore != null) {
//            ParameterExpression<Date> param = cb.parameter(Date.class, "dateLimit");
            list.add(cb.lessThanOrEqualTo(pet.<java.sql.Timestamp>get("expirationTime"), expringBefore));
        }

        cq.select(pet).distinct(true).where(list.stream().toArray(Predicate[]::new));
        return em.createQuery(cq).setLockMode(LockModeType.OPTIMISTIC).getResultList();
    }

    @Override
    public Set<UnitDocumentEntity> findForStudents(Set<StudentId> related) {

//        final Set<EmbeddableStudentId> set = related.stream()
//                .map(EmbeddableStudentId::new)
//                .collect(Collectors.toSet());
        final Map<String, List<Long>> m = related.stream()
                .collect(Collectors.groupingBy(StudentId::getAuthority, Collectors.mapping(StudentId::getId, Collectors.toList())));
        final Set<UnitDocumentEntity> ret = new HashSet<>();
        m.forEach((a, l) -> {
            List<UnitDocumentEntity> res = em.createNamedQuery("findUnitsForStudents", UnitDocumentEntity.class)
                    .setParameter("studentIds", l)
                    .setParameter("authority", a)
                    .setLockMode(LockModeType.OPTIMISTIC)
                    .getResultList();
            ret.addAll(res);
        });
        return ret;
    }

    @Override
    public void adoptStudentsToVersionAsOf(final UnitDocumentEntity ude, final Date date, final Set<StudentId> set) {
        final UnitStudentsRestore restore = new UnitStudentsRestore(set);
        ude.applyRestoreVersion(date, restore);
//        final List<BaseChangeLog> cl = ude.getChangeLog();
//        if (!cl.isEmpty()) {
//            int stop = -1;
//            for (int i = cl.size() - 1; i >= 0; i--) {
//                final BaseChangeLog bcl = cl.get(i);
//                if (bcl instanceof VersionChangeLog) {
//                    final VersionChangeLog log = (VersionChangeLog) bcl;
//                    if (log.getTimeStamp().before(date)) {
//                        break;
//                    } else {
//                        stop = i;
//                    }
//                }
//            }
//            if (stop != -1) {
//                for (int i = cl.size() - 1; i > stop; i--) {
//                    final BaseChangeLog bcl = cl.get(i);
//                    applyChangeLog(bcl, set);
//                }
//            }
//        }
    }
//
//    public void applyChangeLog(BaseChangeLog bcl, Set<StudentId> set) {
//        if (bcl instanceof StudentIdCollectionChangeLog && bcl.getProperty().equals("UNIT_DOCUMENT_STUDENTS")) {
//            final StudentIdCollectionChangeLog log = (StudentIdCollectionChangeLog) bcl;
//            final StudentId csid = log.getValue();
//            if (csid != null) {
//                switch (log.getAction()) {
//                    case REMOVE:
//                        set.add(csid);
//                        break;
//                    case ADD:
//                        set.remove(csid);
//                        break;
//                }
//            }
//        }
//    }
}
