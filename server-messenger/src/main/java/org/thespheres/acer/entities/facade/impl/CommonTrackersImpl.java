/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.facade.impl;

import java.sql.Timestamp;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.thespheres.acer.MessageId;
import org.thespheres.acer.entities.facade.CommonTrackers;
import org.thespheres.acer.entities.facade.MessageTrackingException;
import org.thespheres.acer.entities.BaseMessage;
import org.thespheres.acer.entities.MessageNotFoundException;
import org.thespheres.acer.entities.messages.tracking.PrivateStatusTracker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.server.beans.SigneeLocal;
import org.thespheres.betula.util.CollectionUtil;

/**
 *
 * @author boris.heithecker
 */
@Stateless
public class CommonTrackersImpl extends AbstractFacade<BaseMessage> implements CommonTrackers {

    @EJB
    private SigneeLocal signees;
    @PersistenceContext(unitName = "messagingPU")
    private EntityManager em;

    public CommonTrackersImpl() {
        super(BaseMessage.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public void setUpdated(MessageId mid, Timestamp update) throws MessageTrackingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Timestamp getUpdated(MessageId mid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private PrivateStatusTracker findPrivateStatusTracker(MessageId mid, LockModeType lmt) {
        final BaseMessage bm = find(mid, BaseMessage.class, lmt);
        if (bm == null) {
            throw new MessageNotFoundException(mid);
        }
        final Signee signee = signees.getSigneePrincipal(true);
        return bm.getTrackers().stream()
                .filter(PrivateStatusTracker.class::isInstance)
                .collect(CollectionUtil.singleton())
                .map(PrivateStatusTracker.class::cast)
                .orElseGet(() -> {
                    PrivateStatusTracker ret = new PrivateStatusTracker(signee);
                    ret.getMessages().add(bm);
                    bm.getTrackers().add(ret);
                    getEntityManager().persist(ret);
                    edit(bm);
                    return ret;
                });

    }

    @Override
    public Timestamp getPrivateStatusRead(MessageId mid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPrivateStatusRead(MessageId mid, Timestamp read) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getPrivateStatus(MessageId mid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPrivateStatus(MessageId mid, int privateStatus) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean getPrivateStatusCollapsed(MessageId mid) {
        PrivateStatusTracker tr = findPrivateStatusTracker(mid, LockModeType.OPTIMISTIC);
        return tr.isCollapsed();
    }

    @Override
    public void setPrivateStatusCollapsed(MessageId mid, boolean collapsed) {
        PrivateStatusTracker tr = findPrivateStatusTracker(mid, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        tr.setCollapsed(collapsed);
        getEntityManager().merge(tr);
    }

}
