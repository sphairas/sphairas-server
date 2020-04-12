/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.LockModeType;
import org.thespheres.betula.Identity;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.server.beans.NoEntityFoundException;
import org.thespheres.betula.services.jms.TicketEvent;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.entities.BaseTicketEntity;
import org.thespheres.betula.entities.EmbeddableStudentId;
import org.thespheres.betula.entities.StudentsTicketEntity;
import org.thespheres.betula.entities.TermGradeTargAssessTicketEnt;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.UnitTicketEntity;
import org.thespheres.betula.entities.facade.TicketFacade;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class TicketFacadeImpl extends AbstractTicketsFacade implements TicketFacade {

//    @Inject
//    private DocumentsModel model;

    @Override
    public List<BaseTicketEntity> getTickets(final DocumentId targetDoc, final TermId term, final StudentId student, final String signeeType) {
        final List<BaseTicketEntity> ret = new ArrayList<>();
        final TermGradeTargetAssessmentEntity target = findEntity(targetDoc);
        if (target == null) {
            return ret;
        }
        //luid -> to jahrgang, find jahrgangstickets
        List<TermGradeTargAssessTicketEnt> l = findTickets(target, term, student, signeeType, false, LockModeType.OPTIMISTIC);
        ret.addAll(l);
        if (signeeType != null && term != null && student != null) {
            //Do not remove commented alternative if getUnitTickets causes problems
//            List<UnitTicketEntity> l2 = getUnitTickets(target, term, student, signeeType, LockModeType.OPTIMISTIC);
            final List<UnitTicketEntity> l2 = getUnitTickets(term, student, signeeType, LockModeType.OPTIMISTIC);
            l2.stream()
                    .filter(ute -> !isTargetTypeExempted(ute, target))
                    .filter(ute -> !isStudentExempted(ute, student))
                    .forEach(ret::add);
            List<UnitTicketEntity> l3 = getAGTickets(target, term, student, signeeType, LockModeType.OPTIMISTIC);
            ret.addAll(l3);
        }
        if (student != null) {
            List<StudentsTicketEntity> l4 = getStudentsTickets(student, term, signeeType, LockModeType.OPTIMISTIC);
            ret.addAll(l4);
        }
        return ret;
    }

    private boolean isStudentExempted(UnitTicketEntity ute, final StudentId student) {
        if (student != null) {
            return ute.getExemptedStudents().stream().map(EmbeddableStudentId::getStudentId).anyMatch(sid -> sid.equals(student));
        }
        return false;
    }

    private boolean isTargetTypeExempted(final UnitTicketEntity ute, final TermGradeTargetAssessmentEntity target) {
        final String tt = ute.getTargetType();
        if (tt != null) {
            final String entityTargetType = target.getTargetType();
            return entityTargetType == null
                    || Arrays.stream(tt.split(","))
                            .noneMatch(entityTargetType::equalsIgnoreCase);
        }
        return false;
    }

    @Override
    public List<BaseTicketEntity> getTickets(Identity scope) {
        javax.persistence.criteria.CriteriaQuery<BaseTicketEntity> cq = em.getCriteriaBuilder().createQuery(BaseTicketEntity.class);
        cq.select(cq.from(BaseTicketEntity.class));
        return em.createQuery(cq)
                .setLockMode(LockModeType.OPTIMISTIC)
                .getResultList().stream()
                .collect(Collectors.toList());
    }

    @Override
    public BaseTicketEntity getTicket(Ticket ticket, LockModeType lmt) throws NoEntityFoundException {
        return findTicket(BaseTicketEntity.class, ticket, lmt);
    }

    @Override
    public void removeTicket(Ticket ticket) throws NoEntityFoundException {
        final BaseTicketEntity entity = findTicket(BaseTicketEntity.class, ticket, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        em.remove(entity);
    }

    @Override
    public UnitTicketEntity createUnitTicketEntity(UnitId identity, TermId term, String signeeType, String[] targetTypes, StudentId[] exempted) {
        final String tt;
        if (targetTypes != null) {
            final StringJoiner sj = new StringJoiner(",");
            Arrays.stream(targetTypes).forEach(sj::add);
            tt = sj.toString();
        } else {
            tt = null;
        }
        final UnitTicketEntity ne = new UnitTicketEntity(identity, term, signeeType, tt);
        if (exempted != null && exempted.length != 0) {
            Arrays.stream(exempted)
                    .map(sid -> new EmbeddableStudentId(sid))
                    .forEach(ne.getExemptedStudents()::add);
        }
        em.persist(ne);
        final TicketEvent te = new TicketEvent(ne.getTicket(), TicketEvent.TicketEventType.ADD);
        ticketsNotificator.notityConsumers(te);
        return ne;
    }

    @Override
    public TermGradeTargAssessTicketEnt createTermGradeTargetAssessmentTicketEntity(DocumentId targetDoc, TermId term, StudentId student, String signeeType) throws NoEntityFoundException {
        TermGradeTargetAssessmentEntity target = findEntity(targetDoc);
        if (target == null) {
            throw new NoEntityFoundException();
        }
        final TermGradeTargAssessTicketEnt ne = new TermGradeTargAssessTicketEnt(target, term, student, signeeType);
        em.persist(ne);
        TicketEvent te = new TicketEvent(ne.getTicket(), TicketEvent.TicketEventType.ADD);
        ticketsNotificator.notityConsumers(te);
        return ne;
    }

    @Override
    public StudentsTicketEntity createStudentTicket(StudentId[] studs, TermId term, String signeeType) {
        final StudentsTicketEntity t = new StudentsTicketEntity(term, signeeType, studs);
        em.persist(t);
        TicketEvent te = new TicketEvent(t.getTicket(), TicketEvent.TicketEventType.ADD);
        ticketsNotificator.notityConsumers(te);
        return t;
    }
}
