/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients;

import java.io.IOException;
import java.util.logging.Logger;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.server.clients.model.BaseTargetDocument;
import org.thespheres.server.clients.model.TargetDocument;

/**
 * REST Web Service
 *
 * @author boris
 */
@Path("documents/units")
@RolesAllowed("signee")
@RequestScoped//SessionScoped?
public class UnitsResource {

    @Context
    private UriInfo context;
    @Inject
    private ServiceClientBean service;
    @Inject
    @SessionScoped
    private ClientConfiguration config;
    @Inject
    private NamingResolver naming;
    @Inject
    private DocumentsModel dm;
    @Inject
    private Logger logger;

    //FastTargetDocuments2.getTargetAssessmentDocuments()
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public BaseTargetDocument[] getDocuments() {
        return null;
    }

    //FastTermTargetDocument FastTargetDocuments2.getFastTermTargetDocument(DocumentId d);
    @GET
    @Path("/{unit}")
    @Produces(MediaType.APPLICATION_JSON)
    public TargetDocument getDocument(@PathParam("unit") String did, @HeaderParam("If-Modified-Since") String userAgent, @Context final HttpServletResponse resp) throws IOException {
        return null;
    }

}
