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
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.thespheres.betula.services.ServiceConstants;
import org.thespheres.betula.services.dav.ActiveLock;
import org.thespheres.betula.services.dav.Collection;
import org.thespheres.betula.services.dav.DAVProp;
import org.thespheres.betula.services.dav.DisplayName;
import org.thespheres.betula.services.dav.GetContentLength;
import org.thespheres.betula.services.dav.GetContentType;
import org.thespheres.betula.services.dav.GetLastModified;
import org.thespheres.betula.services.dav.LockDiscovery;
import org.thespheres.betula.services.dav.LockEntry;
import org.thespheres.betula.services.dav.LockScope;
import org.thespheres.betula.services.dav.LockType;
import org.thespheres.betula.services.dav.Multistatus;
import org.thespheres.betula.services.dav.PropStat;
import org.thespheres.betula.services.dav.ResourceType;
import org.thespheres.betula.services.dav.SupportedLock;

/**
 * JAX-RS replacement for the WebDAV-based AppResourcesServlet.
 *
 * Supports GET (files), PUT, POST (create/update), PROPFIND and
 * basic WebDAV LOCK/UNLOCK token handling.
 */
@Stateless
@Path("dav")
public class AppResourcesResource {

    private static final Logger LOGGER = Logger.getLogger(AppResourcesResource.class.getName());
    private static final long DEFAULT_LOCK_TIMEOUT_SECONDS = 600L;
    private static final ConcurrentMap<String, DavLock> LOCKS = new ConcurrentHashMap<>();
    private static JAXBContext davJAXB;

    @Context
    private UriInfo uriInfo;
    @Context
    private HttpHeaders headers;

