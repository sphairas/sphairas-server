/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.saccess;

import java.util.Collections;
import java.util.List;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.LockModeType;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.TermTextTargetAssessmentEntity;
import org.thespheres.betula.entities.facade.TextTargetDocumentFacade;
import org.thespheres.betula.entities.facade.impl.SigneeFacadeImpl;

/**
 *
 * @author boris.heithecker@gmx.net
 */
@Decorator
public abstract class TextTargetDocumentSigneeDecorator implements TextTargetDocumentFacade {

    @Inject
    @Delegate
    private TextTargetDocumentFacade delegate;
    @EJB
    protected SigneeFacadeImpl login;

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
    public List<TermTextTargetAssessmentEntity> findAll(final LockModeType lmt) {
        final SessionContext ctx = getDecoratedSessionContext();
        if (ctx.isCallerInRole("unitadmin")) {
            return delegate.findAll(lmt);
        } else if (ctx.isCallerInRole("signee")) {
            final SigneeEntity signee = login.getCurrent();
            if (signee != null) {
                return delegate.findAll(signee, lmt);
            }
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<TermTextTargetAssessmentEntity> findAll(final SigneeEntity signee, final LockModeType lmt) {
        if (getDecoratedSessionContext().isCallerInRole("unitadmin")
                || getDecoratedSessionContext().isCallerInRole("remoteadmin")
                || (signee != null && login.getCurrent().equals(signee))) {
            return delegate.findAll(signee, lmt);
        }
        throw new SigneeEJBAccessException("TermGradeTargetDocumentSigneeDecorator.findAll", login.getSigneePrincipal(false));
    }

}
