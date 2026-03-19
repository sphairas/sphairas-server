package org.thespheres.betula.server.service;

import jakarta.ejb.EJB;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Path;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
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
import org.thespheres.betula.server.beans.SigneeLocal;

/**
 * REST Web Service
 *
 * @author boris
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
    @Path("signee-primary-unit")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSigneePrimaryUnit(
            @QueryParam("primary-unit-head-teachers-document") DocumentId klassenlehrerDoc,
            @QueryParam("signee") Signee signee) {
        final UnitId ret = ubean.getPrimaryUnit(klassenlehrerDoc, signee);
        return ret.toString();
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
            return ret.toString();
        }
        return null;
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    @GET
    @Path("submit-grade-value")
    @Produces(MediaType.TEXT_PLAIN)
    public String submitSingleGradeValue(
            @QueryParam("target") DocumentId target,
            @QueryParam("student") StudentId student,
            @QueryParam("term") TermId term,
            @QueryParam("grade") Grade grade) {
        if (target != null && student != null && term != null) {
            final boolean ret = facade.submit(target, student, term, grade, null);
            return Boolean.toString(ret);
        } else {
            throw new IllegalArgumentException("DocumentId, StudentId, and TermId cannot be null.");
        }
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    @GET
    @Path("submit-text-value")
    @Produces(MediaType.TEXT_PLAIN)
    public String submitSingleTextValue(
            @QueryParam("target") DocumentId target,
            @QueryParam("student") StudentId student,
            @QueryParam("term") TermId term,
            @QueryParam("section") Marker section,
            @QueryParam("text") String text) {
        if (target != null && student != null && term != null) {
            final boolean ret = textFacade.submit(target, student, term, section, text, null, -1);
            return Boolean.toString(ret);
        } else {
            throw new IllegalArgumentException("DocumentId, StudentId, and TermId cannot be null.");
        }
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
