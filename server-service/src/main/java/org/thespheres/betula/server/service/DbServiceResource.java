package org.thespheres.betula.server.service;

import jakarta.annotation.security.RolesAllowed;
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
import jakarta.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.bind.JAXBException;
import org.thespheres.betula.database.DBAdminTask;
import org.thespheres.betula.database.DBAdminTaskResult;
import org.thespheres.betula.database.DbAdminService;

/**
 * REST Web Service
 *
 * @author boris.heithecker@gmx.net
 */
@Path("db")
@RolesAllowed({"superadmin", "unitadmin"})
public class DbServiceResource {

    @Context
    private UriInfo context;
    @EJB(beanName = "DbAdminServiceEndpoint")
    private DbAdminService delegate;
    @Inject
    private AppResources appResources;

    @Path("submit-task")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response submitTask(final InputStream in) throws Exception {
        javax.xml.bind.JAXBContext jaxb = appResources.getJAXBContext();
        DBAdminTask task = (DBAdminTask) jaxb.createUnmarshaller().unmarshal(in);
        DBAdminTaskResult ret = delegate.submitTask(task);
        final StreamingOutput stream = (OutputStream os) -> {
            try {
                jaxb.createMarshaller().marshal(ret, os);
            } catch (JAXBException ex) {
                throw new WebApplicationException(ex);
            }
        };
        return Response.ok()
                .entity(stream)
                .build();
    }
}
