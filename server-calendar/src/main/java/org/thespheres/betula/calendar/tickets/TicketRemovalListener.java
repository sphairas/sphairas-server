/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.tickets;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.services.jms.TicketEvent;

/**
 *
 * @author boris.heithecker
 */
@MessageDriven(mappedName = "jms/tickets-topic", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/tickets-topic"),
//    @ActivationConfigProperty(propertyName = "clientId", propertyValue = "jms/documents-topic"),
    @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "jms/tickets-topic"),
    @ActivationConfigProperty(propertyName = "connectionFactoryJndiName", propertyValue = "jms/ticket-topic-factory")
})
public class TicketRemovalListener implements MessageListener {

    @EJB
    private ConcurrentTicketsBean concurrentTicketsBean;

    public TicketRemovalListener() {
    }

    @Override
    public void onMessage(Message message) {
        TicketEvent event;
        try {
            event = message.getBody(TicketEvent.class);
        } catch (JMSException | ClassCastException ex) {
            return;
        }
        if (event.getSource() != null && event.getType().equals(TicketEvent.TicketEventType.REMOVE)) {
            Ticket t = event.getSource();
            concurrentTicketsBean.removeTicket(t);
        }
    }

}
