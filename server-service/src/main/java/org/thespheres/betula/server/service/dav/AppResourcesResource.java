package org.thespheres.betula.server.service.dav;

import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.thespheres.betula.services.ServiceConstants;

/**
 * JAX-RS replacement for the WebDAV-based AppResourcesServlet.
 *
 * Supports GET (files), PUT, POST (create/update) and PROPFIND (directory
 * listings). All paths are resolved under the configured app-resources
 * directory and validated against path-traversal attacks.
 *
 * Accessible at /api/dav/... alongside the legacy /dav/... WebDAV
 * servlet during the transition period.
 */
@Stateless
@Path("dav")
public class AppResourcesResource {

    private static final Logger LOGGER = Logger.getLogger(AppResourcesResource.class.getName());

    @Context
    private UriInfo uriInfo;

    // ---------------------------------------------------------------------------
    // Base-directory resolution
    // ---------------------------------------------------------------------------
    private java.nio.file.Path resolveBase() {
        return java.nio.file.Path.of(
                System.getProperty("com.sun.aas.instanceRoot"),
                ServiceConstants.APP_RESOURCES);
    }

    /**
     * Resolves a client-supplied relative path safely under the base directory.
     * Throws 403 Forbidden if the resolved path escapes the base (path-traversal
     * prevention).
     */
    private java.nio.file.Path resolveSafe(String relativePath) {
        final java.nio.file.Path base = resolveBase().toAbsolutePath().normalize();
        if (relativePath == null || relativePath.isEmpty()) {
            return base;
        }
        final java.nio.file.Path resolved = base.resolve(relativePath).toAbsolutePath().normalize();
        if (!resolved.startsWith(base)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return resolved;
    }

    // ---------------------------------------------------------------------------
    // GET – retrieve a single file
    // ---------------------------------------------------------------------------
    @GET
    @Path("{path: .+}")
    public Response get(@PathParam("path") String path) {
        final java.nio.file.Path file = resolveSafe(path);
        if (!Files.exists(file) || Files.isDirectory(file)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            final BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            final Date lastModified = new Date(attrs.lastModifiedTime().toMillis());
            String contentType = Files.probeContentType(file);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }
            return Response.ok(file.toFile())
                    .header(HttpHeaders.LAST_MODIFIED, lastModified)
                    .type(contentType)
                    .build();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return Response.serverError().build();
        }
    }

    // ---------------------------------------------------------------------------
    // PUT / POST – create or replace a file
    // ---------------------------------------------------------------------------
    @PUT
    @Path("{path: .+}")
    public Response put(@PathParam("path") String path, InputStream body) {
        final java.nio.file.Path file = resolveSafe(path);
        try {
            Files.createDirectories(file.getParent());
            final boolean created = !Files.exists(file);
            Files.copy(body, file, StandardCopyOption.REPLACE_EXISTING);
            return created
                    ? Response.created(uriInfo.getAbsolutePath()).build()
                    : Response.noContent().build();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return Response.serverError().build();
        }
    }

    @POST
    @Path("{path: .+}")
    public Response post(@PathParam("path") String path, InputStream body) {
        return put(path, body);
    }

    // ---------------------------------------------------------------------------
    // PROPFIND – list a directory (WebDAV multistatus, depth 1)
    // ---------------------------------------------------------------------------
    @PROPFIND
    @Produces(MediaType.APPLICATION_XML)
    public Response propfindRoot() {
        return propfind(null);
    }

    @PROPFIND
    @Path("{path: .+}")
    @Produces(MediaType.APPLICATION_XML)
    public Response propfind(@PathParam("path") String path) {
        final java.nio.file.Path dir = resolveSafe(path);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            final String xml = buildMultistatusXml(dir, path);
            return Response.status(207)   // 207 Multi-Status
                    .entity(xml)
                    .type(MediaType.APPLICATION_XML)
                    .build();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return Response.serverError().build();
        }
    }

    // ---------------------------------------------------------------------------
    // WebDAV XML helpers
    // ---------------------------------------------------------------------------
    private String buildMultistatusXml(java.nio.file.Path dir, String basePath) throws IOException {
        final StringBuilder sb = new StringBuilder(512);
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<D:multistatus xmlns:D=\"DAV:\">\n");
        try (Stream<java.nio.file.Path> children = Files.list(dir)) {
            children.sorted().forEach(child -> {
                try {
                    appendEntry(sb, child, basePath);
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Error listing resource " + child, ex);
                }
            });
        }
        sb.append("</D:multistatus>");
        return sb.toString();
    }

    private void appendEntry(StringBuilder sb, java.nio.file.Path file, String basePath) throws IOException {
        final BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
        final String name = file.getFileName().toString();

        UriBuilder hrefBuilder = uriInfo.getBaseUriBuilder().path(AppResourcesResource.class);
        if (basePath != null && !basePath.isEmpty()) {
            hrefBuilder = hrefBuilder.path(basePath);
        }
        final String href = hrefBuilder.path(name).build().toASCIIString();
        final String lastModified = formatHttpDate(new Date(attrs.lastModifiedTime().toMillis()));

        sb.append("  <D:response>\n");
        sb.append("    <D:href>").append(escapeXml(href)).append("</D:href>\n");
        sb.append("    <D:propstat>\n");
        sb.append("      <D:prop>\n");
        sb.append("        <D:displayname>").append(escapeXml(name)).append("</D:displayname>\n");
        sb.append("        <D:getlastmodified>").append(lastModified).append("</D:getlastmodified>\n");
        if (attrs.isDirectory()) {
            sb.append("        <D:resourcetype><D:collection/></D:resourcetype>\n");
        } else {
            sb.append("        <D:resourcetype/>\n");
            sb.append("        <D:getcontentlength>").append(attrs.size()).append("</D:getcontentlength>\n");
            final String contentType = Files.probeContentType(file);
            if (contentType != null) {
                sb.append("        <D:getcontenttype>").append(escapeXml(contentType)).append("</D:getcontenttype>\n");
            }
        }
        sb.append("      </D:prop>\n");
        sb.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
        sb.append("    </D:propstat>\n");
        sb.append("  </D:response>\n");
    }

    private static String formatHttpDate(final Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(date);
    }

    private static String escapeXml(final String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
