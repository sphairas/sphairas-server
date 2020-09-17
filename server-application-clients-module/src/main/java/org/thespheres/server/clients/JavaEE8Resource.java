package org.thespheres.server.clients;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.thespheres.clientauth.JWTCallerPrincipal;

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
    public Response pingSecure(@Context final SecurityContext ctx) {
        final JWTCallerPrincipal principal = (JWTCallerPrincipal) ctx.getUserPrincipal();
        return Response
                .ok("ping-secure with " + principal.getToken())
                .build();
    }
}
