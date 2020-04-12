package org.thespheres.acer.entities.resource;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
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
