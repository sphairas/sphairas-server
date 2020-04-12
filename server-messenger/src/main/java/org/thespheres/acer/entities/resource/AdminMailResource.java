/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.resource;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.thespheres.acer.MessageId;
import org.thespheres.acer.entities.facade.MailMessageFacade;
import org.thespheres.acer.entities.messages.tracking.MailStatusTracker;

/**
 *
 * @author boris.heithecker
 */
@Stateless
@Path("mail")
public class AdminMailResource {

    @EJB
    private MailMessageFacade mail;

    @PUT
    @Consumes({MediaType.TEXT_PLAIN})
    @Produces({MediaType.TEXT_PLAIN})
    public String enqueueEmail(@PathParam("message-authority") String authority, @PathParam("message-id") long id, @QueryParam("version") String v) {
        final MessageId mid = RestUtilities.messageId(authority, id, v);
        try {
            final MailStatusTracker ret = mail.sendMessageAsEmail(mid);
            return Long.toString(ret.getId());
        } catch (Exception e) {
            return Long.toString(-1l);
        }
    }

    @GET
    @Consumes({MediaType.TEXT_PLAIN})
    @Produces({MediaType.TEXT_PLAIN})
    public String getStatus(long mailStatusTrackerId) {
        MailStatusTracker mst = mail.getTracker(mailStatusTrackerId);
        return mst.getStatus();
    }

}
