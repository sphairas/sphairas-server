package org.thespheres.betula.server.beans.clients;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Date;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.ical.VCard;

//Annotation wird in Payara Server gebraucht, in Payara Micro angeblich nicht
//@Dependent
//@RegisterRestClient(configKey = "persistence-internal")
//Funktioniert noch nicht, weil ein Hostname Verfier gesetzt werden muss. 
//Sollte später mit die Konfiguration über einen configKey ersetzt werden. 
//@RegisterRestClient(baseUri = "https://localhost:8181/service/api")
@Path("internal")
public interface ServiceInternalClient {

    public static final String URI_SERVICE_API = "https://localhost:8181/service/api";

    @GET
    @Path("unit-common-name")
    @Produces(MediaType.TEXT_PLAIN)
    public String getUnitCommonName(
            @QueryParam("studentId") DocumentId cNames,
            @QueryParam("authority") UnitId unit);

    @GET
    @Path("signee-common-name")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSigneeCommonName(
            @QueryParam("signee") Signee signee);

    @GET
    @Path("signee-groups")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSigneeGroups(
            @QueryParam("signee") Signee signee);

    @GET
    @Path("signee-primary-unit")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSigneePrimaryUnit(
            @QueryParam("primary-unit-head-teachers-document") DocumentId klassenlehrerDoc,
            @QueryParam("signee") Signee signee);

    @GET
    @Path("applicable-tickets")
    @Produces(MediaType.TEXT_PLAIN)
    public String findApplicableTickets(
            @QueryParam("target-document") DocumentId target,
            @QueryParam("term") TermId term,
            @QueryParam("student") StudentId student);

    @GET
    @Path("select")
    @Produces(MediaType.TEXT_PLAIN)
    public String selectSingle(
            @QueryParam("target") DocumentId target,
            @QueryParam("student") StudentId student,
            @QueryParam("term") TermId term);

    @POST
    @Path("submit-grade-value")
    @Produces(MediaType.TEXT_PLAIN)
    public String submitSingleGradeValue(
            @QueryParam("target") DocumentId target,
            @QueryParam("student") StudentId student,
            @QueryParam("term") TermId term,
            @QueryParam("grade") Grade grade);

    @POST
    @Path("submit-text-value")
    @Produces(MediaType.TEXT_PLAIN)
    public String submitSingleTextValue(
            @QueryParam("target") DocumentId target,
            @QueryParam("student") StudentId student,
            @QueryParam("term") TermId term,
            @QueryParam("section") Marker section,
            @QueryParam("text") String text);

    @GET
    @Path("student-vcard")
    @Produces(VCard.MIME)
    public String getStudentVCard(@QueryParam("student") StudentId student);

    @GET
    @Path("report-date")
    public Date getReportDate(
            @QueryParam("category") String category,
            @QueryParam("unit") UnitId unit,
            @QueryParam("term") TermId termId,
            @QueryParam("document") DocumentId zgn,
            @QueryParam("cat") String moreCategories) throws BadRequest;

    /** @return unit id text, or null (204) if the student has no primary unit */
    @GET
    @Path("student-primary-unit")
    @Produces(MediaType.TEXT_PLAIN)
    public String getStudentPrimaryUnit(@QueryParam("student") StudentId student, @QueryParam("asOf") Date asOf);

    /** @return marker text, or null (204) */
    @GET
    @Path("student-marker-entry")
    @Produces(MediaType.TEXT_PLAIN)
    public String getStudentMarkerEntry(@QueryParam("student") StudentId student, @QueryParam("document") DocumentId document, @QueryParam("asOf") Date asOf);

    /** @return alternative subject name, or null (204) */
    @GET
    @Path("subject-alternative-name")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSubjectAlternativeName(@QueryParam("document") DocumentId document);

    /** Read-only access to the app resources directory; 404 if the file does not exist. */
    @GET
    @Path("app-resource")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public jakarta.ws.rs.core.Response getAppResource(@QueryParam("path") String path);

    @GET
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String sendPing();
}
