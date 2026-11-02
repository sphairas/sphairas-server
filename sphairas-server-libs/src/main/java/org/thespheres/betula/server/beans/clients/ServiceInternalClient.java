package org.thespheres.betula.server.beans.clients;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.server.beans.AmbiguousDateException;
import org.thespheres.betula.server.beans.TermReportDataException;
import org.thespheres.ical.VCard;

@Path("internal")
public interface ServiceInternalClient {

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
            @QueryParam("cat") String moreCategories) throws BadRequest, AmbiguousDateException;

    @GET
    @Path("reports")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] findReports(@QueryParam("student") StudentId student, @QueryParam("term") TermId term);

    @POST
    @Path("reports")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] findOrCreateReports(@QueryParam("student") StudentId student, @QueryParam("term") TermId term);

    @GET
    @Path("reports/student")
    @Produces(MediaType.TEXT_PLAIN)
    public String getReportStudent(@QueryParam("document") DocumentId document);

    @GET
    @Path("reports/term")
    @Produces(MediaType.TEXT_PLAIN)
    public String getReportTerm(@QueryParam("document") DocumentId document);

    @GET
    @Path("reports/ags")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] getReportAgs(@QueryParam("student") StudentId student, @QueryParam("term") TermId term);

    @GET
    @Path("reports/kopfnote")
    @Produces(MediaType.TEXT_PLAIN)
    public String getReportKopfnote(@QueryParam("document") DocumentId document, @QueryParam("convention") String convention);

    @PUT
    @Path("reports/kopfnote")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String setReportKopfnote(@QueryParam("document") DocumentId document, @QueryParam("convention") String convention, String grade) throws TermReportDataException;

    @GET
    @Path("reports/integers")
    @Produces(MediaType.TEXT_PLAIN)
    public String getReportInteger(@QueryParam("document") DocumentId document, @QueryParam("type") String type);

    @PUT
    @Path("reports/integers")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String setReportInteger(@QueryParam("document") DocumentId document, @QueryParam("type") String type, String value) throws TermReportDataException;

    @GET
    @Path("reports/markers")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] getReportMarkers(@QueryParam("document") DocumentId document);

    @GET
    @Path("reports/markers/batch")
    @Produces(MediaType.APPLICATION_JSON)
    public ReportMarkers[] getReportMarkers(@QueryParam("document") List<DocumentId> documents);

    @POST
    @Path("reports/markers")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String addReportMarker(@QueryParam("document") DocumentId document, String marker);

    @DELETE
    @Path("reports/markers")
    @Produces(MediaType.TEXT_PLAIN)
    public String removeReportMarker(@QueryParam("document") DocumentId document, @QueryParam("marker") String marker);

    @GET
    @Path("reports/notes")
    @Produces(MediaType.TEXT_PLAIN)
    public String getReportNote(@QueryParam("document") DocumentId document, @QueryParam("key") String key);

    @GET
    @Path("reports/notes/batch")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getReportNotes(@QueryParam("document") DocumentId document, @QueryParam("key") List<String> keys);

    @PUT
    @Path("reports/notes")
    @Consumes(MediaType.TEXT_PLAIN)
    public jakarta.ws.rs.core.Response setReportNote(@QueryParam("document") DocumentId document, @QueryParam("key") String key, String value);

    @GET
    @Path("reports/custom-notes")
    @Produces(MediaType.APPLICATION_JSON)
    public ReportCustomNote[] getReportCustomNotes(@QueryParam("document") DocumentId document);

    @PUT
    @Path("reports/custom-notes")
    @Consumes(MediaType.APPLICATION_JSON)
    public jakarta.ws.rs.core.Response setReportCustomNotes(@QueryParam("document") DocumentId document, ReportCustomNote[] notes);

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
