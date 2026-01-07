/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.facade.impl;

import java.util.Collection;
import jakarta.ejb.LocalBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.calendar.facade.StudentFacade;
import org.thespheres.betula.server.beans.FastTargetDocuments2;
import org.thespheres.betula.server.beans.annot.DocumentsRequest;
import org.thespheres.ical.VCard;

/**
 *
 * @author boris.heithecker
 */
//@Decorator
@LocalBean
@Stateless
public class DecoratedStudentFacade extends StudentFacadeImpl implements StudentFacade {

//    @Inject
//    @Delegate
//    private StudentFacade delegate;
//    @DocumentsSession
//    @Inject
//    private Instance<FastTargetDocuments2> ftd2SessionInstance;
    @DocumentsRequest
    @Inject
    private Instance<FastTargetDocuments2> ftd2RequestInstance;

    private FastTargetDocuments2 getFastTargetDocuments2() {
        return ftd2RequestInstance.get();
    }

    private SessionContext getDecoratedSessionContext() {
        InitialContext ic;
        try {
            ic = new InitialContext();
            return (SessionContext) ic.lookup("java:comp/EJBContext");
        } catch (NamingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Collection<VCard> findAllVCards(UnitId unit) {
        Collection<StudentId> studs = getFastTargetDocuments2().getStudents(unit, null);
        return super.findVCards(studs);
    }

    @Override
    public Collection<VCard> findAllVCards() {
        if (getDecoratedSessionContext().isCallerInRole("unitadmin")) {
            return super.findAllVCards();
        }
        Collection<StudentId> students = getFastTargetDocuments2().getStudents();
        return super.findVCards(students);
    }

    @Override
    public void remove(StudentId student) {
        final SessionContext ctx = getDecoratedSessionContext();
        if (!ctx.isCallerInRole("unitadmin")) {
            throw new IllegalStudentAccessException(student, ctx.getCallerPrincipal());
        }
        super.remove(student);
    }

}
