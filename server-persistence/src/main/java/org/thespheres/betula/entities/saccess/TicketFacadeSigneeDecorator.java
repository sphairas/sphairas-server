/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.saccess;

import java.util.List;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.LockModeType;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.entities.BaseTicketEntity;
import org.thespheres.betula.entities.EmbeddableSigneeInfo;
import org.thespheres.betula.entities.GradeTargetAssessmentEntity;
import org.thespheres.betula.entities.facade.GradeTargetDocumentFacade;
import org.thespheres.betula.entities.facade.TicketFacade;
import org.thespheres.betula.server.beans.SigneeLocal;

/**
 *
 * @author boris.heithecker@gmx.net
 */
@Decorator
public abstract class TicketFacadeSigneeDecorator implements TicketFacade {

    @Inject
    @Delegate
    private TicketFacade delegate;
    @EJB
    protected SigneeLocal login;
    @Inject
    private GradeTargetDocumentFacade targets;

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
    public List<BaseTicketEntity> getTickets(DocumentId targetDoc, TermId term, StudentId student, String signeeType) {
        final Signee current = login.getSigneePrincipal(true);
        if (getDecoratedSessionContext().isCallerInRole("unitadmin")) {
            return delegate.getTickets(targetDoc, term, student, signeeType);
        } else {
            final GradeTargetAssessmentEntity<?> ret = targets.find(targetDoc, LockModeType.OPTIMISTIC);
            if (ret != null && signeeType != null) {
                final EmbeddableSigneeInfo si = ret.getEmbeddableSignees().get(signeeType);
                if (si != null && si.getSignee().equals(current)) {
                    return delegate.getTickets(targetDoc, term, student, signeeType);
                }
            }
        }
        throw new SigneeEJBAccessException(targetDoc, current);
    }

}
