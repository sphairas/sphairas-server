/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.tickets;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.thespheres.betula.Ticket;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddedableTicketEntry implements Serializable {

    @Column(name = "TICKET_ID")
    private Long ticketId;
    @Column(name = "TICKET_AUTHORITY")
    private String ticketAuthority;

    public EmbeddedableTicketEntry() {
    }

    public EmbeddedableTicketEntry(Ticket ticket) {
        this.ticketId = ticket.getId();
        this.ticketAuthority = ticket.getAuthority();
    }

    public Ticket getTicket() {
        return new Ticket(ticketAuthority, ticketId);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.ticketId);
        hash = 79 * hash + Objects.hashCode(this.ticketAuthority);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EmbeddedableTicketEntry other = (EmbeddedableTicketEntry) obj;
        if (!Objects.equals(this.ticketId, other.ticketId)) {
            return false;
        }
        return Objects.equals(this.ticketAuthority, other.ticketAuthority);
    }

}
