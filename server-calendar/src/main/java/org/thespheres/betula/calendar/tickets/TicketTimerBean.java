/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.tickets;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.calendar.facade.TicketsFacade;
import org.thespheres.betula.server.beans.TicketsLocal;

/**
 *
 * @author boris.heithecker
 */
@Startup
@Singleton
public class TicketTimerBean {

    @Resource
    private TimerService service;
    @EJB(beanName = "TicketsLocalImpl")
    private TicketsLocal ticketsLocal;
    @EJB
    private TicketsFacade facade;

    public void addEvent(final Date d, final Ticket t) {
        service.createTimer(d, t);
    }

    @PostConstruct
    public void onStartup() {
        if (Boolean.getBoolean("TicketTimerBean.no.restore.timers")) {//Option for failfast start
            return;
        }
        final Collection<Timer> timers = service.getTimers();
        final Ticket[] at = ticketsLocal.activeTickets();
        Logger.getLogger(TicketTimerBean.class.getName()).log(Level.INFO, "Found {0} active Ticket(s) after startup, and {1} active ticket timeout service(s).", new Object[]{at.length, timers.size()});
        final Date now = new Date();
        for (final Ticket ticket : at) {
            boolean found = timers.stream().anyMatch(t -> (t.getInfo() instanceof Ticket && ((Ticket) t.getInfo()).equals(ticket)));
            if (!found) {
                final List<TicketEntity> l = facade.findTicketEntitiesForTicket(ticket);
                if (!l.isEmpty()) {
                    final TicketEntity te = l.get(0);
                    final Date start = te.getDtstart();
                    if (start.after(now)) {
                        addEvent(start, ticket);
                        Logger.getLogger(TicketTimerBean.class.getName()).log(Level.INFO, "Restored ticket timeout service for Ticket {0} at {1}.", new Object[]{Long.toString(ticket.getId()), start.toString()});
                    }
                }
            }
        }
    }

    @Timeout
    public void timeout(final Timer timer) {
        if (timer.getInfo() instanceof Ticket) {
            final Ticket ticket = (Ticket) timer.getInfo();
            boolean success = ticketsLocal.deleteTicket(ticket);
            Logger.getLogger(TicketTimerBean.class.getName()).log(Level.INFO, "A ticket timeout timer event has happened. Ticket id is {0}, success is {1}.", new Object[]{Long.toString(ticket.getId()), success});
        }
    }
}
