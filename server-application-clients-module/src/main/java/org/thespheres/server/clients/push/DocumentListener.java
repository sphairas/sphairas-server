/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.push;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.thespheres.betula.services.jms.AbstractDocumentEvent;

/**
 *
 * @author boris.heithecker
 */
@MessageDriven(mappedName = "jms/documentsMessages", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/documents-topic"),
//    @ActivationConfigProperty(propertyName = "clientId", propertyValue = "jms/documents-topic"),
    @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "jms/documents-topic"),
    @ActivationConfigProperty(propertyName = "connectionFactoryJndiName", propertyValue = "jms/document-topic-factory")
})
public class DocumentListener implements MessageListener {

    public DocumentListener() {
    }

    @Override
    public void onMessage(Message msg) {
        try {
            if (msg.isBodyAssignableTo(AbstractDocumentEvent.class)) {
                final AbstractDocumentEvent event = msg.getBody(AbstractDocumentEvent.class);
                MessagesEndpoint.getEventBus().post(event);
            }
        } catch (JMSException | ClassCastException ex) {
            Logger.getLogger(DocumentListener.class.getName()).log(Level.WARNING, ex.getLocalizedMessage(), ex);
        }
    }
}
