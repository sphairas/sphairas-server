package org.thespheres.betula.calendar.rest;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.calendar.facade.CalendarCompatibilities;
import org.thespheres.betula.calendar.facade.TicketsFacade;
import org.thespheres.betula.calendar.tickets.TicketEntity;
import org.thespheres.betula.server.beans.Utilities;
import org.thespheres.ical.CalendarComponent;
import org.thespheres.ical.ICalendar;
import org.thespheres.ical.InvalidComponentException;
import org.thespheres.ical.UID;
import org.thespheres.ical.builder.ICalendarBuilder;

@Stateless
@Path("tickets")
public class TicketsResource {

    @EJB
    private TicketsFacade facade;
    @Context
    private HttpServletRequest request;

    @GET
    @Produces("text/calendar; charset=UTF-8")
    public Response get() {
        final Ticket ticket = Utilities.extractTicket(request);
        final CalendarCompatibilities compat = CalendarCompatibilities.extractCompatibilities(request);
        final ICalendar ical;
        if (ticket != null) {
            final List<TicketEntity> entries = facade.findTicketEntitiesForTicket(ticket);
            final UID[] uid = entries.stream().map(TicketEntity::getUID).toArray(UID[]::new);
            ical = facade.getICalendar(uid, compat);
        } else {
            ical = facade.getICalendar((UID[]) null, compat);
        }
        return Response
                .ok(ical.toString())
                .build();
    }

    @POST
    @RolesAllowed({"unitadmin"})
    public Response post() throws IOException {
        final List<ICalendar> calendars;
        try (InputStream is = request.getInputStream()) {
            calendars = ICalendarBuilder.parseCalendars(is);
        } catch (ParseException | InvalidComponentException ex) {
            throw new IOException(ex);
        }
        for (ICalendar cal : calendars) {
            if (cal.getAnyPropertyValue("METHOD").map("UPDATE"::equals).orElse(false)) {
                for (CalendarComponent cc : cal.getComponents()) {
                    UID uid = cc.getUID();
                    try {
                        if (uid == null) {
                            uid = facade.create(cc);
                        } else {
                            facade.update(cc);
                        }
                    } catch (Exception ex) {
                        return Response.serverError().build();
                    }
                    final UID componentUid = uid;
                    assert componentUid != null;
                    cc.getProperties("X-TICKET").stream()
                            .filter(ccp -> ccp.getAnyParameter("x-ticket-authority").isPresent())
                            .map(ccp -> new Ticket(ccp.getAnyParameter("x-ticket-authority").get(), Long.parseLong(ccp.getValue())))
                            .forEach(t -> facade.updateTicket(componentUid, t, false));
                }
            }
        }
        return Response
                .noContent()
                .build();
    }

    @DELETE
    public Response delete() {
        final Ticket ticket = Utilities.extractTicket(request);
        if (ticket != null) {
            final int removed = facade.removeTicketEntries(ticket);
            if (removed == 0) {
                return Response
                        .status(Response.Status.NOT_FOUND)
                        .build();
            }
        }
        return Response
                .noContent()
                .build();
    }
}
