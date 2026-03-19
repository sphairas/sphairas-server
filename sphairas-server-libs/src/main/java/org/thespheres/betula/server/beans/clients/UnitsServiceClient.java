package org.thespheres.betula.server.beans.clients;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.thespheres.betula.document.Container;

/**
 *
 * @author boris.heithecker
 */
@Path("units")
public interface UnitsServiceClient {

    /**
     * Retrieves representation of an instance of
     * org.thespheres.betula.web.rest.ContainerService
     *
     * @param container
     * @return an instance of java.lang.String
     */
    @Path("solicit")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Container solicit(
//            @QueryParam("signee") Signee signee,
            Container container);
}
