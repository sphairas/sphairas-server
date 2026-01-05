package org.thespheres.betula.server.service;

import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.io.OutputStream;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.services.ws.BetulaWebService;

/**
 * REST Web Service
 *
 * @author boris.heithecker@gmx.net
 */
@Path("units")
public class UnitsServiceResource {
    
    @Context
    private UriInfo context;
    @Context
    private SecurityContext securityContext;
    @Inject
    private AppResources appResources;
    
    @EJB(beanName = "BetulaAdminService")
    private BetulaWebService delegate;

    /**
     * Retrieves representation of an instance of
     * org.thespheres.betula.web.rest.ContainerService
     *
     * @param requestContainer
     * @return an instance of java.lang.String
     */
    @Path("solicit")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response solicit(final InputStream in) throws Exception {
        javax.xml.bind.JAXBContext jaxb = appResources.getJAXBContext();
//        if (!securityContext.isUserInRole("unitadmin")) {
//            throw new IllegalAccessException();
//        }
        System.out.println("solicit, user: " + securityContext.getUserPrincipal().getName() + ", in role unitadmin " + securityContext.isUserInRole("unitadmin"));
        Container requestContainer = (Container) jaxb.createUnmarshaller().unmarshal(in);
        Container returnContainer = delegate.solicit(requestContainer);
        final StreamingOutput stream = (OutputStream os) -> {
            try {
                jaxb.createMarshaller().marshal(returnContainer, os);
            } catch (javax.xml.bind.JAXBException ex) {
                throw new WebApplicationException(ex);
            }
        };
        return Response.ok()
                .entity(stream)
                .build();
    }
    
}
