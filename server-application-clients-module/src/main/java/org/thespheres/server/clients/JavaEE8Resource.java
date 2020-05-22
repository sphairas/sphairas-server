package org.thespheres.server.clients;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 *
 * @author
 */
@Path("ping")
public class JavaEE8Resource {

    @GET
    public Response ping() {
        return Response
                .ok("ping")
                .build();
    }

    @RolesAllowed("signee")
    @Path("secure")
    @GET
    public Response pingSecure() {
        return Response
                .ok("ping-secure")
                .build();
    }
}
