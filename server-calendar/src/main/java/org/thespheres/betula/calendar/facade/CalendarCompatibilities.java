/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.facade;

import java.util.Arrays;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author boris.heithecker@gmx.net
 */
public class CalendarCompatibilities {
    
    private final String[] compat;
    
    CalendarCompatibilities(final String[] compatibilities) {
        this.compat = compatibilities;
    }
    
    public static CalendarCompatibilities extractCompatibilities(final HttpServletRequest request) {
        final String unitAuth = request.getParameter("compat");
        return Optional.ofNullable(unitAuth)
                .filter(v -> !v.isEmpty())
                .map(v -> v.split(","))
                .map(CalendarCompatibilities::new)
                .orElse(new CalendarCompatibilities(new String[0]));
    }
    
    public boolean hasCompatibility(final String c) {
        return Arrays.stream(compat).anyMatch(c::equals);
    }
    
}
