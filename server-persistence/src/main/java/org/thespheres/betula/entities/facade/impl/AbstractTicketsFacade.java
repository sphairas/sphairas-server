/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.server.beans.NoEntityFoundException;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.entities.BaseTargetAssessmentEntity;
import org.thespheres.betula.entities.BaseTicketEntity;
import org.thespheres.betula.entities.EmbeddableStudentId;
import org.thespheres.betula.entities.EmbeddableTermId;
import org.thespheres.betula.entities.StudentsTicketEntity;
import org.thespheres.betula.entities.TermGradeTargAssessTicketEnt;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.UnitTicketEntity;
import org.thespheres.betula.entities.config.AppProperties;
import org.thespheres.betula.entities.jmsimpl.TicketsNotificator;
import org.thespheres.betula.server.beans.annot.Arbeitsgemeinschaft;

/**
 *
 * @author boris.heithecker
 */
public abstract class AbstractTicketsFacade {

    @PersistenceContext(unitName = "betula0")
    protected EntityManager em;
    @Inject
    protected TicketsNotificator ticketsNotificator;
    @Arbeitsgemeinschaft
    @Inject
    private Marker agMarker;

    protected <T extends BaseTicketEntity> T findTicket(Class<T> clz, Ticket ticket, LockModeType lmt) throws NoEntityFoundException {
        if (!ticket.getAuthority().equals(AppProperties.ticketsAuthority())) {
            throw new NoEntityFoundException();
        }
        final T t = em.find(clz, ticket.getId(), lmt);
        if (t == null) {
            throw new NoEntityFoundException();
        }
        return t;
    }

    protected List<UnitTicketEntity> getAGTickets(final BaseTargetAssessmentEntity target, TermId term, StudentId student, String signeeType, LockModeType lmt) {
        if (!target.getDocumentId().getId().contains("-ag-")) {
            //TODO: config, service: marker for documentId
            return Collections.EMPTY_LIST;
        }
        //TODO use configurable AG marker
        return em.createNamedQuery("findUnitTicketForAG", UnitTicketEntity.class).
                setParameter("target", target).
                setParameter("termAuthority", term.getAuthority()).
                setParameter("termId", term.getId()).
                setParameter("signeeType", signeeType).
                setParameter("studentAuthority", student.getAuthority()).
                setParameter("studentId", student.getId()).
                setLockMode(lmt).
                getResultList();
    }

    protected List<UnitTicketEntity> getUnitTickets(TermGradeTargetAssessmentEntity target, TermId term, StudentId student, String signeeType, LockModeType lmt) {
        return em.createNamedQuery("findUnitTicketForDocument", UnitTicketEntity.class).
                setParameter("target", target).
                setParameter("termAuthority", term.getAuthority()).
                setParameter("termId", term.getId()).
                setParameter("studentAuthority", student.getAuthority()).
                setParameter("studentId", student.getId()).
                setParameter("signeeType", signeeType).
                setLockMode(lmt).
                getResultList();
    }

    protected List<UnitTicketEntity> getUnitTickets(TermId term, StudentId student, String signeeType, LockModeType lmt) {
        return em.createNamedQuery("UnitTicketEntity.findUnitTicketForStudent", UnitTicketEntity.class).
                setParameter("termAuthority", term.getAuthority()).
                setParameter("termId", term.getId()).
                setParameter("studentAuthority", student.getAuthority()).
                setParameter("studentId", student.getId()).
                setParameter("signeeType", signeeType).
                setLockMode(lmt).
                getResultList();
    }

    protected TermGradeTargetAssessmentEntity findEntity(DocumentId targetDoc) {
        return em.find(TermGradeTargetAssessmentEntity.class, targetDoc, LockModeType.OPTIMISTIC);
    }

    //strict = for createion check of new ticekt
    protected List<TermGradeTargAssessTicketEnt> findTickets(final BaseTargetAssessmentEntity<?, ?> target, TermId term, StudentId student, String signeeType, boolean strict, LockModeType lmt) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<TermGradeTargAssessTicketEnt> cq = cb.createQuery(TermGradeTargAssessTicketEnt.class);
        Root<TermGradeTargAssessTicketEnt> tgtate = cq.from(TermGradeTargAssessTicketEnt.class);
        List<Predicate> list = new ArrayList<>();
        list.add(cb.equal(tgtate.get("target"), target));
        if (term != null) {
            list.add(cb.equal(tgtate.get("term"), new EmbeddableTermId(term)));
        }
        if (student != null) {
            //ZGN
            Predicate studPred = cb.equal(tgtate.get("student"), new EmbeddableStudentId(student));
            if (!strict) {
                list.add(cb.or(cb.isNull(tgtate.get("student")), studPred)); //
            } else {
                list.add(studPred);
            }
        }
        if (signeeType != null) {
            list.add(cb.equal(tgtate.get("type"), signeeType));
        }
        cq.select(tgtate)
                .distinct(true)
                .where(list.stream().toArray(Predicate[]::new));
        List<TermGradeTargAssessTicketEnt> l = em.createQuery(cq).setLockMode(lmt).getResultList();
        return l;
    }

    protected List<StudentsTicketEntity> getStudentsTickets(StudentId student, TermId term, String signeeType, LockModeType lmt) {
        return em.createNamedQuery("findStudentsTicket", StudentsTicketEntity.class).
                setParameter("studentAuthority", student.getAuthority()).
                setParameter("studentId", student.getId()).
                setParameter("term", term != null ? new EmbeddableTermId(term) : null).
                setParameter("signeeType", signeeType).
                setLockMode(lmt).
                getResultList();
    }

}
