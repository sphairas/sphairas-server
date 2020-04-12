/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.tickets;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.calendar.facade.TicketsFacade;
import org.thespheres.betula.server.beans.Utilities;
import org.thespheres.ical.CalendarComponent;
import org.thespheres.ical.ICalendar;
import org.thespheres.ical.InvalidComponentException;
import org.thespheres.ical.UID;
import org.thespheres.ical.builder.ICalendarBuilder;

/**
 *
 * @author boris.heithecker
 */
public class TicketsServlet extends HttpServlet {

    @EJB
    private TicketsFacade facade;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Ticket ticket = Utilities.extractTicket(request);
        final ICalendar ical;
        if (ticket != null) {
            final List<TicketEntity> l = facade.findTicketEntitiesForTicket(ticket);
            UID[] uid = l.stream()
                    .map(TicketEntity::getUID)
                    .toArray(UID[]::new);
            ical = facade.getICalendar(uid);
        } else {
            ical = facade.getICalendar((UID[]) null);
        }
        response.getWriter().print(ical.toString());
    }

    @RolesAllowed({"unitadmin"})
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final List<ICalendar> ical;
        try (InputStream is = request.getInputStream()) {
            ical = ICalendarBuilder.parseCalendars(is);
        } catch (ParseException | InvalidComponentException ex) {
            throw new IOException(ex);
        }
        for (ICalendar cal : ical) {
            if (cal.getAnyPropertyValue("METHOD").map("UPDATE"::equals).orElse(false)) {
                for (CalendarComponent cc : cal.getComponents()) {
                    UID uid = cc.getUID();
                    try {
                        if (uid == null) {
                            uid = facade.create(cc);
                        } else {
                            facade.update(cc);
                        }
                    } catch (Exception e) {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                    final UID ccUID = uid;
                    assert ccUID != null;
                    cc.getProperties("X-TICKET").stream()
                            .filter(ccp -> ccp.getAnyParameter("x-ticket-authority").isPresent())
                            .map(ccp -> new Ticket(ccp.getAnyParameter("x-ticket-authority").get(), Long.parseLong(ccp.getValue())))
                            .forEach(t -> facade.updateTicket(ccUID, t, false));
                }
            }
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final Ticket ticket = Utilities.extractTicket(req);
        if (ticket != null) {
            final int removed = facade.removeTicketEntries(ticket);
            if (removed == 0) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }

}
