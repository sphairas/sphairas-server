/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.facade.impl;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.calendar.UniqueCalendarComponentEntity;
import org.thespheres.betula.calendar.facade.CalendarCompatibilities;
import org.thespheres.ical.builder.ICalendarBuilder;
import org.thespheres.betula.calendar.util.EmbeddableSignee;
import org.thespheres.betula.calendar.util.EmbeddableUnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.services.scheme.spi.PeriodId;
import org.thespheres.ical.CalendarComponentProperty;
import org.thespheres.ical.ICalendar;
import org.thespheres.ical.InvalidComponentException;
import org.thespheres.ical.Parameter;
import org.thespheres.ical.UID;
import org.thespheres.ical.util.IComponentUtilities;
import org.thespheres.betula.calendar.facade.LessonCalendarFacade;
import org.thespheres.betula.calendar.lesson.Lesson;
import org.thespheres.betula.calendar.lesson.LessonCalendar;
import org.thespheres.betula.calendar.lesson.LessonSubscriber;
import org.thespheres.betula.calendar.lesson.VendorLessonMapping;
import org.thespheres.betula.calendar.lesson.WeeklyLessonComponent;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.server.beans.annot.LessonsCalendar;
import org.thespheres.betula.services.scheme.spi.LessonId;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class LessonCalendarFacadeImpl extends FixedCalendarFacade<LessonCalendar, WeeklyLessonComponent> implements LessonCalendarFacade {

    @Default
    @Inject
    private NamingResolver nr;
    @LessonsCalendar
    @Inject
    private DocumentId calendarId;
    final NumberFormat nf = NumberFormat.getIntegerInstance();

    public LessonCalendarFacadeImpl() {
        super(WeeklyLessonComponent.class);
        nf.setMinimumIntegerDigits(2);
    }

    @Override
    public LessonCalendar getCalendar() {
        return getEntityManager().find(LessonCalendar.class, calendarId);
    }

    @Override//Cannot use entitymanager in decorator
    public List<Lesson> findLessonsForSignee(final Signee signee) {
        final List<LessonSubscriber> res = getEntityManager().createNamedQuery("findLessonsForSignee", LessonSubscriber.class)
                .setParameter("signee", new EmbeddableSignee(signee))
                .setLockMode(LockModeType.OPTIMISTIC)//No lock column
                .getResultList();
        return res.stream()
                .map(s -> s.getLessonUnit())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public ICalendar getPublished(UID[] restrict, final CalendarCompatibilities compat) {
        return getICalendar(restrict, compat);
    }

    @Override
    protected void addPropertiesToICalendarBody(ICalendarBuilder cb) throws InvalidComponentException {
        final DocumentId id = getCalendar().getDocumentId();
        cb.addProperty("X-CALENDAR-ID", id.getId(), new Parameter("x-calendar-authority", id.getAuthority()), new Parameter("x-calendar-version", id.getVersion().getVersion()));
    }

    @Override   //TODO security check!!!!
    public ICalendar getPublished(final UnitId unit, final CalendarCompatibilities compat) {
        final List<Lesson> l = getEntityManager().createNamedQuery("findLessonsForUnit", Lesson.class)
                .setParameter("unit", new EmbeddableUnitId(unit))
                .setLockMode(LockModeType.OPTIMISTIC)
                .getResultList();
        final UID[] uid = l.stream()
                .flatMap(lu -> lu.getTimes().stream())
                .map(UniqueCalendarComponentEntity::getUID)
                .toArray(UID[]::new);
        return getPublished(uid, compat);
    }

    @Override
    protected void addEntityPropertiesToComponent(ICalendarBuilder.CalendarComponentBuilder ccb, WeeklyLessonComponent ut, final CalendarCompatibilities compat) throws InvalidComponentException {
        super.addEntityPropertiesToComponent(ccb, ut, compat);
        if (ut.getSummary() == null) {
            final String dn;
            try {
                dn = nr.resolveDisplayName(ut.getLesson().getUnit());
                if (dn != null) {
                    ccb.addProperty(CalendarComponentProperty.SUMMARY, dn, new Parameter("x-generated-summary", "true"));
                }
            } catch (IllegalAuthorityException ex) {
            }
        }
        if (ut.getStatus() == null) {
            ccb.addProperty(CalendarComponentProperty.STATUS, "CONFIRMED");
        }
        final Date until = ut.getUntil();
        if (until != null) {
            ccb.addProperty(CalendarComponentProperty.RRULE, "FREQ=WEEKLY;UNTIL=" + IComponentUtilities.DATE_TIME.format(until));
        }
        final String exWeeks = ut.getExDates();
        if (exWeeks != null) {
            if (compat.hasCompatibility("google-calendar")) {
                final String time = IComponentUtilities.DATE_TIME.format(ut.getDtstart()).substring(8);
                final String mapped = Arrays.stream(exWeeks.split(","))
                        .map(w -> w.concat(time))
                        .collect(Collectors.joining(","));
                        
                ccb.addProperty(CalendarComponentProperty.EXDATE, mapped);
            } else {
                ccb.addProperty(CalendarComponentProperty.EXDATE, exWeeks, Parameter.VALUE_DATE);
            }
        }
        if (ut.getLocation() != null) {
            ccb.addProperty(CalendarComponentProperty.LOCATION, ut.getLocation());
        }
        ccb.addProperty(CalendarComponentProperty.CATEGORIES, "regular");
        final PeriodId periodId = ut.getPeriod();
        ccb.addProperty("X-PERIOD", Integer.toString(periodId.getId()), new Parameter("x-period-authority", periodId.getAuthority()), new Parameter("x-period-version", periodId.getVersion().getVersion()));
        //
        final UnitId unit = ut.getLesson().getUnit();
        ccb.addProperty("X-UNIT", unit.getId(), new Parameter("x-authority", unit.getAuthority()));
        //
        final LessonId lesson = ut.getLesson().getLesson();
        ccb.addProperty("X-LESSON", lesson.getId(), new Parameter("x-authority", lesson.getAuthority()));
//        for (final VendorLessonMapping vlm : ut.getLesson().getVendorLessonMappings()) {
//            final LessonId vl = vlm.getVendorLessonId();
//            final Parameter[] params = new Parameter[]{new Parameter("x-authority", vl.getAuthority()),
//                new Parameter("x-link", nf.format(vlm.getLink()))
//            };
//            ccb.addProperty("X-VENDOR-LESSON", vl.getId(), params);
//        }
        for (final VendorLessonMapping vlm : ut.getVendorLessonMappings()) {
            final LessonId vl = vlm.getVendorLessonId();
            final Parameter[] params = new Parameter[]{new Parameter("x-authority", vl.getAuthority()),
                new Parameter("x-link", nf.format(vlm.getLink()))
            };
            ccb.addProperty("X-VENDOR-LESSON", vl.getId(), params);
        }
    }
}
