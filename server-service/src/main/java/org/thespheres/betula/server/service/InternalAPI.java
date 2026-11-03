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
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.Consumes;
import jakarta.persistence.LockModeType;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
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
import org.thespheres.betula.server.beans.FastTargetDocuments2;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;
import org.thespheres.betula.server.beans.annot.DocumentsRequest;
import org.thespheres.betula.server.beans.clients.ActingSubject;
import org.thespheres.betula.server.beans.clients.TargetDocumentMarkers;
import org.thespheres.betula.services.ws.CommonDocuments;
import org.thespheres.ical.VCard;

/**
 * REST Web Service
 *
 * @author boris.heithecker
 */
@Path("internal")
@RolesAllowed("internal")
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
    @Inject
    @DocumentsRequest
    private Instance<FastTargetDocuments2> fastTargetDocuments;
    @Inject
    private ActingSubject actingSubject;

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

    @GET
    @Path("target-documents/fast-term")
    @Produces(MediaType.APPLICATION_JSON)
    public FastTermTargetDocument getFastTermTargetDocument(
            @QueryParam("document") DocumentId document,
            @HeaderParam("X-Betula-Signee") Signee actingSignee,
            @HeaderParam("X-Betula-Roles") String actingRoles) {
        setActingSubject(actingSignee, actingRoles);
        return fastTargetDocuments.get().getFastTermTargetDocument(document);
    }

    @GET
    @Path("target-documents/fast-text-term")
    @Produces(MediaType.APPLICATION_JSON)
    public FastTextTermTargetDocument getFastTextTermTargetDocument(
            @QueryParam("document") DocumentId document,
            @HeaderParam("X-Betula-Signee") Signee actingSignee,
            @HeaderParam("X-Betula-Roles") String actingRoles) {
        setActingSubject(actingSignee, actingRoles);
        return fastTargetDocuments.get().getFastTextTermTargetDocument(document);
    }

    @GET
    @Path("target-documents/by-unit")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] getTargetAssessmentDocuments(
            @QueryParam("unit") UnitId unit,
            @HeaderParam("X-Betula-Signee") Signee actingSignee,
            @HeaderParam("X-Betula-Roles") String actingRoles) {
        setActingSubject(actingSignee, actingRoles);
        return fastTargetDocuments.get().getTargetAssessmentDocuments(unit).stream()
                .map(DocumentId::toString)
                .toArray(String[]::new);
    }

    @GET
    @Path("target-documents/by-unit-term")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] getTargetAssessmentDocumentsForTerm(
            @QueryParam("unit") UnitId unit,
            @QueryParam("term") TermId term,
            @HeaderParam("X-Betula-Signee") Signee actingSignee,
            @HeaderParam("X-Betula-Roles") String actingRoles) {
        setActingSubject(actingSignee, actingRoles);
        return fastTargetDocuments.get().getTargetAssessmentDocumentsForTerm(unit, term).stream()
                .map(DocumentId::toString)
                .toArray(String[]::new);
    }

    @POST
    @Path("target-documents/by-unit-term/with-map")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String[] getTargetAssessmentDocumentsForTermWithMap(
            @QueryParam("unit") UnitId unit,
            @QueryParam("term") TermId term,
            Map<String, FastTermTargetDocument> targetDocuments,
            @HeaderParam("X-Betula-Signee") Signee actingSignee,
            @HeaderParam("X-Betula-Roles") String actingRoles) {
        setActingSubject(actingSignee, actingRoles);
        final Map<DocumentId, FastTermTargetDocument> documents = targetDocuments.entrySet().stream()
                .collect(Collectors.toMap(entry -> DocumentId.valueOf(entry.getKey()), Map.Entry::getValue));
        return fastTargetDocuments.get().getTargetAssessmentDocumentsForTerm(unit, term, documents).stream()
                .map(DocumentId::toString)
                .toArray(String[]::new);
    }

    @POST
    @Path("target-documents/text/by-unit-term")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String[] getTextTargetAssessmentDocumentsForTerm(
            @QueryParam("unit") UnitId unit,
            @QueryParam("term") TermId term,
            Map<String, FastTextTermTargetDocument> targetDocuments,
            @HeaderParam("X-Betula-Signee") Signee actingSignee,
            @HeaderParam("X-Betula-Roles") String actingRoles) {
        setActingSubject(actingSignee, actingRoles);
        final Map<DocumentId, FastTextTermTargetDocument> documents = targetDocuments.entrySet().stream()
                .collect(Collectors.toMap(entry -> DocumentId.valueOf(entry.getKey()), Map.Entry::getValue));
        return fastTargetDocuments.get().getTextTargetAssessmentDocumentsForTerm(unit, term, documents).stream()
                .map(DocumentId::toString)
                .toArray(String[]::new);
    }

    @GET
    @Path("target-documents/markers")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Marker> getTargetDocumentMarkers(
            @QueryParam("document") DocumentId document,
            @HeaderParam("X-Betula-Signee") Signee actingSignee,
            @HeaderParam("X-Betula-Roles") String actingRoles) {
        setActingSubject(actingSignee, actingRoles);
        return fastTargetDocuments.get().getDocumentMarkers(document);
    }

    @POST
    @Path("target-documents/markers/batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TargetDocumentMarkers[] getTargetDocumentMarkers(
            String[] documents,
            @HeaderParam("X-Betula-Signee") Signee actingSignee,
            @HeaderParam("X-Betula-Roles") String actingRoles) {
        setActingSubject(actingSignee, actingRoles);
        return Arrays.stream(documents)
                .map(DocumentId::valueOf)
                .map(document -> new TargetDocumentMarkers(document.toString(),
                        Optional.ofNullable(fastTargetDocuments.get().getDocumentMarkers(document))
                                .orElseGet(java.util.Collections::emptySet).stream()
                                .map(Marker::toString)
                                .toList()))
                .toArray(TargetDocumentMarkers[]::new);
    }

    @GET
    @Path("target-documents/students")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] getTargetDocumentStudents(
            @QueryParam("unit") UnitId unit,
            @QueryParam("asOf") Date asOf,
            @HeaderParam("X-Betula-Signee") Signee actingSignee,
            @HeaderParam("X-Betula-Roles") String actingRoles) {
        setActingSubject(actingSignee, actingRoles);
        return fastTargetDocuments.get().getStudents(unit, asOf).stream()
                .map(StudentId::toString)
                .toArray(String[]::new);
    }

    @GET
    @Path("target-documents/find-single")
    @Produces(MediaType.TEXT_PLAIN)
    public String findSingle(
            @QueryParam("student") StudentId student,
            @QueryParam("term") TermId term,
            @QueryParam("subject") Marker subject,
            @QueryParam("suffix") String suffix,
            @HeaderParam("X-Betula-Signee") Signee actingSignee,
            @HeaderParam("X-Betula-Roles") String actingRoles) {
        setActingSubject(actingSignee, actingRoles);
        final Grade result = fastTargetDocuments.get().findSingle(student, term, subject, suffix);
        return result != null ? result.toString() : null;
    }

    @POST
    @Path("target-documents/find-single-checked")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Grade[] findSingleChecked(
            @QueryParam("unit") UnitId unit,
            @QueryParam("term") TermId term,
            @QueryParam("student") StudentId student,
            String[] documents,
            @HeaderParam("X-Betula-Signee") Signee actingSignee,
            @HeaderParam("X-Betula-Roles") String actingRoles) {
        setActingSubject(actingSignee, actingRoles);
        return fastTargetDocuments.get().findSingleChecked(unit, term, student,
                Arrays.stream(documents).map(DocumentId::valueOf).toList());
    }

    private void setActingSubject(final Signee signee, final String roles) {
        if (StringUtils.isBlank(roles)) {
            throw new BadRequestException("Acting roles are required.");
        }
        final Set<String> assertedRoles = Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toUnmodifiableSet());
        if (assertedRoles.isEmpty() || !Set.of("signee", "unitadmin").containsAll(assertedRoles)) {
            throw new BadRequestException("Acting roles must be signee and/or unitadmin.");
        }
        if (assertedRoles.contains("signee") && signee == null) {
            throw new BadRequestException("An acting signee is required for the signee role.");
        }
        if (signee == null && !assertedRoles.contains("unitadmin")) {
            throw new BadRequestException("A signee is required unless acting as unitadmin.");
        }
        actingSubject.set(signee, assertedRoles);
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
