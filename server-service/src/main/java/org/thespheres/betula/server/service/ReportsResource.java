package org.thespheres.betula.server.service;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeFactory;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.MarkerFactory;
import org.thespheres.betula.server.beans.ReportsBean;
import org.thespheres.betula.server.beans.ReportsBean.CustomNote;
import org.thespheres.betula.server.beans.TermReportDataException;
import org.thespheres.betula.server.beans.clients.ReportMarkers;

/**
 * REST replacement for the ReportsBean EJB. Callers are authenticated as role
 * internal; no acting identity is needed (the EJB performs no role checks).
 * Report documents are passed as query parameter, because ids contain slashes.
 */
@Path("internal/reports")
public class ReportsResource {

    @EJB
    private ReportsBean reports;

    public static class CustomNoteDto {

        public int position;
        public String value;

        public CustomNoteDto() {
        }

        public CustomNoteDto(int position, String value) {
            this.position = position;
            this.value = value;
        }
    }

    private static Response bad(String msg) {
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(msg).build();
    }

    private static Response ok(Object o) {
        return o == null ? Response.noContent().build() : Response.ok(o).build();
    }

    private static Response conflict() {
        return Response.status(Response.Status.CONFLICT)
                .header("X-Betula-Error", "term-report-data")
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response find(@QueryParam("student") StudentId student, @QueryParam("term") TermId term) {
        if (student == null || term == null) {
            return bad("student and term required");
        }
        return Response.ok(Arrays.stream(reports.findTermReports(student, term, false))
                .map(DocumentId::toString).toList()).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response findOrCreate(@QueryParam("student") StudentId student, @QueryParam("term") TermId term) {
        if (student == null || term == null) {
            return bad("student and term required");
        }
        return Response.ok(Arrays.stream(reports.findTermReports(student, term, true))
                .map(DocumentId::toString).toList()).build();
    }

    @GET
    @Path("student")
    @Produces(MediaType.TEXT_PLAIN)
    public Response student(@QueryParam("document") DocumentId zgn) {
        if (zgn == null) {
            return bad("document required");
        }
        final StudentId s = reports.getStudent(zgn);
        return s == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(s.toString()).build();
    }

    @GET
    @Path("term")
    @Produces(MediaType.TEXT_PLAIN)
    public Response term(@QueryParam("document") DocumentId zgn) {
        if (zgn == null) {
            return bad("document required");
        }
        final TermId t = reports.getTerm(zgn);
        return t == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(t.toString()).build();
    }

    @GET
    @Path("ags")
    @Produces(MediaType.APPLICATION_JSON)
    public Response ags(@QueryParam("student") StudentId student, @QueryParam("term") TermId term) {
        if (student == null || term == null) {
            return bad("student and term required");
        }
        return Response.ok(reports.getAGs(student, term)).build();
    }

    @GET
    @Path("kopfnote")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getKopfnote(@QueryParam("document") DocumentId zgn, @QueryParam("convention") String convention) {
        if (zgn == null || convention == null) {
            return bad("document and convention required");
        }
        final Grade g = reports.getKopfnote(zgn, convention);
        return ok(g == null ? null : g.toString());
    }

    @PUT
    @Path("kopfnote")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response setKopfnote(@QueryParam("document") DocumentId zgn, @QueryParam("convention") String convention, String grade) {
        if (zgn == null || convention == null) {
            return bad("document and convention required");
        }
        final Grade g;
        try {
            g = grade == null || grade.isBlank() ? null : GradeFactory.resolve(grade.trim());
        } catch (RuntimeException e) {
            return bad("invalid grade");
        }
        try {
            return Response.ok(Boolean.toString(reports.setKopfnote(zgn, convention, g))).build();
        } catch (TermReportDataException e) {
            return conflict();
        }
    }

    @GET
    @Path("integers")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getInteger(@QueryParam("document") DocumentId zgn, @QueryParam("type") String type) {
        if (zgn == null || type == null) {
            return bad("document and type required");
        }
        final Integer i = reports.getIntegerValue(zgn, type);
        return ok(i == null ? null : i.toString());
    }

    @PUT
    @Path("integers")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response setInteger(@QueryParam("document") DocumentId zgn, @QueryParam("type") String type, String value) {
        if (zgn == null || type == null) {
            return bad("document and type required");
        }
        final Integer i;
        try {
            i = value == null || value.isBlank() ? null : Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return bad("invalid integer");
        }
        try {
            return Response.ok(Boolean.toString(reports.setIntegerValue(zgn, type, i))).build();
        } catch (TermReportDataException e) {
            return conflict();
        }
    }

    @GET
    @Path("markers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMarkers(@QueryParam("document") DocumentId zgn) {
        if (zgn == null) {
            return bad("document required");
        }
        final Marker[] m = reports.getMarkers(zgn);
        final List<String> l = m == null ? List.of() : Arrays.stream(m).map(Marker::toString).collect(Collectors.toList());
        return Response.ok(l).build();
    }

    @GET
    @Path("markers/batch")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMarkersBatch(@QueryParam("document") List<DocumentId> documents) {
        if (documents == null || documents.isEmpty()) {
            return bad("at least one document required");
        }
        final List<ReportMarkers> result = documents.stream()
                .distinct()
                .map(document -> {
                    final Marker[] markers = reports.getMarkers(document);
                    final List<String> values = markers == null ? List.of()
                            : Arrays.stream(markers).map(Marker::toString).toList();
                    return new ReportMarkers(document.toString(), values);
                })
                .toList();
        return Response.ok(result).build();
    }

    @POST
    @Path("markers")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response addMarker(@QueryParam("document") DocumentId zgn, String marker) {
        final Marker m = parse(marker);
        if (zgn == null || m == null) {
            return bad("document and valid marker required");
        }
        return Response.ok(Boolean.toString(reports.addMarker(zgn, m))).build();
    }

    @DELETE
    @Path("markers")
    @Produces(MediaType.TEXT_PLAIN)
    public Response removeMarker(@QueryParam("document") DocumentId zgn, @QueryParam("marker") String marker) {
        final Marker m = parse(marker);
        if (zgn == null || m == null) {
            return bad("document and valid marker required");
        }
        return Response.ok(Boolean.toString(reports.removeMarker(zgn, m))).build();
    }

    private static Marker parse(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return MarkerFactory.resolve(s.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    @GET
    @Path("notes")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getNote(@QueryParam("document") DocumentId zgn, @QueryParam("key") String key) {
        if (zgn == null || key == null) {
            return bad("document and key required");
        }

        return ok(reports.getNote(zgn, key));
    }

    @GET
    @Path("notes/batch")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNotes(@QueryParam("document") DocumentId zgn, @QueryParam("key") List<String> keys) {
        if (zgn == null || keys == null || keys.isEmpty()) {
            return bad("document and at least one key required");
        }
        final Map<String, String> notes = new LinkedHashMap<>();
        keys.stream().distinct().forEach(key -> {
            if (key != null) {
                final String value = reports.getNote(zgn, key);
                if (value != null) {
                    notes.put(key, value);
                }
            }
        });
        return Response.ok(notes).build();
    }

    @PUT
    @Path("notes")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response setNote(@QueryParam("document") DocumentId zgn, @QueryParam("key") String key, String value) {
        if (zgn == null || key == null) {
            return bad("document and key required");
        }
        reports.setNote(zgn, key, value);
        return Response.noContent().build();
    }

    @GET
    @Path("custom-notes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCustomNotes(@QueryParam("document") DocumentId zgn) {
        if (zgn == null) {
            return bad("document required");
        }
        final CustomNote[] n = reports.getCustomNotes(zgn);
        final List<CustomNoteDto> l = n == null ? List.of()
                : Arrays.stream(n).map(c -> new CustomNoteDto(c.getPosition(), c.getValue())).collect(Collectors.toList());
        return Response.ok(l).build();
    }

    @PUT
    @Path("custom-notes")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setCustomNotes(@QueryParam("document") DocumentId zgn, List<CustomNoteDto> notes) {
        if (zgn == null || notes == null) {
            return bad("document and notes required");
        }
        reports.setCustomNotes(zgn, notes.stream().map(d -> new CustomNote(d.position, d.value)).toArray(CustomNote[]::new));
        return Response.noContent().build();
    }
}
