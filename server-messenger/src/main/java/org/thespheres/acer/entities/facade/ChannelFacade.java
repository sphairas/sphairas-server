/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.facade;

import java.util.Collection;
import javax.ejb.Local;
import javax.persistence.LockModeType;
import org.thespheres.acer.MessageId;
import org.thespheres.acer.entities.BaseChannel;
import org.thespheres.acer.entities.BaseMessage;
import org.thespheres.acer.entities.DurableMessage;
import org.thespheres.acer.entities.DynamicChannel;
import org.thespheres.acer.entities.StaticChannel;
import org.thespheres.acer.entities.StudentsChannel;
import org.thespheres.acer.entities.UnitChannel;
import org.thespheres.betula.document.Signee;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface ChannelFacade {

    public void create(BaseChannel channel);

//    void edit(DynamicChannel dynamicChannel);
//
//    void remove(DynamicChannel dynamicChannel);
    public <C extends BaseChannel> C find(String name, Class<C> type, LockModeType lmt);

//    List<DynamicChannel> findAll();
//
//    List<DynamicChannel> findRange(int[] range);
//
//    int count();
    public Collection<Signee> getCurrentSignees();

    public String getCurrentDisplayName(DynamicChannel channel);

    public DurableMessage publish(String channel, String text, int priority, boolean encoded);

    public <M extends BaseMessage> M findMessage(MessageId mid, Class<M> type, LockModeType lmt);

    public void edit(BaseMessage message);

    public void delete(MessageId message);

    public void update(DurableMessage dm, String messageText, int i, boolean confidential);

    public Collection<StaticChannel> getStaticChannels(LockModeType lmt);

    public Collection<UnitChannel> getUnitChannels(boolean pu, LockModeType lmt);

    public Collection<StudentsChannel> getStudentsChannels(LockModeType lmt);

    public void remove(BaseChannel bc);

    public void edit(BaseChannel channel);

}
