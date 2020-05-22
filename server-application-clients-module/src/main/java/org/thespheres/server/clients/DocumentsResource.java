/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.server.clients.model.Document;

/**
 * REST Web Service
 *
 * @author boris
 */
@Path("documents")
@RolesAllowed("signee")
@RequestScoped//SessionScoped?
public class DocumentsResource {

    @Context
    private UriInfo context;
    @Inject
    private ServiceClientBean service;
    @Inject
    private ClientConfiguration config;

    @GET
    @Path("/{target}")
    @Produces(MediaType.APPLICATION_JSON)
    public Document getJson(@PathParam("target") String did, @Context final HttpServletRequest request) {
        final DocumentId doc = ClientConfiguration.parseDocumentId(did, config::getAuthority);
//        final TargetAssessmentEntry<TermId> entry = service.getDocument(doc);
        //TODO return proper representation object
        throw new UnsupportedOperationException();
    }

}
