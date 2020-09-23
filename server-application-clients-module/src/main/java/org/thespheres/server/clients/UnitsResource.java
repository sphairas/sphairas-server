/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients;

import java.io.IOException;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
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
