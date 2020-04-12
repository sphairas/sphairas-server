/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.config;

import java.time.Month;
import java.util.StringJoiner;
import org.thespheres.betula.server.beans.MissingConfigurationResourceException;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.server.beans.annot.Authority;
import org.thespheres.betula.server.beans.annot.Current;
import org.thespheres.betula.services.NoProviderException;
import org.thespheres.betula.services.ical.CalendarResourceProvider;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.server.beans.annot.LessonsCalendar;

/**
 *
 * @author boris.heithecker
 */
@ApplicationScoped
public class CalendarConfig {

    public static final String CALENDAR_ID = "lessons";
    @Authority
    @Inject
    private String authority;

    @Current
    @Inject
    private Term currentTerm;

    @Default
    @Produces
    public CalendarResourceProvider calResource() {
        try {
            //TODO: check type && System.property providerUrl
            return CalendarResourceProvider.find("mk.niedersachsen.de");
        } catch (NoProviderException npex) {
            throw new MissingConfigurationResourceException("TermSchedule");
        }
    }

    @LessonsCalendar
    @Produces
    public DocumentId currentLessonCalendar() {
        final int beginYear = currentTerm.getBeginDate().getYear();
        final boolean hj2 = currentTerm.getBeginDate().getMonthValue() < Month.AUGUST.getValue();
        final int year = hj2 ? beginYear - 1 : beginYear;
        final DocumentId.Version version = docVersionFromYearAndCalendarVersion(year, null);
        return new DocumentId(authority, CALENDAR_ID, version);
    }

    private DocumentId.Version docVersionFromYearAndCalendarVersion(final int year, final Integer version) {
        final StringJoiner sj = new StringJoiner(".");
        final String yearValue = Integer.toString(year);
        sj.add(yearValue);
        if (version != null) {
            sj.add(version.toString());
        }
        return DocumentId.Version.parse(sj.toString());
    }

}
