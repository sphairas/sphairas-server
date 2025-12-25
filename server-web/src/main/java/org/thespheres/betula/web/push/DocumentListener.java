/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.push;

import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import org.thespheres.betula.services.jms.AbstractDocumentEvent;

/**
 *
 * @author boris.heithecker
 */
@MessageDriven(mappedName = "jms/documentsMessages", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic"),
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
        final boolean push = Boolean.getBoolean("push.document.events");
        if (push) {
            try {
                if (msg.isBodyAssignableTo(AbstractDocumentEvent.class)) {
                    final AbstractDocumentEvent event = msg.getBody(AbstractDocumentEvent.class);
//                    final EventBus eventBus = EventBusFactory.getDefault().eventBus();
//                    eventBus.publish(DocumentMessagesResource.CHANNEL_BASE, event);
                }
            } catch (JMSException | ClassCastException ex) {
                Logger.getLogger(DocumentListener.class.getName()).log(Level.WARNING, ex.getLocalizedMessage(), ex);
            }
        }
    }
}
