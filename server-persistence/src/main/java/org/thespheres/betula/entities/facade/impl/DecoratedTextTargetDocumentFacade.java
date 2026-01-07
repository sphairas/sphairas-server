/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade.impl;

import java.util.Collections;
import java.util.List;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.persistence.LockModeType;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.TermTextTargetAssessmentEntity;
import org.thespheres.betula.entities.facade.TextTargetDocumentFacade;

/**
 *
 * @author boris.heithecker@gmx.net
 */
@LocalBean
@Stateless
public class DecoratedTextTargetDocumentFacade extends TextTargetDocumentFacadeImpl implements TextTargetDocumentFacade {

//    @Inject
//    @Delegate
//    private TextTargetDocumentFacade delegate;
    @EJB
    protected SigneeFacadeImpl signees;

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
            return super.findAll(lmt);
        } else if (ctx.isCallerInRole("signee")) {
            final SigneeEntity signee = signees.getCurrent();
            if (signee != null) {
                return super.findAll(signee, lmt);
            }
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<TermTextTargetAssessmentEntity> findAll(final SigneeEntity signee, final LockModeType lmt) {
        if (getDecoratedSessionContext().isCallerInRole("unitadmin")
                || getDecoratedSessionContext().isCallerInRole("remoteadmin")
                || (signee != null && signees.getCurrent().equals(signee))) {
            return super.findAll(signee, lmt);
        }
        throw new SigneeEJBAccessException("TermGradeTargetDocumentSigneeDecorator.findAll", signees.getSigneePrincipal(false));
    }

}
