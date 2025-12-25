/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.jms;

import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Topic;
import org.thespheres.betula.services.jms.AbstractJMSEvent;

/**
 *
 * @author boris.heithecker
 */
public abstract class AbstractNotificator {

//    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.MANDATORY)//ZGN
    public void notityConsumers(AbstractJMSEvent event) {
        final JMSProducer messageProducer =  getJMSContext().createProducer();
        messageProducer.send(getTopic(), event);
    }

    protected abstract JMSContext getJMSContext();

    protected abstract Topic getTopic();

}
