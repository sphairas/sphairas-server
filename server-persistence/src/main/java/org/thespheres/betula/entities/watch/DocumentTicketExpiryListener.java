/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.watch;

import java.util.Date;
import java.util.List;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.services.jms.MultiTargetAssessmentEvent;
import org.thespheres.betula.services.jms.MultiTargetAssessmentEvent.Update;
import org.thespheres.betula.entities.TermGradeTargAssessTicketEnt;

/**
 *
 * @author boris.heithecker
 */
@MessageDriven(mappedName = "jms/documents-topic", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/documents-topic"),
//    @ActivationConfigProperty(propertyName = "clientId", propertyValue = "jms/documents-topic"),
    @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "jms/documents-topic"),
    @ActivationConfigProperty(propertyName = "connectionFactoryJndiName", propertyValue = "jms/document-topic-factory")
})
//@MessageDriven(activationConfig = {
//    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/tickets-topic"),
//    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic")
//})
public class DocumentTicketExpiryListener implements MessageListener {

    @PersistenceContext(unitName = "betula0")
    protected EntityManager em;
    @EJB
    private DocumentTicketExpiryTimer documentTicketExpiryTimer;

    public DocumentTicketExpiryListener() {
    }

    @Override
    public void onMessage(final Message message) {
        final MultiTargetAssessmentEvent<TermId> event;
        try {
            event = message.getBody(MultiTargetAssessmentEvent.class);
        } catch (JMSException | ClassCastException ex) {
            return;
        }
        final Update<TermId>[] updates = event.getUpdates();
        if (updates != null) {
            for (final Update<TermId> u : updates) {
                StudentId student = u.getStudent();
                TermId term = u.getGradeId();
                if (event.getSource() != null && term != null && student != null && u.getValue() != null) {
                    //Cached ResultList
                    List<TermGradeTargAssessTicketEnt> l = em.createNamedQuery("findNonNullDeleteIntervalTermGradeTargAssessTickets", TermGradeTargAssessTicketEnt.class).getResultList();
                    l.stream().filter((t) -> (t.getStudent().equals(student) && t.getTerm().equals(term))).forEach((t) -> {
//                deleteDocumentTickets.addDelete(t);
                        //TODO: lösche reguläres event!
//                service.getTimers().iterator().next().cancel();
                        Date timeout = new Date(System.currentTimeMillis() + (t.getDeleteAfterEditInterval() * 1000));
                        documentTicketExpiryTimer.addEvent(timeout, t.getTicket());
                    });
                }
            }
        }
    }

}
