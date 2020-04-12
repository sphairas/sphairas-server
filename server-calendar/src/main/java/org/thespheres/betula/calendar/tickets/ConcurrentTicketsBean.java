/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.tickets;

import javax.ejb.Asynchronous;
import javax.ejb.Singleton;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
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
