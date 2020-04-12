/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.reports;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.Set;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

/**
 *
 * @author boris.heithecker
 */
public class ReportDatesServlet extends HttpServlet {

    @EJB
    private ZeugniskonferenzFacade zkFacade;
//    @Default
//    @Inject
//    private CalendarResourceProvider calResource;   not goot use inject this in servlet

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final UnitId uid = Utilities.extractUnitId(request);
        final TermId term = Utilities.extractTermId(request);
        final ICalendar ical = zkFacade.getZeugnisCalendar(uid, term);
        response.getWriter().print(ical.toString());
    }

    @RolesAllowed({"unitadmin"})
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final List<ICalendar> ical;
        try (InputStream is = request.getInputStream()) {
            ical = ICalendarBuilder.parseCalendars(is, request.getCharacterEncoding());
        } catch (ParseException | InvalidComponentException ex) {
            throw new IOException(ex);
        }
        for (final ICalendar cal : ical) {
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
                        } catch (Exception e) {
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
                        }
                    }
                } else if (isUpdate) {
                    final boolean success = zkFacade.update(uid, cc, cat);
                    if (!success) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                }
            }
        }
        response.setContentType(ICalendar.MIME);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final UID uid = Utilities.extractUID(req);
        if (uid != null) {
            final boolean success = zkFacade.remove(uid);
            if (!success) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }

}
