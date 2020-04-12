/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.facade.impl;

import java.util.Collection;
import java.util.stream.Collectors;
import javax.ejb.LocalBean;
import javax.ejb.NoSuchEntityException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.services.jms.StudentEvent;
import org.thespheres.ical.builder.VCardBuilder;
import org.thespheres.betula.calendar.EmbeddableComponentProperty;
import org.thespheres.betula.calendar.facade.StudentFacade;
import org.thespheres.betula.calendar.jms.StudentsNotificator;
import org.thespheres.betula.calendar.students.StudentEntity;
import org.thespheres.ical.CardComponentProperty;
import org.thespheres.ical.InvalidComponentException;
import org.thespheres.ical.Parameter;
import org.thespheres.ical.VCard;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class StudentFacadeImpl extends BaseComponentFacade<StudentEntity, StudentId> implements StudentFacade {

    @PersistenceContext(unitName = "studentsPU")
    private EntityManager em;
    @Inject
    protected StudentsNotificator studentsNotificator;

    public StudentFacadeImpl() {
        super(StudentEntity.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public VCard findVCard(StudentId id) {
        StudentEntity ce = find(id, LockModeType.OPTIMISTIC);
        if (ce == null) {
            return null;
        }
        return toVCard(ce);
    }

    @Override
    public Collection<VCard> findAllVCards() {
        return findAll(LockModeType.OPTIMISTIC).stream().map(e -> toVCard(e)).collect(Collectors.toSet());
    }

    @Override
    public Collection<VCard> findVCards(Collection<StudentId> students) {
        return students.stream().map(sid -> toVCard(find(sid, LockModeType.OPTIMISTIC))).collect(Collectors.toList());
    }

    @Override
    public Collection<VCard> findAllVCards(UnitId unit) {
        return findAll(LockModeType.OPTIMISTIC).stream().map(e -> toVCard(e)).collect(Collectors.toSet());
    }

    @Override
    public void create(StudentId id, String fn) {
        if (id == null || fn == null || fn.isEmpty()) {
            throw new IllegalArgumentException("StudentId and fullname must not be null or empty.");
        }
        StudentEntity se = find(id, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (se == null) {
            se = new StudentEntity(id);
            se.setFullname(fn);
            create(se);
            final StudentEvent evt = new StudentEvent(se.getStudentId(), StudentEvent.StudentEventType.ADD);
            studentsNotificator.notityConsumers(evt);
        } else if (!fn.equals(se.getFullname())) {
            se.setFullname(fn);
            edit(se);
            final StudentEvent evt = new StudentEvent(se.getStudentId(), StudentEvent.StudentEventType.CHANGE);
            studentsNotificator.notityConsumers(evt);
        }
    }

    @Override
    public void update(StudentId id, CardComponentProperty property) {
        boolean merge = false;
        final String name = property.getName();
        final String value = property.getValue();
        final StudentEntity se = find(id, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (se == null) {
            throw new NoSuchEntityException();
        }
        if (null != name) {
            switch (name) {
                case VCard.FN:
                    if (!value.equals(se.getFullname())) {
                        se.setFullname(value);
                        merge = true;
                    }
                    break;
                case VCard.N:
                    if (!value.equals(se.getNprop())) {
                        se.setNprop(value);
                        merge = true;
                    }
                    break;
                case VCard.GENDER:
                    if (!value.equals(se.getGender())) {
                        se.setGender(value);
                        merge = true;
                    }
                    break;
                case VCard.BDAY:
                    if (!value.equals(se.getBirthday())) {
                        se.setBirthday(value);
                        merge = true;
                    }
                    break;
                case VCard.BIRTHPLACE:
                    if (!value.equals(se.getBirthplace())) {
                        se.setBirthplace(value);
                        merge = true;
                    }
                    break;
            }
        }
        if (merge) {
            edit(se);
            final StudentEvent evt = new StudentEvent(se.getStudentId(), StudentEvent.StudentEventType.CHANGE);
            studentsNotificator.notityConsumers(evt);
        }
    }

    @Override
    public void remove(final StudentId student) {
        final StudentEntity se = find(student, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (se == null) {
            throw new NoSuchEntityException();
        }
        remove(se);
        final StudentEvent evt = new StudentEvent(se.getStudentId(), StudentEvent.StudentEventType.REMOVE);
        studentsNotificator.notityConsumers(evt);
    }

    protected VCard toVCard(StudentEntity se) {
        //Do not create VCard in StudentEntity --> access control decoration
        //TODO: cache readyVCards? WeakHashMap
        VCardBuilder vb = new VCardBuilder();
        try {
            vb.addProperty("FN", se.getFullname()).addProperty("N", se.getNprop()).addProperty("GENDER", se.getGender());
            vb.addProperty("BDAY", se.getBirthday()).addProperty("BIRTHPLACE", se.getBirthplace());
            //No lambdas in eclipselink!!!"!
            for (EmbeddableComponentProperty cpe : se.getProperties()) {
                cpe.toComponentProperty(vb);
            }
            StudentId studentId = se.getStudentId();
            vb.addProperty("X-STUDENT", Long.toString(studentId.getId()), new Parameter("x-authority", studentId.getAuthority()));
        } catch (InvalidComponentException invalidComponentException) {
        }
        return vb.toVCard();
    }
}
