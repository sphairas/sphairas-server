/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.facade.impl;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

/**
 *
 * @author boris.heithecker
 * @param <T>
 */
abstract class AbstractFacade<T> {

    protected final Class<T> entityClass;

    protected AbstractFacade(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected abstract EntityManager getEntityManager();

    protected void create(T entity) {
        getEntityManager().persist(entity);
    }

    protected void edit(T entity) {
        getEntityManager().merge(entity);
    }

    protected void remove(T entity) {
        getEntityManager().remove(getEntityManager().merge(entity));
    }

    protected <C extends T> C find(Object id, Class<C> type, LockModeType lmt) {
        return getEntityManager().find(type, id, lmt);
    }

    protected <C extends T> List<C> findAll(Class<C> type, LockModeType lmt) {
        javax.persistence.criteria.CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
        cq.select(cq.from(type));
        return getEntityManager().createQuery(cq).setLockMode(lmt).getResultList();
    }

    protected List<T> findRange(int[] range) {
        javax.persistence.criteria.CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
        cq.select(cq.from(entityClass));
        javax.persistence.Query q = getEntityManager().createQuery(cq);
        q.setMaxResults(range[1] - range[0] + 1);
        q.setFirstResult(range[0]);
        return q.getResultList();
    }

    protected int count() {
        javax.persistence.criteria.CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
        javax.persistence.criteria.Root<T> rt = cq.from(entityClass);
        cq.select(getEntityManager().getCriteriaBuilder().count(rt));
        javax.persistence.Query q = getEntityManager().createQuery(cq);
        return ((Long) q.getSingleResult()).intValue();
    }

}
