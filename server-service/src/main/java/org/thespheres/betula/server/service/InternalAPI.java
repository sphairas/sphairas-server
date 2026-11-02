package org.thespheres.betula.server.service;

import jakarta.ejb.EJB;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Path;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.thespheres.betula.Identity;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.entities.BaseTicketEntity;
import org.thespheres.betula.entities.GradeTargetAssessmentEntity;
import org.thespheres.betula.entities.facade.GradeTargetDocumentFacade;
import org.thespheres.betula.entities.facade.TextTargetDocumentFacade;
import org.thespheres.betula.entities.facade.TicketFacade;
import org.thespheres.betula.entities.facade.UnitDocumentFacade;
import org.thespheres.betula.server.beans.AmbiguousDateException;
import org.thespheres.betula.server.beans.CalendarsBean;
import org.thespheres.betula.server.beans.SigneeLocal;
import org.thespheres.betula.server.beans.StudentsListsLocalBean;
import org.thespheres.betula.server.beans.StudentsLocalBean;
import org.thespheres.betula.services.ws.CommonDocuments;
import org.thespheres.ical.VCard;

/**
 * REST Web Service
 *
 * @author boris.heithecker
 */
@Path("internal")
public class InternalAPI {

//    @Context
//    private UriInfo context;
    @Inject
    private UnitDocumentFacade ubean;
    @Context
    private SecurityContext securityContext;
    @EJB
    protected SigneeLocal signeeLocal;
    @EJB
    protected TicketFacade tickets;
    @EJB
    protected GradeTargetDocumentFacade facade;
    @EJB
    protected TextTargetDocumentFacade textFacade;
    @EJB(beanName = "StudentVCardsImpl")
    private StudentsLocalBean studentVCardsImpl;
    @EJB(beanName = "CalendarsBeanImpl")
    private CalendarsBean calendars;
    @EJB(beanName = "StudentsListsLocalBeanImpl")
    private StudentsListsLocalBean studentsLists;
    @Inject
    private CommonDocuments commonDocuments;

    @GET
    @Path("unit-common-name")
    @Produces(MediaType.TEXT_PLAIN)
    public String getUnitCommonName(
            @QueryParam("studentId") DocumentId cNames,
            @QueryParam("authority") UnitId unit) {
        return ubean.getCommonName(cNames, unit);
    }

    @GET
    @Path("signee-common-name")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSigneeCommonName(
            @QueryParam("signee") Signee signee) {
        return signeeLocal.getSigneeCommonName(signee);
    }

    @GET
    @Path("signee-groups")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSigneeGroups(
            @QueryParam("signee") Signee signee) {
        final String[] ret = signeeLocal.getSigneeGroups(signee);
        return ret != null ? Arrays.stream(ret)
                .collect(Collectors.joining(",")) : null;
    }

    @GET
    @Path("signee-primary-unit")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSigneePrimaryUnit(
            @QueryParam("primary-unit-head-teachers-document") DocumentId klassenlehrerDoc,
            @QueryParam("signee") Signee signee) {
        final UnitId ret = ubean.getPrimaryUnit(klassenlehrerDoc, signee);
        return ret != null ? ret.toString() : UnitId.NULL.toString();
    }

    @GET
    @Path("applicable-tickets")
    @Produces(MediaType.TEXT_PLAIN)
    public String findApplicableTickets(
            @QueryParam("target-document") DocumentId target,
            @QueryParam("term") TermId term,
            @QueryParam("student") StudentId student) {
        final Ticket[] ret = tickets.getTickets(target, term, student, "entitled.signee").stream().map(BaseTicketEntity::getTicket).toArray(Ticket[]::new);
        return Arrays.stream(ret)
                .map(Identity::toString)
                .collect(Collectors.joining("\n"));
    }

    @GET
    @Path("select")
    @Produces(MediaType.TEXT_PLAIN)
    public String selectSingle(
            @QueryParam("target") DocumentId target,
            @QueryParam("student") StudentId student,
            @QueryParam("term") TermId term) {
        final GradeTargetAssessmentEntity e = facade.find(target, LockModeType.OPTIMISTIC);
        if (e != null) {
            final Grade ret = e.select(student, term);
            return ret != null ? ret.toString() : null;
        }
        return null;
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    @POST
    @Path("submit-grade-value")
    @Produces(MediaType.TEXT_PLAIN)
    public Response submitSingleGradeValue(
            @QueryParam("target") DocumentId target,
            @QueryParam("student") StudentId student,
            @QueryParam("term") TermId term,
            @QueryParam("grade") Grade grade) {
        if (target != null && student != null && term != null) {
            final boolean ret = facade.submit(target, student, term, grade, null);
            return Response.ok(Boolean.toString(ret)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN)
                    .entity("target, student and term are required").build();
        }
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    @POST
    @Path("submit-text-value")
    @Produces(MediaType.TEXT_PLAIN)
    public Response submitSingleTextValue(
            @QueryParam("target") DocumentId target,
            @QueryParam("student") StudentId student,
            @QueryParam("term") TermId term,
            @QueryParam("section") Marker section,
            @QueryParam("text") String text) {
        if (target != null && student != null && term != null) {
            final boolean ret = textFacade.submit(target, student, term, section, text, null, -1);
            return Response.ok(Boolean.toString(ret)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN)
                    .entity("target, student and term are required").build();
        }
    }

    @GET
    @Path("student-vcard")
    @Produces(VCard.MIME)
    public Response getStudentVCard(@QueryParam("student") StudentId student) {
        final VCard ret = studentVCardsImpl.get(student);
        return Response.ok(ret.toString())
                .build();
    }

    //Wird nicht gebraucht!!
    public Collection<VCard> getAllVCards() {
        final Collection<VCard> ret = studentVCardsImpl.getAll();
        return ret;
    }

    @GET
    @Path("report-date")
    public Date getReportDate(
            @QueryParam("category") String category,
            @QueryParam("unit") UnitId unit,
            @QueryParam("term") TermId termId,
            @QueryParam("document") DocumentId zgn,
            @QueryParam("cat") String moreCategories) throws AmbiguousDateException {
        final String[] cat = StringUtils.split(moreCategories, ",");
        return calendars.getDate(category, unit, termId, zgn, cat);
    }

    @GET
    @Path("student-primary-unit")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getStudentPrimaryUnit(@QueryParam("student") StudentId student, @QueryParam("asOf") Date asOf) {
        if (student == null) {
            return badRequest("student is required");
        }
        return textOrNoContent(studentsLists.findPrimaryUnit(student, asOf));
    }

    @GET
    @Path("student-marker-entry")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getStudentMarkerEntry(@QueryParam("student") StudentId student, @QueryParam("document") DocumentId document, @QueryParam("asOf") Date asOf) {
        if (student == null || document == null) {
            return badRequest("student and document are required");
        }
        return textOrNoContent(studentsLists.getMarkerEntry(student, document, asOf));
    }

    @GET
    @Path("subject-alternative-name")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getSubjectAlternativeName(@QueryParam("document") DocumentId document) {
        if (document == null) {
            return badRequest("document is required");
        }
        final DocumentId names = commonDocuments.forName(CommonDocuments.SUBJECT_NAMES_DOCID);
        return textOrNoContent(names != null ? facade.getStringValue(names, document) : null);
    }

    private static Response badRequest(String msg) {
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(msg).build();
    }

    private static Response textOrNoContent(Object o) {
        return o == null ? Response.noContent().build() : Response.ok(o.toString()).build();
    }

    @GET
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "Hello from " + Optional.ofNullable(securityContext.getUserPrincipal())
                .map(Principal::getName)
                .orElse("unknown");
    }
}
