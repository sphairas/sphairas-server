/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.facade.impl;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import org.thespheres.betula.Identity;

/**
 *
 * @author boris.heithecker
 * @param <E>
 * @param <I>
 */
public abstract class BaseComponentFacade<E, I extends Identity> {

    protected final Class<E> entityClass;

    protected BaseComponentFacade(Class<E> entityClass) {
        this.entityClass = entityClass;
    }

    protected abstract EntityManager getEntityManager();

    protected void create(E entity) {
        getEntityManager().persist(entity);
    }

    protected void edit(E entity) {
        getEntityManager().merge(entity);
    }

    protected void remove(E entity) {
        getEntityManager().remove(getEntityManager().merge(entity));
    }

    protected E find(I id, LockModeType lmt) {
        return getEntityManager().find(entityClass, id, lmt);
    }

    protected List<E> findAll(LockModeType lmt) {
        javax.persistence.criteria.CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
        cq.select(cq.from(entityClass));
        return getEntityManager().createQuery(cq).setLockMode(lmt).getResultList();
    }

}
