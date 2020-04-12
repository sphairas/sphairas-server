/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.facade.impl;

import java.util.Collection;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.thespheres.acer.MessageId;
import org.thespheres.acer.beans.ChannelEvent;
import org.thespheres.acer.beans.MessageEvent;
import org.thespheres.acer.beans.MessageEvent.MessageEventType;
import org.thespheres.acer.entities.local.MessagesNotificator;
import org.thespheres.acer.entities.local.Naming;
import org.thespheres.acer.entities.facade.ChannelFacade;
import org.thespheres.acer.entities.BaseChannel;
import org.thespheres.acer.entities.BaseMessage;
import org.thespheres.acer.entities.DurableMessage;
import org.thespheres.acer.entities.DynamicChannel;
import org.thespheres.acer.entities.StaticChannel;
import org.thespheres.acer.entities.StudentsChannel;
import org.thespheres.acer.entities.UnitChannel;
import org.thespheres.acer.entities.config.SystemProperties;
import org.thespheres.betula.Identity;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.server.beans.SigneeLocal;

/**
 *
 * @author boris.heithecker
 */
@Stateless
public class ChannelFacadeImpl extends AbstractFacade<BaseChannel> implements ChannelFacade {

    @EJB
    private MessagesNotificator messagesNotificator;
    @EJB
    private Naming naming;
    @EJB
    private SigneeLocal signees;
    @PersistenceContext(unitName = "messagingPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ChannelFacadeImpl() {
        super(BaseChannel.class);
    }

    @Override
    public Collection<Signee> getCurrentSignees() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void create(BaseChannel channel) {
        super.create(channel);
        messagesNotificator.notityChannelEvent(new ChannelEvent(channel.getName(), ChannelEvent.ChannelEventType.CREATED));
    }

    @Override
    public void edit(BaseChannel channel) {
        super.edit(channel);
    }

    @Override
    public void remove(BaseChannel channel) {
        super.remove(channel);
        messagesNotificator.notityChannelEvent(new ChannelEvent(channel.getName(), ChannelEvent.ChannelEventType.REMOVE));
    }

    @Override
    public void update(DurableMessage dm, String messageText, int priority, boolean encoded) {
        dm.setBaseText(messageText);
        dm.setPriority(priority);
        setEncoded(encoded, dm);
        edit(dm);
    }

    @Override
    public DurableMessage publish(String channel, String text, int priority, boolean encoded) {
        final BaseChannel bc = find(channel, BaseChannel.class, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (bc == null) {
            throw new IllegalArgumentException("Channel " + channel + " does not exist.");
        }
        final Signee sig = signees.getSigneePrincipal(true);
        DurableMessage dm = new DurableMessage(SystemProperties.messagesAuthority(), bc, sig, text);
        em.persist(dm);
        dm.setPriority(priority);
        setEncoded(encoded, dm);
        bc.getMessages().add(dm);
        dm = em.merge(dm);
        em.merge(bc);
        messagesNotificator.notityMessageEvent(new MessageEvent(dm.getId(), MessageEventType.PUBLISH));
        return dm;
    }

    private void setEncoded(boolean encoded, DurableMessage dm) {
        if (encoded) {
            dm.getProperties().setProperty("encoding", "base64");
        } else {
            dm.getProperties().remove("encoding");
        }
    }

    @Override
    public Collection<StaticChannel> getStaticChannels(LockModeType lmt) {
        return findAll(StaticChannel.class, lmt);
    }

    @Override
    public Collection<UnitChannel> getUnitChannels(boolean pu, LockModeType lmt) {
        return findAll(UnitChannel.class, lmt).stream().filter(uc -> uc.isisPrimaryUnitChannel() == pu).collect(Collectors.toSet());
    }

    @Override
    public Collection<StudentsChannel> getStudentsChannels(LockModeType lmt) {
        return findAll(StudentsChannel.class, lmt);
    }

    @Override
    public <C extends BaseChannel> C find(String name, Class<C> type, LockModeType lmt) {
        return super.find(name, type, lmt);
    }

    @Override
    public <M extends BaseMessage> M findMessage(MessageId mid, Class<M> type, LockModeType lmt) {
        return getEntityManager().find(type, mid, lmt);
    }

    @Override
    public void edit(BaseMessage message) {
        getEntityManager().merge(message);
        messagesNotificator.notityMessageEvent(new MessageEvent(message.getId(), MessageEventType.PUBLISH));
    }

    @Override
    public void delete(MessageId message) {
        BaseMessage bm = em.find(BaseMessage.class, message);
        BaseChannel c = bm.getChannel();
        c.getMessages().remove(bm);
        em.remove(bm);
//        em.merge(c);
        if (c.getMessages().contains(bm)) {
            throw new EJBException();
        }
        messagesNotificator.notityMessageEvent(new MessageEvent(message, MessageEventType.DELETE));
    }

    @Override
    public String getCurrentDisplayName(DynamicChannel channel) {
        Identity rid = channel.getResolvableIdentity();
        if (rid == null) {
            return channel.getDefaultDisplayName();
        }
        String np = channel.getNamingProvider();
        String ts = channel.getTermScheduleProvider();
        return naming.resolveDisplayName(rid, np, ts);
    }

}
