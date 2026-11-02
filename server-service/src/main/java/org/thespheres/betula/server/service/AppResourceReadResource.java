package org.thespheres.betula.server.service;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.thespheres.betula.services.ServiceConstants;

/**
 * Read-only access to the app resources directory for the web WAR (role
 * internal). Replaces the JNDI AppResourcesContext lookup.
 */
@Path("internal/app-resource")
public class AppResourceReadResource {

    @GET
    public Response get(@QueryParam("path") String path) {
        if (path == null || path.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity("path is required").build();
        }
        final java.nio.file.Path base = java.nio.file.Path.of(System.getProperty("com.sun.aas.instanceRoot"), ServiceConstants.APP_RESOURCES)
                .toAbsolutePath().normalize();
        final java.nio.file.Path file = base.resolve(path).toAbsolutePath().normalize();
        if (!file.startsWith(base)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        if (!Files.isRegularFile(file)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            return Response.ok(file.toFile())
                    .header(HttpHeaders.LAST_MODIFIED, new Date(Files.getLastModifiedTime(file).toMillis()))
                    .type(MediaType.APPLICATION_OCTET_STREAM)
                    .build();
        } catch (IOException ex) {
            Logger.getLogger(AppResourceReadResource.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
            return Response.serverError().build();
        }
    }
}
