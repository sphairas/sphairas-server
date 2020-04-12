/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import javax.ejb.Local;
import org.thespheres.betula.Ticket;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface TicketsLocal {

    public boolean deleteTicket(Ticket ticket);
    
    public Ticket[] activeTickets();
}
