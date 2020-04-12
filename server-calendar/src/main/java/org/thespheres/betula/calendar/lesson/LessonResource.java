/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.lesson;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Size;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.server.beans.annot.Authority;
import org.thespheres.betula.server.beans.annot.LessonsCalendar;
import org.thespheres.betula.server.beans.config.CommonAppProperties;
import org.thespheres.betula.services.calendar.LessonData;
import org.thespheres.betula.services.calendar.LessonTimeData;
import org.thespheres.betula.services.calendar.VendorData;
import org.thespheres.ical.UID;
import org.thespheres.ical.util.IComponentUtilities;

/**
 * REST Web Service
 *
 * @author boris.heithecker
 */
//https://github.com/payara/Payara/issues/2841
//https://github.com/payara/Payara/issues/2490
//@RolesAllowed("unitadmin")
@Path("/lessons")
@Stateless
public class LessonResource {

    @PersistenceContext(unitName = "calendarsPU")
    private EntityManager em;
    @Resource
    protected SessionContext context;
    @Authority
    @Inject
    private String authority;
    @LessonsCalendar
    @Inject
    private DocumentId calendar;

    public LessonResource() {
    }

    @GET
    @Path("subscription-types/{signee}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getUpdateTypes(@PathParam("signee") String sig, @Context final HttpServletRequest request) {
        final Signee signee = Signee.parse(sig);
//        if (!Signee.isNull(signee)) {
//return Response.ok(ret, MediaType.TEXT_PLAIN).build();
//        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @PUT
    @Path("subscription-types/{signee}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response setUpdateTypes(@PathParam("signee") String sig, @Context final HttpServletRequest request, @Size(max = 255) final String raw) {
        final Signee signee = Signee.parse(sig);
        if (!Signee.isNull(signee)) {
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @PUT
    @Path("/lesson")
    @Consumes(MediaType.APPLICATION_XML)
    public Response putLessonData(final LessonData l, @QueryParam("calendar-authority") String authority, @QueryParam("calendar-id") String id, @QueryParam("calendar-version") String version, @Context final HttpServletRequest request) { //@QueryParam("message.authority") String authority, @QueryParam("message.id") Long id) {
        final DocumentId lc;
        if (authority != null && id != null && version != null) {
            lc = new DocumentId(authority, id, DocumentId.Version.parse(version));
        } else {
            lc = calendar;
        }
        LessonCalendar lessonCalendar = em.find(LessonCalendar.class, lc);
        if (lessonCalendar == null) {
            lessonCalendar = new LessonCalendar(lc, CommonAppProperties.provider(), "Stundenplan");
            em.persist(lessonCalendar);
        }
        Lesson lesson;
        try {
            lesson = em.createNamedQuery("findLessonForLessonId", Lesson.class)
                    .setParameter("lesson", new EmbeddableLessonId(l.getLesson(), 0))
                    .getSingleResult();
        } catch (final NoResultException ignored) {
            lesson = new Lesson(lessonCalendar, l.getLesson(), l.getUnit());
            em.persist(lesson);
        }
        final VendorData vData = l.getVendorData();
        VendorLessonMapping mapping = null;
        if (vData != null) {
            mapping = em.find(VendorLessonMapping.class, new EmbeddableLessonId(vData.getVendorLesson(), vData.getVendorLink()));
            if (mapping == null) {
                mapping = new VendorLessonMapping(vData.getVendorLesson(), vData.getVendorLink(), lesson);
                em.persist(lesson);
            } else {
                mapping.getComponents().clear();
            }
            lesson.getVendorLessonMappings().add(mapping);
        }
        final Date b = Date.from(l.getCourseBegin().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        lesson.setBeginDate(b);
        final Date e = Date.from(l.getCourseEnd().atStartOfDay().plusDays(1l).atZone(ZoneId.systemDefault()).toInstant());
        lesson.setEndDate(e);

        final List<WeeklyLessonComponent> toRemove = new ArrayList<>(lesson.getTimes());
        param:
        for (final LessonTimeData t : l.getTimes()) {
            for (final WeeklyLessonComponent cmp : lesson.getTimes()) {
                if (equalDt(cmp, t, l)) {
                    updateComponent(cmp, t, l);
                    toRemove.remove(cmp);
                    cmp.getVendorLessonMappings().clear();
                    if (mapping != null) {
                        cmp.getVendorLessonMappings().add(mapping);
                        mapping.getComponents().add(cmp);
                    }
                    continue param;
                } else if (vData != null && isLinked(cmp, vData, lesson)) {
                    //Don't remove the component if it stems from the same updater session
                    toRemove.remove(cmp);
                }
            }
            final WeeklyLessonComponent cmp;
            try {
                cmp = new WeeklyLessonComponent(UID.create(), lesson);
            } catch (IOException ex) {
                throw new IllegalStateException("Could not automatically create new UID for component.", ex);
            }
            updateComponent(cmp, t, l);
            lesson.getTimes().add(cmp);
            if (mapping != null) {
                cmp.getVendorLessonMappings().add(mapping);
                mapping.getComponents().add(cmp);
            }
        }
        lesson.getTimes().removeAll(toRemove);
        em.merge(lessonCalendar);
        em.merge(lesson);
        if (mapping != null) {
            em.merge(mapping);
        }
        return Response.ok().build();
    }

    private void updateComponent(final WeeklyLessonComponent ut, final LessonTimeData t, final LessonData d) {
        final Date s = toDate(d, t, false);
        final Date e = toDate(d, t, true);
        ut.setDtstart(s);
        ut.setDtend(e);
        ut.setLocation(t.getRoom());
        ut.setDayOfWeek(t.getDay());
        ut.setPeriod(t.getPeriod());

        final LessonTimeData.ExWeeks[] exWeeks = t.getExWeeks();
        if (exWeeks != null && exWeeks.length != 0) {
            Calendar c = Calendar.getInstance(Locale.GERMANY);
//            c.setFirstDayOfWeek(Calendar.MONDAY);
//            c.setMinimalDaysInFirstWeek(1);
            c.setTime(ut.getDtstart());
            final StringJoiner sj = new StringJoiner(",");
            for (final LessonTimeData.ExWeeks me : exWeeks) {
                //Set it every time, problem is week 53 > week of year 53 may add +1 one to year!
//                c.set(Calendar.YEAR, me.getKey());
                for (int i = 0; i < me.getExWeeks().length; i++) {
                    int w = me.getExWeeks()[i];
                    c.set(Calendar.YEAR, me.getYear());
                    c.set(Calendar.WEEK_OF_YEAR, w);
                    String f = IComponentUtilities.DATE.format(c.getTime());
                    sj.add(f);
                }
            }
            ut.setExDates(sj.toString());
        }
    }

    private static boolean equalDt(final WeeklyLessonComponent cmp, final LessonTimeData t, final LessonData d) {
        return cmp.getDtstart().equals(toDate(d, t, false))
                && cmp.getDtend().equals(toDate(d, t, true));
    }

    private boolean isLinked(final WeeklyLessonComponent cmp, final VendorData vData, final Lesson lesson) {
        return cmp.getVendorLessonMappings().stream()
                .filter(vlm -> vlm.getLessonUnit().equals(lesson)) //just in case
                .map(vlm -> vlm.getVendorLessonId())
                .anyMatch(vData.getJoinedVendorLessons()::contains);
    }

    static Date toDate(final LessonData d, final LessonTimeData t, final boolean end) {
        final LocalDate cb = d.getCourseBegin()
                .with(TemporalAdjusters.nextOrSame(t.getDay()));
        final LocalDateTime sdt = LocalDateTime.of(cb, !end ? t.getStart() : t.getEnd());
        return Date.from(sdt.atZone(ZoneId.systemDefault()).toInstant());
    }

}
