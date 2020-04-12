/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade.impl;

import java.util.Set;
import java.util.stream.Collectors;
import org.thespheres.betula.entities.facade.DocumentsFacade;
import javax.annotation.security.RolesAllowed;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.LockModeType;
import javax.persistence.TemporalType;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.entities.BaseDocumentEntity;
import org.thespheres.betula.entities.DocumentIdWrapper;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class DocumentsFacadeImpl extends BaseDocumentFacade<BaseDocumentEntity> implements DocumentsFacade {

    static final String ALL_QUERY = "SELECT DISTINCT NEW org.thespheres.betula.entities.DocumentIdWrapper(e.id, e.authority, e.version) FROM BaseDocumentEntity e";
    static final String EXPIRY_QUERY = "SELECT DISTINCT NEW org.thespheres.betula.entities.DocumentIdWrapper(e.id, e.authority, e.version) FROM BaseDocumentEntity e "
            + "WHERE e.expirationTime < :dateTime";

    public DocumentsFacadeImpl() {
        super(BaseDocumentEntity.class);
    }

    @RolesAllowed("unitadmin")
    @Override
    public Set<DocumentId> findAll() {
        return em.createQuery(ALL_QUERY, DocumentIdWrapper.class)
                .setLockMode(LockModeType.OPTIMISTIC)
                .getResultList().stream()
                .map(DocumentIdWrapper::unwrap)
                .collect(Collectors.toSet());
    }

    //Must be allowed for all because of timer
    @Override
    public Set<DocumentId> findAllExpired(long before) {
        return em.createQuery(EXPIRY_QUERY, DocumentIdWrapper.class)
                .setLockMode(LockModeType.OPTIMISTIC)
                .setParameter("dateTime", new java.sql.Timestamp(before), TemporalType.TIMESTAMP)
                .getResultList().stream()
                .map(DocumentIdWrapper::unwrap)
                .collect(Collectors.toSet());
    }

}
