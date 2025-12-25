package org.thespheres.server.clients;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
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
