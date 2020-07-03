/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.saccess;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.thespheres.betula.Identity;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.entities.GradeTargetAssessmentEntity;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.facade.impl.SigneeFacadeImpl;
import org.thespheres.betula.entities.facade.GradeTargetDocumentFacade;
import org.thespheres.betula.server.beans.config.CommonAppProperties;
import org.thespheres.betula.services.LocalProperties;

/**
 *
 * @author boris.heithecker
 */
@Decorator
public abstract class TermGradeTargetDocumentSigneeDecorator implements GradeTargetDocumentFacade {

    @Inject
    @Delegate
    private GradeTargetDocumentFacade delegate;
    @PersistenceContext(unitName = "betula0")
    protected EntityManager em;
    @EJB
    protected SigneeFacadeImpl login;
    @Inject
    @SessionScoped
    private Provider<LocalProperties> properties;

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
    public <T extends GradeTargetAssessmentEntity> List<T> findAll(LockModeType lmt, Class<T> type) {
        final SessionContext ctx = getDecoratedSessionContext();
        if (ctx.isCallerInRole("unitadmin")) {
            return delegate.findAll(lmt, type);
        } else if (ctx.isCallerInRole("signee")) {
            final SigneeEntity signee = login.getCurrent();
            if (signee != null) {
                return delegate.findAll(signee, type, lmt);
            }
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public <T extends GradeTargetAssessmentEntity> List<T> findAll(SigneeEntity signee, Class<T> type, LockModeType lmt) {
        if (getDecoratedSessionContext().isCallerInRole("unitadmin")
                || getDecoratedSessionContext().isCallerInRole("remoteadmin")
                || (signee != null && login.getCurrent().equals(signee))) {
            return delegate.findAll(signee, type, lmt);
        }
        throw new SigneeEJBAccessException("TermGradeTargetDocumentSigneeDecorator.findAll", login.getSigneePrincipal(false));
    }
    //Do not delete: serves as sample for getting user transaction
//    public void persist(Object object) {
//        /* Add this to the deployment descriptor of this module (e.g. web.xml, ejb-jar.xml):
//         * <persistence-context-ref>
//         * <persistence-context-ref-name>persistence/LogicalName</persistence-context-ref-name>
//         * <persistence-unit-name>betula0</persistence-unit-name>
//         * </persistence-context-ref>
//         * <resource-ref>
//         * <res-ref-name>UserTransaction</res-ref-name>
//         * <res-type>javax.transaction.UserTransaction</res-type>
//         * <res-auth>Container</res-auth>
//         * </resource-ref> */
//        try {
//            Context ctx = new InitialContext();
//            UserTransaction utx = (UserTransaction) ctx.lookup("java:comp/env/UserTransaction");
//            utx.begin();
//            em.joinTransaction();
//            em.persist(object);
//            utx.commit();
//        } catch (Exception e) {
//            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", e);
//            throw new RuntimeException(e);
//        }
//    }

    @Override
    public <I extends Identity> boolean submit(DocumentId id, StudentId student, I gradeId, Grade grade, Timestamp ts) {
        if (!getDecoratedSessionContext().isCallerInRole("unitadmin") && !getDecoratedSessionContext().isCallerInRole("remoteadmin")) {
            final GradeTargetAssessmentEntity target = delegate.find(id, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            if (grade != null) {
                if (grade.getConvention().equals(target.getPreferredConvention()) || isPermittedExtra(grade)) {
                    return delegate.submit(id, student, gradeId, grade, ts);
                }
            }
            throw new IllegalSubmitException(target.getDocumentId(), login.getSigneePrincipal(false), grade);
        }
        return delegate.submit(id, student, gradeId, grade, ts);
    }

    private boolean isPermittedExtra(final Grade g) {
        final String cv = properties.get().getProperty(CommonAppProperties.PROP_SIGNEES_EXTRA_CONVENTIONS_PERMITTED);
        return Optional.ofNullable(cv)
                .map(v -> v.split(","))
                .map(Arrays::stream)
                .orElse(Stream.empty())
                .map(String::trim)
                .anyMatch(g.getConvention()::equals);
    }
}
