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
import org.thespheres.betula.services.jms.TicketEvent;

/**
 *
 * @author boris.heithecker
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/tickets-topic"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic")
})
public class TicketsMessageListener implements MessageListener {

    @EJB
    private EventDispatch multiTargetAssessmentEventService;

    public TicketsMessageListener() {
    }

    @Override
    public void onMessage(Message msg) {
        try {
            if (msg.isBodyAssignableTo(TicketEvent.class)) {
                final TicketEvent event = msg.getBody(TicketEvent.class);
                if (event != null && event.getSource() != null) {
                    multiTargetAssessmentEventService.onTicketEvent(event);
                }
            }
        } catch (JMSException | ClassCastException ex) {
        }
    }

}
