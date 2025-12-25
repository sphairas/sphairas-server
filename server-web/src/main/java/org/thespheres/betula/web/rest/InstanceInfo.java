/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.MediaType;

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
