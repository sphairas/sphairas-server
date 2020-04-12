/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.localbeans;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.services.jms.TicketEvent;
import org.thespheres.betula.entities.BaseTicketEntity;
import org.thespheres.betula.entities.config.AppProperties;
import org.thespheres.betula.entities.jmsimpl.TicketsNotificator;
import org.thespheres.betula.server.beans.TicketsLocal;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class TicketsLocalImpl implements TicketsLocal {

    @PersistenceContext(unitName = "betula0")
    protected EntityManager em;
    @Inject
    protected TicketsNotificator ticketsNotificator;
//    @Provider
//    @Inject
//    private String ticketsAuthority;

    @Override
    public boolean deleteTicket(Ticket ticket) {
        if (!ticket.getAuthority().equals(AppProperties.ticketsAuthority())) {
            Logger.getLogger(TicketsLocalImpl.class.getName()).log(Level.WARNING, "Could not delete ticket {0} because its authority does not match {1}.", new Object[]{ticket, AppProperties.ticketsAuthority()});
            return false;
        }
        final BaseTicketEntity te = em.find(BaseTicketEntity.class, ticket.getId(), LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (te == null) {
            Logger.getLogger(TicketsLocalImpl.class.getName()).log(Level.WARNING, "Could not delete ticket {0} because no corresponding BaseTicketEntity has been found.", ticket);
            return false;
        }
        final TicketEvent evt = new TicketEvent(ticket, TicketEvent.TicketEventType.REMOVE);
        em.remove(te);
        ticketsNotificator.notityConsumers(evt);
        return true;
    }

    @Override
    public Ticket[] activeTickets() {
        final javax.persistence.criteria.CriteriaQuery<BaseTicketEntity> cq = em.getCriteriaBuilder().createQuery(BaseTicketEntity.class);
        cq.select(cq.from(BaseTicketEntity.class)).distinct(true);
        return em.createQuery(cq).setLockMode(LockModeType.OPTIMISTIC).getResultList().stream().map(BaseTicketEntity::getTicket).toArray(Ticket[]::new);
    }

}
