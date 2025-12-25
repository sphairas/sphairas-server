/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.tickets;

import jakarta.ejb.Asynchronous;
import jakarta.ejb.Singleton;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.thespheres.betula.Ticket;

/**
 *
 * @author boris.heithecker
 */
@Singleton
@LocalBean
public class ConcurrentTicketsBean {

    @PersistenceContext(unitName = "calendarsPU")
    private EntityManager em;

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Asynchronous
    public void removeTicket(Ticket t) {
        TypedQuery<TicketEntity> q = em.createNamedQuery("findTicketEntitiesForTicket", TicketEntity.class);
        q.setParameter("ticketId", t.getId())
                .setParameter("ticketAuthority", t.getAuthority())
                //                        .setLockMode(LockModeType.PESSIMISTIC_FORCE_INCREMENT) //Funktioniert nicht  //ZGN
                .getResultList().stream().forEach((te) -> {
                    EmbeddedableTicketEntry ete = new EmbeddedableTicketEntry(t);
                    if (te.getTicketEntries().remove(ete)) {
                        em.merge(te);
                    }
                });
    }
}
