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
import org.thespheres.betula.TermId;
import org.thespheres.betula.services.jms.MultiTargetAssessmentEvent;

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
public class DocumentsMessagesListener implements MessageListener {

    @EJB
    private EventDispatch multiTargetAssessmentEventService;
//    @Resource
//    private MessageDrivenContext mdc;

    public DocumentsMessagesListener() {
    }

    @Override
    public void onMessage(Message msg) {
        try {
            if (msg.isBodyAssignableTo(MultiTargetAssessmentEvent.class)) {
                MultiTargetAssessmentEvent<TermId> event = msg.getBody(MultiTargetAssessmentEvent.class);
                if (event.getSource() != null && event.getUpdates() != null) {
                    multiTargetAssessmentEventService.onDocumentEvent(event);
                }
            }
        } catch (JMSException | ClassCastException ex) {
        }
    }
}