    private static JAXBContext getDavJAXB() {
        if (davJAXB == null) {
            try {
                davJAXB = JAXBContext.newInstance(Multistatus.class, DAVProp.class);
            } catch (JAXBException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return davJAXB;
    }

    private java.nio.file.Path resolveBase() {
        return java.nio.file.Path.of(System.getProperty("com.sun.aas.instanceRoot"), ServiceConstants.APP_RESOURCES);
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
        final Response locked = requireWriteLockToken(file);
        if (locked != null) {
            return locked;
        }
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

    @LOCK
    @Produces(MediaType.APPLICATION_XML)
    public Response lockRoot(InputStream body) {
        return lock(null, body);
    }

    @LOCK
    @Path("{path: .+}")
    @Produces(MediaType.APPLICATION_XML)
    public Response lock(@PathParam("path") String path, InputStream body) {
        final java.nio.file.Path target = resolveSafe(path);
        final String key = lockKey(target);
        cleanupExpiredLock(key);

        final String providedToken = providedLockToken();
        final long timeout = parseTimeoutSeconds(headers.getHeaderString("Timeout"));
        final DavLock current = LOCKS.get(key);
        if (current != null) {
            if (providedToken == null || !Objects.equals(current.token, providedToken)) {
                return Response
                        .status(423)
                        .build();
            }
            final DavLock refreshed = current.refresh(timeout);
            LOCKS.put(key, refreshed);
            return lockResponse(Response.Status.OK, refreshed);
        }

        final String owner = extractOwner(body);
        final DavLock lock = DavLock.create(owner, timeout);
        LOCKS.put(key, lock);
        return lockResponse(Response.Status.CREATED, lock);
    }

    @UNLOCK
    public Response unlockRoot() {
        return unlock(null);
    }

    @UNLOCK
    @Path("{path: .+}")
    public Response unlock(@PathParam("path") String path) {
        final java.nio.file.Path target = resolveSafe(path);
        final String key = lockKey(target);
        final DavLock current = activeLock(target);
        if (current == null) {
            return Response
                    .noContent()
                    .build();
        }
        final String providedToken = providedLockToken();
        if (providedToken == null || !Objects.equals(current.token, providedToken)) {
            return Response
                    .status(Response.Status.CONFLICT)
                    .build();
        }
        LOCKS.remove(key, current);
        return Response
                .noContent()
                .build();
    }

    @PROPFIND
    @Produces(MediaType.APPLICATION_XML)
    public Response propfindRoot() {
        return propfind(null);
    }

    @PROPFIND
    @Path("{path: .+}")
    @Produces(MediaType.APPLICATION_XML)
    public Response propfind(@PathParam("path") String path) {
        final java.nio.file.Path target = resolveSafe(path);
        if (!Files.exists(target)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        final int depth;
        try {
            depth = parsePropfindDepth(headers.getHeaderString("Depth"));
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            final String xml = buildMultistatusXml(target, depth);
            return Response.status(207)
                    .entity(xml)
                    .type(MediaType.APPLICATION_XML)
                    .build();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return Response
                    .serverError()
                    .build();
        }
    }

    private String buildMultistatusXml(java.nio.file.Path target, int depth) throws IOException {
        final Multistatus ms = new Multistatus();
        appendEntry(ms, target);

        if (depth > 0 && Files.isDirectory(target)) {
            if (depth == 1) {
                try (Stream<java.nio.file.Path> children = Files.list(target)) {
                    children.sorted().forEach(child -> {
                        try {
                            appendEntry(ms, child);
                        } catch (IOException ex) {
                            LOGGER.log(Level.WARNING, "Error listing resource " + child, ex);
                        }
                    });
                }
            } else {
                appendDescendantsRecursive(ms, target);
            }
        }

        try {
            final StringWriter writer = new StringWriter();
            getDavJAXB().createMarshaller().marshal(ms, writer);
            return writer.toString();
        } catch (JAXBException ex) {
            throw new IOException(ex);
        }
    }

    private void appendEntry(Multistatus ms, java.nio.file.Path file) throws IOException {
        final BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
        final String name = file.getFileName().toString();
        final String href = hrefForPath(file);

        final org.thespheres.betula.services.dav.Response response = new org.thespheres.betula.services.dav.Response(href);
        final PropStat ps = new PropStat();
        ps.setStatus("HTTP/1.1 200 OK");
        final DAVProp prop = new DAVProp();

        final DisplayName displayName = new DisplayName();
        displayName.setValue(name);
        prop.setDisplayName(displayName);

        final GetLastModified glm = new GetLastModified();
        glm.setValue(formatHttpDate(new Date(attrs.lastModifiedTime().toMillis())));
        prop.setGetLastModified(glm);

        final ResourceType resourceType = new ResourceType();
        if (attrs.isDirectory()) {
            resourceType.setCollection(new Collection());
        }
        prop.setResourcetype(resourceType);

        if (!attrs.isDirectory()) {
            final GetContentLength gcl = new GetContentLength();
            gcl.setValue(Long.toString(attrs.size()));
            prop.setGetContentLength(gcl);
            final String contentType = Files.probeContentType(file);
            if (contentType != null) {
                final GetContentType gct = new GetContentType();
                gct.setValue(contentType);
                prop.setGetContentType(gct);
            }
        }

        final DavLock lock = activeLock(file);
        final LockDiscovery discovery = lock != null ? lock.toDiscovery() : new LockDiscovery();
        prop.setLockDiscovery(discovery);

        final SupportedLock supportedLock = new SupportedLock();
        final LockEntry entry = new LockEntry();
        entry.setLockScope(new LockScope());
        entry.setLockType(new LockType());
        supportedLock.getLockEntry().add(entry);
        prop.setSupportedLock(supportedLock);

        ps.setProp(prop);
        response.getPropstat().add(ps);
        ms.getResponses().add(response);
    }

    private void appendDescendantsRecursive(Multistatus ms, java.nio.file.Path root) throws IOException {
        try (Stream<java.nio.file.Path> children = Files.list(root)) {
            for (java.nio.file.Path child : children.sorted().toList()) {
                appendEntry(ms, child);
                if (Files.isDirectory(child)) {
                    appendDescendantsRecursive(ms, child);
                }
            }
        }
    }

    private Response lockResponse(Response.Status status, DavLock lock) {
        try {
            final StringWriter writer = new StringWriter();
            final DAVProp prop = new DAVProp();
            prop.setLockDiscovery(lock.toDiscovery());

            final SupportedLock supportedLock = new SupportedLock();
            final LockEntry entry = new LockEntry();
            entry.setLockScope(new LockScope());
            entry.setLockType(new LockType());
            supportedLock.getLockEntry().add(entry);
            prop.setSupportedLock(supportedLock);

            getDavJAXB().createMarshaller().marshal(prop, writer);
            return Response.status(status)
                    .header("Lock-Token", "<" + lock.token + ">")
                    .type(MediaType.APPLICATION_XML)
                    .entity(writer.toString())
                    .build();
        } catch (JAXBException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return Response.serverError().build();
        }
    }

    private String hrefForPath(java.nio.file.Path file) {
        final java.nio.file.Path base = resolveBase().toAbsolutePath().normalize();
        final java.nio.file.Path abs = file.toAbsolutePath().normalize();
        final String relative = abs.equals(base) ? "" : base.relativize(abs).toString().replace('\\', '/');
        UriBuilder hrefBuilder = uriInfo.getBaseUriBuilder().path(AppResourcesResource.class);
        if (!relative.isEmpty()) {
            hrefBuilder = hrefBuilder.path(relative);
        }
        return hrefBuilder.build().toASCIIString();
    }

    private int parsePropfindDepth(String depthHeader) {
        if (depthHeader == null || depthHeader.isBlank()) {
            return Integer.MAX_VALUE;
        }
        final String value = depthHeader.trim().toLowerCase(Locale.ROOT);
        if ("0".equals(value)) {
            return 0;
        }
        if ("1".equals(value)) {
            return 1;
        }
        if ("infinity".equals(value)) {
            return Integer.MAX_VALUE;
        }
        throw new IllegalArgumentException("Unsupported PROPFIND depth: " + depthHeader);
    }

    private static String formatHttpDate(final Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(date);
    }

    private Response requireWriteLockToken(java.nio.file.Path path) {
        final DavLock lock = activeLock(path);
        if (lock == null) {
            return null;
        }
        final String providedToken = providedLockToken();
        if (!Objects.equals(lock.token, providedToken)) {
            return Response
                    .status(423)
                    .build();
        }
        return null;
    }

    private String lockKey(java.nio.file.Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    private DavLock activeLock(java.nio.file.Path path) {
        final String key = lockKey(path);
        cleanupExpiredLock(key);
        return LOCKS.get(key);
    }

    private void cleanupExpiredLock(String key) {
        final DavLock lock = LOCKS.get(key);
        if (lock != null && lock.isExpired()) {
            LOCKS.remove(key, lock);
        }
    }

    private String providedLockToken() {
        final String ifHeader = headers.getHeaderString("If");
        final String lockTokenHeader = headers.getHeaderString("Lock-Token");
        final String fromIf = extractToken(ifHeader);
        return fromIf != null ? fromIf : extractToken(lockTokenHeader);
    }

    private String extractToken(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        final int start = value.indexOf('<');
        final int end = value.indexOf('>', start + 1);
        if (start >= 0 && end > start) {
            return value.substring(start + 1, end).trim();
        }
        return value.trim();
    }

    private long parseTimeoutSeconds(String timeoutHeader) {
        if (timeoutHeader == null || timeoutHeader.isBlank()) {
            return DEFAULT_LOCK_TIMEOUT_SECONDS;
        }
        final String lower = timeoutHeader.toLowerCase(Locale.ROOT);
        if (lower.contains("infinite")) {
            return Long.MAX_VALUE;
        }
        final int idx = lower.indexOf("second-");
        if (idx >= 0) {
            final String val = timeoutHeader.substring(idx + "second-".length()).trim();
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.FINE, "Invalid timeout: {0}", timeoutHeader);
            }
        }
        return DEFAULT_LOCK_TIMEOUT_SECONDS;
    }

    private String extractOwner(InputStream body) {
        if (body == null) {
            return null;
        }
        try {
            final String xml = new String(body.readAllBytes(), StandardCharsets.UTF_8);
            final String lower = xml.toLowerCase(Locale.ROOT);
            final int start = lower.indexOf("<d:owner>");
            final int end = lower.indexOf("</d:owner>");
            if (start >= 0 && end > start) {
                final String value = xml.substring(start + "<d:owner>".length(), end).trim();
                return value.isEmpty() ? null : value;
            }
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, "Could not parse lock owner", ex);
        }
        return null;
    }

    private static final class DavLock {

        private final String token;
        private final String owner;
        private final long timeoutSeconds;
        private final long createdAt;

        private DavLock(String token, String owner, long timeoutSeconds, long createdAt) {
            this.token = token;
            this.owner = owner;
            this.timeoutSeconds = timeoutSeconds;
            this.createdAt = createdAt;
        }

        static DavLock create(String owner, long timeoutSeconds) {
            return new DavLock("opaquelocktoken:" + UUID.randomUUID(), owner, timeoutSeconds, System.currentTimeMillis());
        }

        DavLock refresh(long timeoutSeconds) {
            return new DavLock(token, owner, timeoutSeconds, System.currentTimeMillis());
        }

        boolean isExpired() {
            if (timeoutSeconds == Long.MAX_VALUE) {
                return false;
            }
            return createdAt + (timeoutSeconds * 1000L) < System.currentTimeMillis();
        }

        LockDiscovery toDiscovery() {
            final LockDiscovery discovery = new LockDiscovery();
            final ActiveLock active = new ActiveLock();
            active.setDepth("infinity");
            active.setTimeout(timeoutSeconds == Long.MAX_VALUE ? "Infinite" : "Second-" + timeoutSeconds);
            active.setLockScope(new LockScope());
            active.setLockType(new LockType());
            final org.thespheres.betula.services.dav.LockToken tokenValue = new org.thespheres.betula.services.dav.LockToken();
            tokenValue.getHref().add(token);
            active.setLockToken(tokenValue);
            if (owner != null && !owner.isBlank()) {
                final org.thespheres.betula.services.dav.Owner ownerValue = new org.thespheres.betula.services.dav.Owner();
                ownerValue.getContent().add(owner);
                active.setOwner(ownerValue);
            }
            discovery.getActiveLock().add(active);
            return discovery;
        }
    }
}
