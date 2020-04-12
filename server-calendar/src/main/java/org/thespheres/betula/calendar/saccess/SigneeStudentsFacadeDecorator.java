/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.saccess;

import java.util.Collection;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.ejb.SessionContext;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
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
@Decorator
public abstract class SigneeStudentsFacadeDecorator implements StudentFacade {

    @Inject
    @Delegate
    private StudentFacade delegate;
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
        return delegate.findVCards(studs);
    }

    @Override
    public Collection<VCard> findAllVCards() {
        if (getDecoratedSessionContext().isCallerInRole("unitadmin")) {
            return delegate.findAllVCards();
        }
        Collection<StudentId> students = getFastTargetDocuments2().getStudents();
        return delegate.findVCards(students);
    }

    @Override
    public void remove(StudentId student) {
        final SessionContext ctx = getDecoratedSessionContext();
        if (!ctx.isCallerInRole("unitadmin")) {
            throw new IllegalStudentAccessException(student, ctx.getCallerPrincipal());
        }
        delegate.remove(student);
    }

}
