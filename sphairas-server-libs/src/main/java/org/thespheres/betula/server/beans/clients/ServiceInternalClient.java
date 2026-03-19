package org.thespheres.betula.server.beans.clients;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;

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

    @GET
    @Path("submit-grade-value")
    @Produces(MediaType.TEXT_PLAIN)
    public String submitSingleGradeValue(
            @QueryParam("target") DocumentId target,
            @QueryParam("student") StudentId student,
            @QueryParam("term") TermId term,
            @QueryParam("grade") Grade grade);

    @GET
    @Path("submit-text-value")
    @Produces(MediaType.TEXT_PLAIN)
    public String submitSingleTextValue(
            @QueryParam("target") DocumentId target,
            @QueryParam("student") StudentId student,
            @QueryParam("term") TermId term,
            @QueryParam("section") Marker section,
            @QueryParam("text") String text);

    @GET
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String sendPing();
}
