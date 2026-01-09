/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import org.thespheres.betula.TermId;
import org.thespheres.betula.services.jms.MultiTargetAssessmentEvent;

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
public class DocumentsMessagesListener implements MessageListener {

    @Inject
    private EventDispatch multiTargetAssessmentEventService;
//    @Resource
//    private MessageDrivenContext mdc;

    public DocumentsMessagesListener() {
    }

    @Override
    public void onMessage(Message msg) {
        try {
            if (msg.isBodyAssignableTo(MultiTargetAssessmentEvent.class)) {
                final MultiTargetAssessmentEvent<TermId> event = msg.getBody(MultiTargetAssessmentEvent.class);
                if (event != null && event.getSource() != null && event.getUpdates() != null) {
                    multiTargetAssessmentEventService.onDocumentEvent(event);
                }
            }
        } catch (JMSException | ClassCastException ex) {
        }
    }
}
