/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeFactory;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.document.util.TargetAssessmentEntry;
import org.thespheres.betula.document.util.TicketEntry;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.server.clients.model.BaseDocument;
import org.thespheres.server.clients.model.BaseTargetDocument;
import org.thespheres.server.clients.model.TargetDocument;
import org.thespheres.server.clients.model.TargetDocumentEntry;
import org.thespheres.server.clients.model.TargetDocumentEntry.DocTicketEntry;

/**
 * REST Web Service
 *
 * @author boris
 */
@Path("documents/targets")
@RolesAllowed("signee")
@RequestScoped//SessionScoped?
public class TargetsResource {

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
    public BaseTargetDocument[] getDocuments(@QueryParam("group") final boolean group) {
        final Map<DocumentId, Set<UnitId>> dd = this.service.getDocuments();
        if (group) {
            final Map<DocumentId, List<DocumentId>> mapped = dd.keySet().stream()
                    .collect(Collectors.groupingBy(dm::convert));
            return mapped.entrySet().stream()
                    .map(e -> new BaseTargetDocument(e.getKey(), e.getValue().stream().collect(Collectors.toMap(v -> v, dd::get)), config))
                    .peek(this::updateName)
                    .toArray(BaseTargetDocument[]::new);//TODO include 
        } else {
            return dd.entrySet().stream()
                    .map(e -> new BaseTargetDocument(e.getKey(), e.getValue(), config))
                    .peek(this::updateName)
                    .toArray(BaseTargetDocument[]::new);//TODO include 
        }
    }

    //FastTermTargetDocument FastTargetDocuments2.getFastTermTargetDocument(DocumentId d);
    @GET
    @Path("/{target}")
    @Produces(MediaType.APPLICATION_JSON)
    public TargetDocument getDocument(@PathParam("target") String did, @HeaderParam("If-Modified-Since") String userAgent, @Context final HttpServletResponse resp) throws IOException {
        final DocumentId doc = DocumentId.resolve(did, config.getAuthority(), DocumentId.Version.LATEST);
        final TargetAssessmentEntry<TermId> entry;
        final Map<TermId, Map<StudentId, List<TicketEntry>>> tickets;
        try {
            entry = service.getDocument(doc);
            tickets = service.getTargetDocumentTickets(doc);
        } catch (IOException ioex) {
            logger.log(Level.SEVERE, "An excetion has occured getting " + did, ioex);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        //TODO return proper representation object
        final TargetDocument ret = createDocument(doc, entry, tickets);
        if (ret == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        final String lm = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        resp.setHeader("Last-Modified", lm);
        return ret;
    }

    protected void updateName(final BaseDocument doc) {
        try {
            final String name = naming.resolveDisplayName(doc.getIdentity());
            doc.setName(name);
        } catch (final IllegalAuthorityException illex) {
            logger.log(Level.WARNING, "Could not resolve " + doc.toString(), illex);
        }
    }

    protected TargetDocument createDocument(final DocumentId doc, final TargetAssessmentEntry<TermId> entry, final Map<TermId, Map<StudentId, List<TicketEntry>>> tickets) throws IOException {
        final TargetDocument ret = new TargetDocument(doc, config);
        updateName(ret);
        for (final StudentId stud : entry.students()) {
            for (final TermId term : entry.identities()) {
                final Grade select = entry.select(stud, term);
                if (select != null) {
                    final Long time = Optional.ofNullable(entry.timestamp(stud, term))
                            .map(ts -> ts.getValue().getTime())
                            .orElse(null);
                    final TargetDocumentEntry res = ret.addEntry(stud, term, select, time);
                    final DocTicketEntry[] tt = tickets.getOrDefault(term, (Map<StudentId, List<TicketEntry>>) Collections.EMPTY_MAP)
                            .getOrDefault(stud, (List<TicketEntry>) Collections.EMPTY_LIST)
                            .stream()
                            .map(te -> new DocTicketEntry(Long.toString(te.getIdentity().getId()), te.getValue().getTicketClass().getValue()))
                            .toArray(DocTicketEntry[]::new);
                    if (tt.length != 0) {
                        res.setTickets(tt);
                    }
                }
            }
        }
        ret.setConvention(entry.getPreferredConvention());
        ret.setSubjectAltName(entry.getSubjectAlternativeName());
        ret.setTargetType(entry.getTargetType());
        ret.setExpirationDate(entry.getDocumentValidity());
        ret.setMarkers(entry.getValue().markers());
        final Map<String, Signee> signees = entry.getValue().getSigneeInfos().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getSignee()));
        ret.setMatchingSigneeTypes(signees);
        return ret;
    }

    @POST
    @Path("/{target}/{term}/{student}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response submit(@PathParam("target") String did, @PathParam("term") String term, @PathParam("student") String student, @FormParam("value") final String value, @Context final HttpServletResponse resp) throws IOException {
//        final DocumentId doc = DocumentId.resolve(URLDecoder.decode(did, "utf-8"));
//        final TermId tid = TermId.resolve(URLDecoder.decode(term, "utf-8"));
//        final StudentId sid = StudentId.resolve(URLDecoder.decode(student, "utf-8"));
        final DocumentId doc = DocumentId.resolve(did);
        final TermId tid = TermId.resolve(term);
        final StudentId sid = StudentId.resolve(student);
        final Grade grade = StringUtils.isBlank(value) ? null : GradeFactory.resolveAbstract(value);
        service.submitValue(doc, tid, sid, grade);
        return Response.ok().build();
    }

}
