/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.watch;

import java.util.Date;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.server.beans.TicketsLocal;

/**
 *
 * @author boris.heithecker
 */
@Stateless
public class DocumentTicketExpiryTimer {

    @Resource
    private TimerService service;
    @EJB(beanName = "TicketsLocalImpl")
    private TicketsLocal ticketsLocal;

    public void addEvent(Date d, Ticket t) {
        service.createTimer(d, t);
    }

    @Timeout
    public void timeout(Timer timer) {
        if (timer.getInfo() instanceof Ticket) {
            Ticket ticket = (Ticket) timer.getInfo();
            if (ticket != null) {
                ticketsLocal.deleteTicket(ticket);
            }
        }
    }
}
