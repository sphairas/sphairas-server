package org.thespheres.betula.calendar.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.calendar.facade.StudentFacade;
import org.thespheres.betula.server.beans.Utilities;
import org.thespheres.betula.services.dav.AddressData;
import org.thespheres.betula.services.dav.CardDavProp;
import org.thespheres.betula.services.dav.Multistatus;
import org.thespheres.betula.services.dav.PropStat;
import org.thespheres.betula.services.vcard.VCardStudentsCollection;
import org.thespheres.ical.VCard;

@Stateless
@Path("students")
public class StudentsResource {

    private static JAXBContext collectionJAXB;
    private static JAXBContext multiStatusJAXB;

    static JAXBContext getCollectionJAXB() {
        if (collectionJAXB == null) {
            try {
                collectionJAXB = JAXBContext.newInstance(VCardStudentsCollection.class);
            } catch (JAXBException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return collectionJAXB;
    }

    static JAXBContext getMultiStatusJAXB() {
        if (multiStatusJAXB == null) {
            try {
                multiStatusJAXB = JAXBContext.newInstance(Multistatus.class, CardDavProp.class);
            } catch (JAXBException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return multiStatusJAXB;
    }
    @EJB
    private StudentFacade facade;
    @Context
    private HttpServletRequest request;

    @GET
    public Response get() throws IOException {
        return createListResponse();
    }

    @REPORT
    public Response report() throws IOException {
        return createListResponse();
    }

    @DELETE
    public Response delete() {
        final StudentId student = Utilities.extractStudentId(request);
        if (student == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        facade.remove(student);
        return Response
                .noContent()
                .build();
    }

    @POST
    public Response post() throws IOException {
        final VCardStudentsCollection coll;
        try (InputStream is = request.getInputStream()) {
            coll = (VCardStudentsCollection) getCollectionJAXB().createUnmarshaller().unmarshal(is);
        } catch (JAXBException ex) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        coll.getAll().forEach(this::updateVCard);
        return Response
                .ok()
                .build();
    }

    private Response createListResponse() throws IOException {
        final UnitId unit = Utilities.extractUnitId(request);
        final boolean json = Optional.ofNullable(request.getHeader("Accept"))
                .map(val -> val.split(","))
                .map(arr -> Arrays.stream(arr).map(String::trim).anyMatch("application/json"::equals))
                .orElse(false);
        final Collection<VCard> cards = unit != null ? facade.findAllVCards(unit) : facade.findAllVCards();
        return json ? writeJson(cards) : writeMultistatus(cards);
    }

    private Response writeMultistatus(final Collection<VCard> cards) throws IOException {
        final Multistatus ms = new Multistatus();
        cards.forEach(card -> {
            final String vcard = card.toString();
            final org.thespheres.betula.services.dav.Response resp = new org.thespheres.betula.services.dav.Response(card.getAnyPropertyValue("X-STUDENT").get());
            final PropStat ps = new PropStat();
            ps.setStatus("HTTP/1.1 200 OK");
            final CardDavProp prop = new CardDavProp();
            prop.setAddressData(new AddressData(vcard));
            ps.setProp(prop);
            resp.getPropstat().add(ps);
            ms.getResponses().add(resp);
        });
        try {
            final StringWriter writer = new StringWriter();
            getMultiStatusJAXB().createMarshaller().marshal(ms, writer);
            return Response.status(207)
                    .type("application/xml;charset=UTF-8")
                    .entity(writer.toString())
                    .build();
        } catch (JAXBException ex) {
            throw new IOException(ex);
        }
    }

    private Response writeJson(final Collection<VCard> cards) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        cards.stream().map(VCard::toString).forEach(builder::add);
        final StringWriter sw = new StringWriter();
        try (JsonWriter writer = Json.createWriter(sw)) {
            writer.writeArray(builder.build());
        }
        return Response
                .ok(sw.toString(), "application/json;charset=UTF-8")
                .build();
    }

    private void updateVCard(final StudentId id, final VCard card) {
        facade.create(id, card.getFN());
        card.getPropertyNames()
                .forEach(n -> card.getProperties(n).forEach(p -> facade.update(id, p)));
    }

}
