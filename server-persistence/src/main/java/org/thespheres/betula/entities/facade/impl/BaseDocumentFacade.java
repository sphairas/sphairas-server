/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade.impl;

import java.util.List;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.ejb.SessionContext;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.entities.BaseDocumentEntity;
import org.thespheres.betula.entities.jmsimpl.DocumentsNotificator;
import org.thespheres.betula.server.beans.SigneeLocal;

/**
 *
 * @author boris.heithecker
 * @param <T>
 */
abstract class BaseDocumentFacade<T extends BaseDocumentEntity> {

    @PersistenceContext(unitName = "betula0")
    protected EntityManager em;
    @Resource
    protected SessionContext session;
    @EJB
    protected SigneeLocal login;
    @Inject
    protected DocumentsNotificator documentsNotificator;
    @Inject
    protected DocumentsModel documentsModel;
    protected final Class<T> entityClass;

    protected BaseDocumentFacade(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public void edit(T entity) {
        em.merge(entity);
    }

    public void remove(T entity) {
        em.remove(em.merge(entity));
    }

    public BaseDocumentEntity findBaseDocumentEntity(DocumentId id, LockModeType lmt) {
        return em.find(BaseDocumentEntity.class, id, lmt);
    }

    protected T findEntity(DocumentId id, LockModeType lmt) {
        return em.find(entityClass, id, lmt);
    }

    protected List<T> findAllEntities(LockModeType lmt, Class type) {
        if (!(session.isCallerInRole("unitadmin") || session.isCallerInRole("remoteadmin"))) {
            throw new EJBAccessException();
        }
        if (!entityClass.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Type mismatch");
        }
        javax.persistence.criteria.CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
        cq.select(cq.from(type));
        return em.createQuery(cq).setLockMode(lmt).getResultList();
    }

}
