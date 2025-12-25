/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.local;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Topic;
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
