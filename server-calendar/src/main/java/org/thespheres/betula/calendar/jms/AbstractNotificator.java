/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.jms;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Topic;
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
