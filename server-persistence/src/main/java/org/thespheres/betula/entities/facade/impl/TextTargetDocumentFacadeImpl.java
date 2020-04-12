/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade.impl;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.services.jms.AbstractDocumentEvent;
import org.thespheres.betula.services.jms.TextTargetAssessmentEvent;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.entities.EmbeddableUnitId;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.TermTextAssessmentEntry2;
import org.thespheres.betula.entities.TermTextTargetAssessmentEntity;
import org.thespheres.betula.entities.TermTextTargetAssessmentEntityLock;
import org.thespheres.betula.entities.UnitDocumentEntity;
import org.thespheres.betula.entities.facade.TextTargetDocumentFacade;
import org.thespheres.betula.entities.service.BetulaService;
import org.thespheres.betula.entities.watch.DocumentLockTimeoutTimer;

/**
 *
 * @author boris.heithecker
 */
@RolesAllowed("unitadmin")
@LocalBean
@Stateless
public class TextTargetDocumentFacadeImpl extends BaseDocumentFacade<TermTextTargetAssessmentEntity> implements TextTargetDocumentFacade {

    @EJB
    private DocumentLockTimeoutTimer lockTimer;
    @Default
    @Inject
    private DocumentsModel docModel;

    public TextTargetDocumentFacadeImpl() {
        super(TermTextTargetAssessmentEntity.class);
    }

    //Must be overriden otherwise decorator is not called!
    @Override
    public List<TermTextTargetAssessmentEntity> findAll(LockModeType lmt) {
        return findAllEntities(lmt, TermTextTargetAssessmentEntity.class);
    }

    //TODO: security check
    @RolesAllowed({"signee", "unitadmin"})
    @Override
    public Collection<TermTextTargetAssessmentEntity> findForPrimaryUnit(UnitId pu, LockModeType lmt) {
        return em.createNamedQuery("findTermTextTargetAssessmentEntityForPrimaryUnit", TermTextTargetAssessmentEntity.class)
                .setParameter("unit", new EmbeddableUnitId(pu))
                .setLockMode(lmt)
                .getResultList();
    }

    @Override
    public List<TermTextTargetAssessmentEntity> findAll(SigneeEntity signee, LockModeType lmt) {
        return em.createNamedQuery("findTermTextTargetAssessmentsForEntitledSignee", TermTextTargetAssessmentEntity.class)
                .setParameter("signee", signee)
                .setLockMode(lmt)
                .getResultList();
    }

    //TODO: security check
    @RolesAllowed({"signee", "unitadmin"})
    @Override
    public TermTextTargetAssessmentEntity find(DocumentId id, LockModeType lmt) {
        return super.findEntity(id, lmt);
    }

    @Override
    public boolean submit(final DocumentId id, final StudentId student, final TermId term, final Marker section, final String grade, final Timestamp timestamp, long lock) {
        final TermTextTargetAssessmentEntity target = findEntity(id, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (target != null) {
            final TermTextTargetAssessmentEntityLock l = target.getLock();
            if (l != null && l.isValid() && l.getLock() != lock) {
                return false;
            }
            final TermTextAssessmentEntry2 ae = target.findAssessmentEntry(student, term, section);
            final boolean changed;
            final String old;
            final String nv;
            final Timestamp nt;
            if (ae == null && grade != null) {
                old = null;
                final TermTextAssessmentEntry2 added = target.addEntry(student, term, section, grade, timestamp);
                em.persist(added);
                nv = added.getText();
                nt = added.getTimestamp();
                changed = true;
            } else if (ae != null) {
                old = ae.getText();
                if (grade != null) {
                    changed = ae.setText(old, timestamp);
                    nv = ae.getText();
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
                final org.thespheres.betula.document.Timestamp ts = nt != null ? new org.thespheres.betula.document.Timestamp(nt) : null;
                documentsNotificator.notityConsumers(new TextTargetAssessmentEvent(id, student, term, nv, old, ts, login.getSigneePrincipal(false)));//ZGN
            }
            return true;
        }
        return false;
    }

    @Override
    public TermTextTargetAssessmentEntity create(DocumentId docId, UnitId related) {
        TermTextTargetAssessmentEntity tae = new TermTextTargetAssessmentEntity(docId, null);
        DocumentId unitDocId = docModel.convertToUnitDocumentId(related); //ContainerBuilder.findUnitDocumentId(related);
        UnitDocumentEntity ude = em.find(UnitDocumentEntity.class, unitDocId);
        if (ude == null) {
            Logger.getLogger(BetulaService.class.getName()).log(Level.SEVERE, "No UnitDocumentEntity for unit: {0}. Cannot add related TargetDocumentEntity + {1} to UnitDocumentEntity.", new Object[]{unitDocId.toString(), docId.toString()});
        } else {
            tae.getUnitDocs().add(ude);
            ude.getTargetAssessments().add(tae);
            em.merge(ude);
        }
        em.persist(tae);
        documentsNotificator.notityConsumers(new TextTargetAssessmentEvent(tae.getDocumentId(), AbstractDocumentEvent.DocumentEventType.ADD, login.getSigneePrincipal(false)));//ZGN
        return tae;
    }

    @Override
    public long getLock(DocumentId id, long timeout) {
        TermTextTargetAssessmentEntity target = findEntity(id, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (target != null) {
            TermTextTargetAssessmentEntityLock last = target.getLock();
            if (last == null || !last.isValid()) {
                long lock = last != null ? last.getLock() + 1 : 1;
                TermTextTargetAssessmentEntityLock l = new TermTextTargetAssessmentEntityLock(lock, timeout, login.getSigneePrincipal(false));
                target.setLock(l);
                em.merge(target);
                lockTimer.addEvent(l.getTimeout(), id, lock);
                return lock;
            }
        }
        return -1;
    }

    //Need to permit all, because this is called by DocumentLockTimeoutTimer
    @PermitAll
    @Override
    public void releaseLock(DocumentId id, long lock) {
        TermTextTargetAssessmentEntity target = findEntity(id, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (target != null) {
            TermTextTargetAssessmentEntityLock last = target.getLock();
            if (last != null && last.isValid() && last.getLock() == lock) {
                last.invalidate();
                em.merge(target);
            }
        }
    }

}
