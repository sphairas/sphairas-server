/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.resource;

import java.util.List;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.thespheres.acer.MessageId;
import org.thespheres.acer.beans.MessageContent;
import org.thespheres.acer.beans.MessageObject;
import org.thespheres.acer.entities.DurableMessage;
import org.thespheres.acer.entities.StaticChannel;
import org.thespheres.acer.entities.UnitChannel;
import org.thespheres.acer.entities.facade.ChannelFacade;
import org.thespheres.betula.UnitId;

/**
 *
 * @author boris.heithecker
 */
@Stateless
@Path("message")
public class AdminMessagesResource {

    @PersistenceContext(unitName = "messagingPU")
    private EntityManager em;
    @EJB
    private ChannelFacade facade;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response allMessages() {
        CriteriaQuery<DurableMessage> cq = em.getCriteriaBuilder().createQuery(DurableMessage.class);
        cq.select(cq.from(DurableMessage.class));
        List<DurableMessage> res = em.createQuery(cq).setLockMode(LockModeType.OPTIMISTIC).getResultList();
        final List<MessageObject> ret = res.stream()
                .map(dm -> new MessageObject(dm.getId(), dm.getChannel().getName(), dm.getBaseText(), false, dm.getCreator(), dm.getCreationTime(), dm.getPriority()))
                .collect(Collectors.toList());
        final GenericEntity<List<MessageObject>> entity = new GenericEntity<List<MessageObject>>(ret) {
        };
        return Response.ok(entity, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("{message-authority}/{message-id}")
    @Produces({MediaType.APPLICATION_JSON})
    public MessageObject message(@PathParam("message-authority") String authority, @PathParam("message-id") long id, @QueryParam("version") String v) {
        final MessageId mid = RestUtilities.messageId(authority, id, v);
        final DurableMessage dm = facade.findMessage(mid, DurableMessage.class, LockModeType.READ);
        if (dm != null) {
            return new MessageObject(dm.getId(), dm.getChannel().getName(), dm.getBaseText(), false, dm.getCreator(), dm.getCreationTime(), dm.getPriority());
        }
        return null;
    }

    @PUT
    @Path("{channel}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public MessageId publishMessage(@PathParam("channel") String channel, final MessageContent mc) {
        return publish(channel, mc);
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public MessageId publishPrimaryUnitMessage(@QueryParam("unit-authority") String ua, @QueryParam("unit-id") String ui, final MessageContent mc) {
        final UnitId unit = new UnitId(ua, ui);
        UnitChannel uc = facade.find(unit.getId(), UnitChannel.class, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (uc == null) {
            uc = new UnitChannel(unit, true);
            facade.create(uc);
        }
        return publishMessage(uc.getName(), mc);
    }

    private MessageId publish(final String channel, final MessageContent mc) {
        if ("static-public-all".equals(channel)) {
            StaticChannel dc = facade.find(channel, StaticChannel.class, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            if (dc == null) {
                facade.create(new StaticChannel(channel, "Allgemein"));
            }
        }
        final DurableMessage dm = facade.publish(channel, mc.getText(), mc.getPriority(), mc.isIsTextEncoded());
        return dm.getId();
    }

    @DELETE
    @Path("{message-authority}/{message-id}")
    public void deleteMessage(@PathParam("message-authority") String authority, @PathParam("message-id") long id, @QueryParam("version") String v) {
        final MessageId message = RestUtilities.messageId(authority, id, v);
        facade.delete(message);
    }

}
