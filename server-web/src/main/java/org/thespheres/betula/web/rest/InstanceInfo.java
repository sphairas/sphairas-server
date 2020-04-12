/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.rest;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author boris.heithecker
 */
//Try RolesPermitted
//https://github.com/payara/Payara/issues/2841
//https://github.com/payara/Payara/issues/2490
//@RolesAllowed({"signee", "unitadmin"})
@Path("/provider")
@Stateless
public class InstanceInfo {

//    @Context
//    private UriInfo context;

    @GET
    @Path("/name")
    @Produces(MediaType.TEXT_PLAIN)
    public String getText() {
        return System.getenv("SPHAIRAS_PROVIDER");
    }

}
