/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.resource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
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
import org.thespheres.acer.beans.ChannelObject;
import org.thespheres.acer.entities.BaseChannel;
import org.thespheres.acer.entities.DynamicChannel;
import org.thespheres.acer.entities.PatternChannel;
import org.thespheres.acer.entities.SigneeAction;
import org.thespheres.acer.entities.StaticChannel;
import org.thespheres.acer.entities.StudentAction;
import org.thespheres.acer.entities.StudentsChannel;
import org.thespheres.acer.entities.UnitChannel;
import org.thespheres.acer.entities.facade.ChannelFacade;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.server.beans.ChannelsLocal;

/**
 *
 * @author boris.heithecker
 */
@Stateless
@Path("channel")
public class AdminChannelsResource {

    @EJB
    private ChannelsLocal channelsLocal;
    @PersistenceContext(unitName = "messagingPU")
    private EntityManager em;
    @EJB
    private ChannelFacade facade;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response allChannels() {
        final CriteriaQuery<BaseChannel> cq = em.getCriteriaBuilder().createQuery(BaseChannel.class);
        cq.select(cq.from(BaseChannel.class));
        final List<BaseChannel> res = em.createQuery(cq).setLockMode(LockModeType.OPTIMISTIC).getResultList();
        final List<ChannelObject> ret = res.stream()
                .map(bc -> new ChannelObject(bc.getName(), bc.getClass().getCanonicalName(), displayName(bc)))
                .collect(Collectors.toList());
        final GenericEntity<List<ChannelObject>> entity = new GenericEntity<List<ChannelObject>>(ret) {
        };
        return Response.ok(entity, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("{channel}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getCurrentChannelDisplayName(@PathParam("channel") String channel) {
        BaseChannel bc = facade.find(channel, BaseChannel.class, LockModeType.OPTIMISTIC);
        return displayName(bc);
    }

    @PUT
    @Path("{channel}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createChannel(@PathParam("channel") String name, final JsonObject value) {
        final String pattern = value.getString("pattern", null);
        final String display = value.getString("display-name");
        final JsonArray studs = value.getJsonArray("students");
        if (pattern != null && !pattern.isEmpty() && studs == null) {
            PatternChannel dc = facade.find(name, PatternChannel.class, LockModeType.OPTIMISTIC);
            if (dc == null) {
                facade.create(new PatternChannel(name, display));
            } 
            channelsLocal.updatePattern(name, pattern);
        } else if (pattern == null && studs == null) {
            StaticChannel dc = facade.find(name, StaticChannel.class, LockModeType.OPTIMISTIC);
            if (dc == null) {
                facade.create(new StaticChannel(name, display));
            }
        } else if (studs != null) {
            final Set<StudentId> students = studs.getValuesAs(JsonObject.class).stream()
                    .map(js -> new StudentId(js.getString("authority"), Integer.valueOf(js.getInt("id")).longValue()))
                    .collect(Collectors.toSet());
            StudentsChannel sc = facade.find(name, StudentsChannel.class, LockModeType.OPTIMISTIC);
            if (sc == null) {
                sc = new StudentsChannel(name, display);
                facade.create(sc);
            } else if (display != null) {
                sc.setDefaultDisplayName(display);
            }
            sc.getStudentAction().clear();
            for (final StudentId sid : students) {
                sc.getStudentAction()
                        .add(new StudentAction(sid, StudentAction.Action.INCLUDE));
            }
            facade.edit(sc);
        }
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    public void createUnitChannel(@QueryParam("unit-authority") String ua, @QueryParam("unit-id") String ui, @QueryParam("primary") boolean primary) {
        final UnitId unit = new UnitId(ua, ui);
        UnitChannel uc = facade.find(unit.getId(), UnitChannel.class, LockModeType.OPTIMISTIC);
        if (uc == null) {
            uc = new UnitChannel(unit, primary);
            facade.create(uc);
        }
    }

    @DELETE
    @Path("{channel}")
    public void removeChannel(@PathParam("channel") String name) {
        final BaseChannel bc = facade.find(name, BaseChannel.class, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (bc != null) {
            facade.remove(bc);
        }
    }

    private String displayName(BaseChannel bc) throws IllegalArgumentException {
        if (bc == null) {
            throw new IllegalArgumentException("Channel does not exist.");
        } else if (bc instanceof StaticChannel) {
            return ((StaticChannel) bc).getDisplayName();
        } else if (bc instanceof DynamicChannel) {
            return facade.getCurrentDisplayName((DynamicChannel) bc);
        } else {
            return bc.getName();
        }
    }

    @GET
    @Path("{channel}/restrict")
    @Produces({MediaType.APPLICATION_JSON})
    public JsonArray getStaticChannelRestrictedSignees(@PathParam("channel") String channel) {
        final StaticChannel sc = em.find(StaticChannel.class,
                channel, LockModeType.OPTIMISTIC);
        if (sc != null) {
            JsonBuilderFactory fac = Json.createBuilderFactory(null);
            JsonArrayBuilder builder = fac.createArrayBuilder();
            sc.getSignees().stream()
                    .map(SigneeAction::getSignee)
                    .forEach(sig -> {
                        JsonObjectBuilder ob = fac.createObjectBuilder();
                        ob.add("prefix", sig.getPrefix());
                        ob.add("suffix", sig.getSuffix());
                        ob.add("alias", sig.isAlias());
                        builder.add(ob);
                    });
            return builder.build();
        }
        return null;
    }

    @PUT
    @Path("{channel}/restrict")
    @Consumes({MediaType.APPLICATION_JSON})
    public void setStaticChannelRestrictedSignees(@PathParam("channel") String channel, JsonArray sig) {
        final StaticChannel sc = em.find(StaticChannel.class,
                channel, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (sc != null) {
            final Set<Signee> signees = sig.getValuesAs(JsonObject.class).stream()
                    .map(js -> new Signee(js.getString("prefix"), js.getString("suffix"), js.getBoolean("alias")))
                    .collect(Collectors.toSet());
            Set<SigneeAction> set = sc.getSignees();
            set.clear();
            signees.stream()
                    .map(s -> new SigneeAction(s, SigneeAction.Action.INCLUDE))
                    .forEach(set::add);
            em.merge(sc);
        }
    }

}
