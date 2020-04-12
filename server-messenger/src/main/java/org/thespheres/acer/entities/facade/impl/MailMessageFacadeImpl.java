/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.facade.impl;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.thespheres.acer.MessageId;
import org.thespheres.acer.entities.facade.ChannelFacade;
import org.thespheres.acer.entities.facade.MailMessageFacade;
import org.thespheres.acer.entities.BaseChannel;
import org.thespheres.acer.entities.DurableMessage;
import org.thespheres.acer.entities.MessageNotFoundException;
import org.thespheres.acer.entities.PatternChannel;
import org.thespheres.acer.entities.SigneeAction;
import org.thespheres.acer.entities.StaticChannel;
import org.thespheres.acer.entities.StudentAction;
import org.thespheres.acer.entities.StudentsChannel;
import org.thespheres.acer.entities.UnitChannel;
import org.thespheres.acer.entities.messages.tracking.MailStatusTracker;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.server.beans.ChannelsLocal;

/**
 *
 * @author boris.heithecker
 */
@Stateless
public class MailMessageFacadeImpl implements MailMessageFacade {

    @EJB
    private ChannelFacade facade;
    @PersistenceContext(unitName = "messagingPU")
    private EntityManager em;
    @EJB
    private ChannelsLocal channelsLocal;

//    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public MailStatusTracker sendMessageAsEmail(MessageId mid) {
        DurableMessage dm = facade.findMessage(mid, DurableMessage.class, LockModeType.READ);
        if (dm == null) {
            throw new MessageNotFoundException(mid);
        }
        BaseChannel bc = dm.getChannel();
        if (bc instanceof PatternChannel) {
            final PatternChannel pc = (PatternChannel) bc;
            channelsLocal.sendEmail(pc.getName(), pc.getDefaultDisplayName(), dm.getBaseText());
        } else if (bc instanceof StaticChannel) {
            final StaticChannel sc = (StaticChannel) bc;
            final Signee[] recipients = sc.getSignees().stream()
                    .filter(sa -> sa.getAction().equals(SigneeAction.Action.INCLUDE))
                    .map(SigneeAction::getSignee)
                    .toArray(Signee[]::new);
            channelsLocal.sendEmail(sc.getDisplayName(), dm.getBaseText(), recipients);
        } else if (bc instanceof UnitChannel) {
            final UnitId unit = ((UnitChannel) bc).getUnit();
            channelsLocal.sendEmail(unit, dm.getBaseText());
        } else if (bc instanceof StudentsChannel) {
            final StudentsChannel sc = (StudentsChannel) bc;
            final StudentId[] studs = sc.getStudentAction().stream()
                    .filter(sa -> sa.getAction().equals(StudentAction.Action.INCLUDE))
                    .map(StudentAction::getStudentId)
                    .toArray(StudentId[]::new);
            channelsLocal.sendEmail(studs, dm.getBaseText());
        }
        MailStatusTracker ret = new MailStatusTracker();
        ret.getMessages().add(dm);
        dm.getTrackers().add(ret);
        em.persist(ret);
        em.merge(dm);
        return ret;
    }

    @Override
    public MailStatusTracker getTracker(long id) {
        MailStatusTracker mst = em.find(MailStatusTracker.class, id, LockModeType.OPTIMISTIC);
        if (mst != null) {
            return mst;
        }
        throw new EntityNotFoundException();
    }

}
