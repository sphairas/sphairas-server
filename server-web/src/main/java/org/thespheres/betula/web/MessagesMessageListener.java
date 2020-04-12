/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.thespheres.acer.beans.MessageEvent;

/**
 *
 * @author boris.heithecker
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/messages-topic"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic")
})
public class MessagesMessageListener implements MessageListener {

    @EJB
    private EventDispatch eventDispatch;

    @Override
    public void onMessage(Message msg) {
        try {
            if (msg.isBodyAssignableTo(MessageEvent.class)) {
                MessageEvent event = msg.getBody(MessageEvent.class);
                if (event.getSource() != null) {
                    eventDispatch.onMessageEvent(event);
                }
            }
        } catch (JMSException | ClassCastException ex) {
        }
    }
}
