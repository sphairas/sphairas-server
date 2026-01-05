package org.thespheres.betula.server.service;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.FastTargetDocuments2;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;
import org.thespheres.betula.server.beans.annot.DocumentsRequest;

/**
 * REST Service implementation for FastTargetDocuments
 * Server-side endpoints that implement the REST interface
 */
//@RequestScoped
@Path("/api/fast-target-documents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FastTargetDocumentsRestService {

    @Inject
    @DocumentsRequest
    private FastTargetDocuments2 fastTargetDocuments;

    @GET
    @Path("/fast-term-target-document")
    public Response getFastTermTargetDocument(@QueryParam("documentId") DocumentId d) {
        try {
            FastTermTargetDocument result = fastTargetDocuments.getFastTermTargetDocument(d);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/fast-text-term-target-document")
    public Response getFastTextTermTargetDocument(@QueryParam("documentId") DocumentId d) {
        try {
            FastTextTermTargetDocument result = fastTargetDocuments.getFastTextTermTargetDocument(d);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/target-assessment-documents")
    public Response getTargetAssessmentDocuments() {
        try {
            Collection<DocumentId> result = fastTargetDocuments.getTargetAssessmentDocuments();
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/students")
    public Response getStudents() {
        try {
            Collection<StudentId> result = fastTargetDocuments.getStudents();
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/primary-unit-students")
    public Response getPrimaryUnitStudents(@QueryParam("docIdName") String docIdName) {
        try {
            Collection<StudentId> result = fastTargetDocuments.getPrimaryUnitStudents(docIdName);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/units")
    public Response getUnits() {
        try {
            Collection<UnitId> result = fastTargetDocuments.getUnits();
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/primary-unit")
    public Response getPrimaryUnit(@QueryParam("docIdName") String docIdName) {
        try {
            UnitId result = fastTargetDocuments.getPrimaryUnit(docIdName);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/target-assessment-documents/by-unit")
    public Response getTargetAssessmentDocumentsByUnit(@QueryParam("primaryUnit") UnitId primaryUnit) {
        try {
            Collection<DocumentId> result = fastTargetDocuments.getTargetAssessmentDocuments(primaryUnit);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/target-assessment-documents-for-term")
    public Response getTargetAssessmentDocumentsForTerm(
            @QueryParam("unit") UnitId unit, 
            @QueryParam("term") TermId term) {
        try {
            Collection<DocumentId> result = fastTargetDocuments.getTargetAssessmentDocumentsForTerm(unit, term);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/document-markers")
    public Response getDocumentMarkers(@QueryParam("documentId") DocumentId d) {
        try {
            Collection<Marker> result = fastTargetDocuments.getDocumentMarkers(d);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/students/by-unit-date")
    public Response getStudentsByUnitDate(
            @QueryParam("unit") UnitId pu, 
            @QueryParam("asOf") Date asOf) {
        try {
            Collection<StudentId> result = fastTargetDocuments.getStudents(pu, asOf);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/tickets")
    public Response getTickets(
            @QueryParam("docId") DocumentId docId, 
            @QueryParam("termId") TermId termId, 
            @QueryParam("studId") StudentId studId) {
        try {
            Ticket[] result = fastTargetDocuments.getTickets(docId, termId, studId);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/single")
    public Response selectSingle(
            @QueryParam("documentId") DocumentId d, 
            @QueryParam("student") StudentId student, 
            @QueryParam("term") TermId term) {
        try {
            Grade result = fastTargetDocuments.selectSingle(d, student, term);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/single/by-marker")
    public Response findSingle(
            @QueryParam("student") StudentId student, 
            @QueryParam("term") TermId term, 
            @QueryParam("fach") Marker fach, 
            @QueryParam("suffix") String suffix) {
        try {
            Grade result = fastTargetDocuments.findSingle(student, term, fach, suffix);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/single/grade")
    public Response submitSingleGrade(
            @QueryParam("docId") DocumentId docId, 
            @QueryParam("studId") StudentId studId, 
            @QueryParam("termId") TermId termId, 
            @QueryParam("grade") Grade grade) {
        try {
            boolean result = fastTargetDocuments.submitSingle(docId, studId, termId, grade);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/single/text")
    public Response submitSingleText(
            @QueryParam("docId") DocumentId docId, 
            @QueryParam("studId") StudentId studId, 
            @QueryParam("termId") TermId termId, 
            @QueryParam("section") Marker section, 
            @QueryParam("text") String text) {
        try {
            boolean result = fastTargetDocuments.submitSingle(docId, studId, termId, section, text);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/single-checked")
    public Response findSingleChecked(
            @QueryParam("unit") UnitId unit, 
            @QueryParam("term") TermId term, 
            @QueryParam("student") StudentId student, 
            Collection<DocumentId> selectFrom) {
        try {
            Grade[] result = fastTargetDocuments.findSingleChecked(unit, term, student, selectFrom);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/target-assessment-documents-for-term/with-map")
    public Response getTargetAssessmentDocumentsForTermWithMap(
            @QueryParam("unit") UnitId unit, 
            @QueryParam("term") TermId term, 
            Map<DocumentId, FastTermTargetDocument> map) {
        try {
            Collection<DocumentId> result = fastTargetDocuments.getTargetAssessmentDocumentsForTerm(unit, term, map);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/text-target-assessment-documents-for-term")
    public Response getTextTargetAssessmentDocumentsForTerm(
            @QueryParam("unit") UnitId unit, 
            @QueryParam("term") TermId term, 
            Map<DocumentId, FastTextTermTargetDocument> map) {
        try {
            Collection<DocumentId> result = fastTargetDocuments.getTextTargetAssessmentDocumentsForTerm(unit, term, map);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }
}