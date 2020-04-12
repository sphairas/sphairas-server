/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.saccess;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.ejb.SessionContext;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.entities.EmbeddableSigneeInfo;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.UnitDocumentEntity;
import org.thespheres.betula.entities.facade.GradeTargetDocumentFacade;
import org.thespheres.betula.server.beans.SigneeLocal;
import org.thespheres.betula.entities.facade.UnitDocumentFacade;
import org.thespheres.betula.services.ws.CommonDocuments;

/**
 *
 * @author boris.heithecker
 */
@Decorator
public abstract class UnitDocumentSigneeDecorator implements UnitDocumentFacade {

    @Inject
    @Delegate
    private UnitDocumentFacade delegate;
    @PersistenceContext(unitName = "betula0")
    protected EntityManager em;
    @EJB
    protected SigneeLocal login;
    @Inject
    private GradeTargetDocumentFacade targets;
    @Default
    @Inject
    private DocumentsModel docModel;
    @Inject
    private CommonDocuments cd;

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
    public List<UnitDocumentEntity> findAll(LockModeType lmt) {
        if (getDecoratedSessionContext().isCallerInRole("unitadmin") || getDecoratedSessionContext().isCallerInRole("remoteadmin")) {
            return delegate.findAll(lmt);
        }

        return targets.findAll(LockModeType.OPTIMISTIC, TermGradeTargetAssessmentEntity.class).stream()
                .flatMap(gtae -> (Stream<UnitDocumentEntity>) gtae.getUnitDocs().stream())
                .distinct()
                .collect(Collectors.toList());

        //This was too much for derby....
//        CriteriaQuery<UnitDocumentEntity> cq = em.getCriteriaBuilder().createQuery(UnitDocumentEntity.class);
//        Root<UnitDocumentEntity> pet = cq.from(UnitDocumentEntity.class);
//        List<Predicate> list = new ArrayList<>();
////        if (!doAsAdmin) {
//        Signee signee = login.getSigneePrincipal();
//        CriteriaBuilder cb = em.getCriteriaBuilder();
//        list.add(cb.equal(pet.join("targets").join("signeeInfoentries").get("type"), "entitled.signee"));
//        list.add(cb.equal(pet.join("targets").join("signeeInfoentries").get("signee").get("prefix"), signee.getPrefix()));
//        list.add(cb.equal(pet.join("targets").join("signeeInfoentries").get("signee").get("suffix"), signee.getSuffix()));
//        list.add(cb.equal(pet.join("targets").join("signeeInfoentries").get("signee").get("alias"), signee.isAlias()));
////        }
//        cq.select(pet).distinct(true).where(list.stream().toArray((s) -> new Predicate[s]));
//        return em.createQuery(cq).setLockMode(lmt).getResultList();
    }

    @Override
    public UnitId getPrimaryUnit(DocumentId kldoc, Signee signee) {
        if (signee == null) {
            throw new EJBAccessException("Signee cannot be null.");
        }
        Signee loggedIn;
        if ((((loggedIn = login.getSigneePrincipal(false)) != null) && loggedIn.equals(signee)) || getDecoratedSessionContext().isCallerInRole("unitadmin")) {
            return delegate.getPrimaryUnit(kldoc, signee);
        }
        return null;
    }

    @Override
    public UnitDocumentEntity find(DocumentId id, LockModeType lmt) {
        final UnitDocumentEntity ret = delegate.find(id, lmt);
        if (ret != null && !getDecoratedSessionContext().isCallerInRole("unitadmin")) {
            final Signee current = login.getSigneePrincipal(true);
            if (current != null) {
                final DocumentId ct = cd.forName(CommonDocuments.PRIMARY_UNIT_HEAD_TEACHERS_DOCID);
                final UnitId pu = ct != null ? delegate.getPrimaryUnit(ct, current) : null;
                boolean permit = (pu != null && pu.equals(docModel.convertToUnitId(id)));
                //TODO: respect unlinked
                permit = permit | ret.getTargetAssessments().stream()
                        .flatMap(btae -> (Stream<EmbeddableSigneeInfo>) btae.getEmbeddableSignees().values().stream())
                        .anyMatch(esi -> esi.getSignee().equals(current) && isSigneeTypePermitted(esi.getSigneeType()));
                if (!permit) {
                    throw new SigneeEJBAccessException(id, current);
                }
            }
        }
        return ret;
    }

    private boolean isSigneeTypePermitted(String type) {
        return true;
    }

    @Override
    public void setCommonName(DocumentId d, UnitId uid, String cn) {
        final DocumentId cNames = cd.forName(CommonDocuments.COMMON_NAMES_DOCID);
        if ((cNames != null && !d.equals(cNames)) || !uid.getId().startsWith("kgs-ag")) {
            throw new IllegalArgumentException();
        }
        delegate.setCommonName(d, uid, cn);
    }

}
