/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import org.thespheres.acer.beans.MessageEvent;

/**
 *
 * @author boris.heithecker
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/messages-topic"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic")
})
public class MessagesMessageListener implements MessageListener {

    @EJB
    private EventDispatch eventDispatch;

    @Override
    public void onMessage(Message msg) {
        try {
            if (msg.isBodyAssignableTo(MessageEvent.class)) {
                final MessageEvent event = msg.getBody(MessageEvent.class);
                if (event != null && event.getSource() != null) {
                    eventDispatch.onMessageEvent(event);
                }
            }
        } catch (JMSException | ClassCastException ex) {
        }
    }
}
