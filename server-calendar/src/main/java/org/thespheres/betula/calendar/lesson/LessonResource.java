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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
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
import org.jboss.logging.Logger;
import org.thespheres.betula.UnitId;
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
                    .setParameter("calendar", lessonCalendar)
                    .getSingleResult();
        } catch (final NoResultException ignored) {
            if (l.getUnits() != null && l.getUnits().length != 1) {
                Logger.getLogger(LessonResource.class.getName()).log(Logger.Level.WARN, "Multiple units not supported.");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            final UnitId unit = l.getUnits() == null ? null : l.getUnits()[0];
            lesson = new Lesson(lessonCalendar, l.getLesson(), unit);
            em.persist(lesson);
        }
        final VendorData vData = l.getVendorData();
        VendorLessonMapping lessonMapping = null;
        if (vData != null) {
            lessonMapping = em.find(VendorLessonMapping.class, new EmbeddableLessonId(vData.getVendorLesson(), vData.getVendorLink()));
            if (lessonMapping == null) {
                lessonMapping = new VendorLessonMapping(vData.getVendorLesson(), vData.getVendorLink(), lesson);
                em.persist(lessonMapping);
            } else {
                lessonMapping.getComponents().clear();
            }
            lesson.getVendorLessonMappings().add(lessonMapping);
        }

        final List<WeeklyLessonComponent> toRemove = new ArrayList<>(lesson.getTimes());
        param:
        for (final LessonTimeData t : l.getTimes()) {
            for (final WeeklyLessonComponent cmp : lesson.getTimes()) {
                if (equalDt(cmp, t, l)) {
                    updateComponent(cmp, t, l);
                    toRemove.remove(cmp);
                    cmp.getVendorLessonMappings().clear();
                    updateVendorMappings(cmp, lessonMapping, t.getVendorData());
                    continue param;
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
            updateVendorMappings(cmp, lessonMapping, t.getVendorData());
        }
        lesson.getTimes().removeAll(toRemove);
        em.merge(lessonCalendar);
        em.merge(lesson);
        if (lessonMapping != null) {
            em.merge(lessonMapping);
        }
        return Response.ok().build();
    }

    private void updateVendorMappings(final WeeklyLessonComponent cmp, final VendorLessonMapping lessonMapping, final VendorData timeVendorData) {
        if (lessonMapping != null) {
            cmp.getVendorLessonMappings().add(lessonMapping);
            lessonMapping.getComponents().add(cmp);
        }
        if (timeVendorData != null) {
            VendorLessonMapping timeMapping = em.find(VendorLessonMapping.class, new EmbeddableLessonId(timeVendorData.getVendorLesson(), timeVendorData.getVendorLink()));
            if (timeMapping == null) {
                timeMapping = new VendorLessonMapping(timeVendorData.getVendorLesson(), timeVendorData.getVendorLink(), cmp.getLesson());
                em.persist(timeMapping);
            } else {
                timeMapping.getComponents().clear();
            }
            cmp.getVendorLessonMappings().add(timeMapping);
            timeMapping.getComponents().add(cmp);
        }
    }

    private void updateComponent(final WeeklyLessonComponent ut, final LessonTimeData t, final LessonData d) {
        final Date s = toDate(t, false);
        final Date e = toDate(t, true);
        ut.setDtstart(s);
        ut.setDtend(e);
        ut.setLocation(t.getLocation());
        ut.setDayOfWeek(t.getDay());
        ut.setPeriod(t.getPeriod());

        final Date since = Date.from(t.getSince().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        ut.setSince(since);
        final Date until = Date.from(t.getUntil().atStartOfDay().plusDays(1l).atZone(ZoneId.systemDefault()).toInstant());
        ut.setUntil(until);

        final LocalDate[] exDates = t.getExdates();
        if (exDates != null) {
            final String exDatesValue = Arrays.stream(exDates)
                    .map(ld -> IComponentUtilities.DATE_FORMATTER.format(ld))
                    .collect(Collectors.joining(","));
            ut.setExDates(exDatesValue);
        }

    }

    private static boolean equalDt(final WeeklyLessonComponent cmp, final LessonTimeData t, final LessonData d) {
        return cmp.getDtstart().equals(toDate(t, false))
                && cmp.getDtend().equals(toDate(t, true));
    }

    static Date toDate(final LessonTimeData t, final boolean end) {
        final LocalDate cb = t.getSince()
                .with(TemporalAdjusters.nextOrSame(t.getDay()));
        final LocalDateTime sdt = LocalDateTime.of(cb, !end ? t.getStart() : t.getEnd());
        return Date.from(sdt.atZone(ZoneId.systemDefault()).toInstant());
    }

}
