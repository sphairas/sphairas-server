package org.thespheres.betula.server.beans.clients;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;

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
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String sendPing();
}
