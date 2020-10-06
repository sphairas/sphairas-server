/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients;

import java.io.IOException;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.thespheres.betula.assess.AssessmentConvention;
import org.thespheres.betula.assess.GradeFactory;
import org.thespheres.betula.document.util.Identities;
import org.thespheres.server.clients.model.Convention;
import org.thespheres.server.clients.model.Icon;

/**
 *
 * @author boris.heithecker@gmx.net
 */
@Path("settings")
@RolesAllowed("signee")
@RequestScoped
public class SettingsResource {

    @GET
    @Path("/assess/{convention}")
    @Produces(MediaType.APPLICATION_JSON)
    public Convention getConventions(@PathParam("convention") final String convention, @HeaderParam("If-Modified-Since") String userAgent, @Context final HttpServletResponse resp) throws IOException {
        if (StringUtils.isBlank(convention)) {
            return null;
        }
        if (convention.length() > Identities.CONVENTION_MAX_LENGTH || !Identities.CONVENTIONPATTERN.matcher(convention).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        final AssessmentConvention cnv = GradeFactory.findConvention(convention);
        if (cnv == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        final Convention ret = new Convention(cnv);
        //TODO: add icons
        return ret;
    }

    @GET
    @Path("/svg-icons")
    @Produces(MediaType.APPLICATION_JSON)
    public Icon[] getIcons(@HeaderParam("If-Modified-Since") String userAgent, @Context final HttpServletResponse resp) throws IOException {
        return null;
    }

    @GET
    @Path("/svg-icons/{icon}")
    @Produces(MediaType.APPLICATION_JSON)
    public Icon getIcon(@PathParam("icon") final String icon, @HeaderParam("If-Modified-Since") String userAgent, @Context final HttpServletResponse resp) throws IOException {
        return null;
    }
}
