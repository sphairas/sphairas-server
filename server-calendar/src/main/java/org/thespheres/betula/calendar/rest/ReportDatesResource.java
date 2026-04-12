package org.thespheres.betula.calendar.rest;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.Set;
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
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.calendar.facade.ZeugniskonferenzFacade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.server.beans.Utilities;
import org.thespheres.ical.CalendarComponent;
import org.thespheres.ical.ICalendar;
import org.thespheres.ical.InvalidComponentException;
import org.thespheres.ical.UID;
import org.thespheres.ical.builder.ICalendarBuilder;
import org.thespheres.ical.util.IComponentUtilities;

@Stateless
@Path("reports")
public class ReportDatesResource {

    @EJB
    private ZeugniskonferenzFacade zkFacade;
    @Context
    private HttpServletRequest request;

    @GET
    @Produces("text/calendar; charset=UTF-8")
    public Response get() {
        final UnitId unit = Utilities.extractUnitId(request);
        final TermId term = Utilities.extractTermId(request);
        final ICalendar ical = zkFacade.getZeugnisCalendar(unit, term);
        return Response
                .ok(ical.toString())
//                .type("text/calendar; charset=UTF-8")
                .build();
    }

    @POST
    @RolesAllowed({"unitadmin"})
    @Produces("text/calendar; charset=UTF-8")
    public Response post() throws IOException {
        final List<ICalendar> calendars;
        try (InputStream is = request.getInputStream()) {
            calendars = ICalendarBuilder.parseCalendars(is, request.getCharacterEncoding());
        } catch (ParseException | InvalidComponentException ex) {
            throw new IOException(ex);
        }
        for (final ICalendar cal : calendars) {
            final boolean isUpdate = cal.getAnyPropertyValue("METHOD").map("UPDATE"::equals).orElse(false);
            for (CalendarComponent cc : cal.getComponents()) {
                final UID uid = cc.getUID();
                final Set<String> cat = IComponentUtilities.parseCategories(cc);
                if (uid == null) {
                    final List<DocumentId> reportId = IComponentUtilities.parseDocumentIds(cc);
                    final UnitId uprop = IComponentUtilities.parseUnitId(cc);
                    final TermId tprop = IComponentUtilities.parseTermId(cc);
                    if (uprop != null && tprop != null) {
                        try {
                            zkFacade.create(uprop, tprop, reportId, cc, cat);
                        } catch (Exception ex) {
                            return Response
                                    .serverError()
                                    .entity(ex.getLocalizedMessage())
                                    .build();
                        }
                    }
                } else if (isUpdate) {
                    final boolean success = zkFacade.update(uid, cc, cat);
                    if (!success) {
                        return Response
                                .status(Response.Status.NOT_FOUND)
                                .build();
                    }
                }
            }
        }
        return Response
                .ok()
//                .type("text/calendar; charset=UTF-8")
                .build();
    }

    @DELETE
    public Response delete() {
        final UID uid = Utilities.extractUID(request);
        if (uid != null) {
            final boolean success = zkFacade.remove(uid);
            if (!success) {
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
