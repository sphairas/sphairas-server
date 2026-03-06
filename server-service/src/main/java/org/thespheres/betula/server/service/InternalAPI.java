package org.thespheres.betula.server.service;

import jakarta.ejb.EJB;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Path;
import jakarta.inject.Inject;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Optional;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Signee;
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
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "Hello from " + Optional.ofNullable(securityContext.getUserPrincipal())
                .map(Principal::getName)
                .orElse("unknown");
    }
}
