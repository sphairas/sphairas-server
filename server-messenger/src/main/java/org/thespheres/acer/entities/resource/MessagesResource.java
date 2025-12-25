package org.thespheres.acer.entities.resource;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.thespheres.betula.server.beans.FastMessage;
import org.thespheres.betula.server.beans.FastMessages;

/**
 *
 * @author boris.heithecker
 */
@RolesAllowed("signee")
@Path("/messages")
@RequestScoped
public class MessagesResource {

    @Inject
    private FastMessages fastMessages;

    @GET
    @Produces("application/json")
    public JsonArray getMessages() {
        List<FastMessage> l = fastMessages.getFastMessages(true);
        JsonBuilderFactory fac = Json.createBuilderFactory(null);
        JsonArrayBuilder ab = fac.createArrayBuilder();
        l.forEach(fm -> {
            JsonObjectBuilder ob = fac.createObjectBuilder();
            JsonObjectBuilder idb = fac.createObjectBuilder();
//            idb.add("authority", fm.getMessageId().getAuthority());
//            idb.add("id", fm.getMessageId().getId());
            ob.add("message-id", idb);
            ob.add("channel", fm.getChannel());
            ob.add("text", fm.getFormattedMessageText());
            ob.add("author", fm.getAuthorDisplayName());
            ob.add("confidential", fm.isConfidential());
            ab.add(ob);
        });
        return ab.build();
    }

    @GET
    @Path("{authority}/{message-id}")
    @Produces("application/json")
    public Response getMessage(@PathParam("message-authority") String authority, @PathParam("message-id") Long id) { //@QueryParam("message.authority") String authority, @QueryParam("message.id") Long id) {
//        final MessageId mid = new MessageId(authority, id, Version.LATEST);
//        fastMessages.delete(mid);
        return Response.ok().build();
    }

    @DELETE
    @Path("{authority}/{message-id}")
    public Response delete(@PathParam("message-authority") String authority, @PathParam("message-id") Long id) { //@QueryParam("message.authority") String authority, @QueryParam("message.id") Long id) {
//        final MessageId mid = new MessageId(authority, id, Version.LATEST);
//        fastMessages.delete(mid);
        return Response.ok().build();
    }
}
