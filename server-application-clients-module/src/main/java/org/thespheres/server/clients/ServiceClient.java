/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients;

import java.io.IOException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.services.ws.BetulaWebService;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.UnauthorizedException;

/**
 *
 * @author boris
 */
@RolesAllowed("signee")
@Path("service")
public class ServiceClient {

    @Inject
    private BetulaWebService service;

    @POST
    @Path("solicit")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Container solicit(final Container container, @Context final HttpServletResponse response) throws IOException {
        try {
            return service.solicit(container);
        } catch (final NotFoundException ex) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, ex.getLocalizedMessage());
        } catch (final UnauthorizedException ex) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getLocalizedMessage());
        } catch (final SyntaxException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getLocalizedMessage());
        }
        return null;
    }
}
