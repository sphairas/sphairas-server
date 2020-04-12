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
import org.thespheres.betula.services.jms.TicketEvent;

/**
 *
 * @author boris.heithecker
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/tickets-topic"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic")
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
                TicketEvent event = msg.getBody(TicketEvent.class);
                if (event.getSource() != null) {
                    multiTargetAssessmentEventService.onTicketEvent(event);
                }
            }
        } catch (JMSException | ClassCastException ex) {
        }
    }

}
