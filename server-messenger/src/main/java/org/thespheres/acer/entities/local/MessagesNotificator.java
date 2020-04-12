/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.local;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Topic;
import org.thespheres.acer.beans.ChannelEvent;
import org.thespheres.acer.beans.MessageEvent;

/**
 *
 * @author boris.heithecker
 */
@Stateless
public class MessagesNotificator {

    @Resource(mappedName = "jms/messages-topic")
    private Topic messages_topic;
    @Inject
    @JMSConnectionFactory("jms/message-topic-factory")
    private JMSContext context;

    @TransactionAttribute(TransactionAttributeType.MANDATORY)//ZGN
    public void notityMessageEvent(MessageEvent event) {
        JMSProducer messageProducer = getJMSContext().createProducer();
        messageProducer.send(getTopic(), event);
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)//ZGN
    public void notityChannelEvent(ChannelEvent event) {
        JMSProducer messageProducer = getJMSContext().createProducer();
        messageProducer.send(getTopic(), event);
    }

    protected Topic getTopic() {
        return messages_topic;
    }

    protected JMSContext getJMSContext() {
        return context;
    }
}
