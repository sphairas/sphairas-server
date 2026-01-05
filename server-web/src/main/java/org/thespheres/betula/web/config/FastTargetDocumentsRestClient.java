package org.thespheres.betula.web.config;

import jakarta.enterprise.context.Dependent;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;

/**
 * REST Client interface for FastTargetDocuments service Replace EJB @Local with
 * MicroProfile REST Client
 */
@Dependent
@RegisterRestClient(configKey = "fast-target-documents")
@Path("/api/fast-target-documents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface FastTargetDocumentsRestClient {

    @GET
    @Path("/fast-term-target-document")
    FastTermTargetDocument getFastTermTargetDocument(@QueryParam("documentId") DocumentId d);

    @GET
    @Path("/fast-text-term-target-document")
    FastTextTermTargetDocument getFastTextTermTargetDocument(@QueryParam("documentId") DocumentId d);

    @GET
    @Path("/target-assessment-documents")
    Collection<DocumentId> getTargetAssessmentDocuments();

    @GET
    @Path("/students")
    Collection<StudentId> getStudents();

    @GET
    @Path("/primary-unit-students")
    Collection<StudentId> getPrimaryUnitStudents(@QueryParam("docIdName") String docIdName);

    @GET
    @Path("/units")
    Collection<UnitId> getUnits();

    @GET
    @Path("/primary-unit")
    UnitId getPrimaryUnit(@QueryParam("docIdName") String docIdName);

    @GET
    @Path("/target-assessment-documents/by-unit")
    Collection<DocumentId> getTargetAssessmentDocumentsByUnit(@QueryParam("primaryUnit") UnitId primaryUnit);

    @GET
    @Path("/target-assessment-documents-for-term")
    Collection<DocumentId> getTargetAssessmentDocumentsForTerm(
            @QueryParam("unit") UnitId unit,
            @QueryParam("term") TermId term);

    @GET
    @Path("/document-markers")
    Collection<Marker> getDocumentMarkers(@QueryParam("documentId") DocumentId d);

    @GET
    @Path("/students/by-unit-date")
    Collection<StudentId> getStudentsByUnitDate(
            @QueryParam("unit") UnitId pu,
            @QueryParam("asOf") Date asOf);

    @GET
    @Path("/tickets")
    Ticket[] getTickets(
            @QueryParam("docId") DocumentId docId,
            @QueryParam("termId") TermId termId,
            @QueryParam("studId") StudentId studId);

    @GET
    @Path("/single")
    Grade selectSingle(
            @QueryParam("documentId") DocumentId d,
            @QueryParam("student") StudentId student,
            @QueryParam("term") TermId term);

    @GET
    @Path("/single/by-marker")
    Grade findSingle(
            @QueryParam("student") StudentId student,
            @QueryParam("term") TermId term,
            @QueryParam("fach") Marker fach,
            @QueryParam("suffix") String suffix);

    @POST
    @Path("/single/grade")
    boolean submitSingleGrade(
            @QueryParam("docId") DocumentId docId,
            @QueryParam("studId") StudentId studId,
            @QueryParam("termId") TermId termId,
            @QueryParam("grade") Grade grade);

    @POST
    @Path("/single/text")
    boolean submitSingleText(
            @QueryParam("docId") DocumentId docId,
            @QueryParam("studId") StudentId studId,
            @QueryParam("termId") TermId termId,
            @QueryParam("section") Marker section,
            @QueryParam("text") String text);

    @POST
    @Path("/single-checked")
    Grade[] findSingleChecked(
            @QueryParam("unit") UnitId unit,
            @QueryParam("term") TermId term,
            @QueryParam("student") StudentId student,
            Collection<DocumentId> selectFrom);

    @POST
    @Path("/target-assessment-documents-for-term/with-map")
    Collection<DocumentId> getTargetAssessmentDocumentsForTermWithMap(
            @QueryParam("unit") UnitId unit,
            @QueryParam("term") TermId term,
            Map<DocumentId, FastTermTargetDocument> map);

    @POST
    @Path("/text-target-assessment-documents-for-term")
    Collection<DocumentId> getTextTargetAssessmentDocumentsForTerm(
            @QueryParam("unit") UnitId unit,
            @QueryParam("term") TermId term,
            Map<DocumentId, FastTextTermTargetDocument> map);
}
