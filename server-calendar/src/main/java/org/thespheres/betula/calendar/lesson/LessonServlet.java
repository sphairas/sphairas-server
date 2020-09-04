/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.lesson;

import java.io.IOException;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.calendar.facade.CalendarCompatibilities;
import org.thespheres.betula.server.beans.Utilities;
import org.thespheres.ical.ICalendar;
import org.thespheres.ical.UID;
import org.thespheres.betula.calendar.facade.LessonCalendarFacade;

/**
 *
 * @author boris.heithecker
 */
public class LessonServlet extends HttpServlet {

    @EJB
    private LessonCalendarFacade facade;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final UnitId unit = Utilities.extractUnitId(request);
        final CalendarCompatibilities compat = CalendarCompatibilities.extractCompatibilities(request);
        if (facade.getCalendar() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            final ICalendar ical;
            if (unit != null) {
                ical = facade.getPublished(unit, compat);
            } else {
                ical = facade.getPublished((UID[]) null, compat);
            }
            response.getWriter().print(ical.toString());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
//        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }

}
