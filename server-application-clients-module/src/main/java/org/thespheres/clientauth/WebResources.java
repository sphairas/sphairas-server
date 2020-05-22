///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package org.thespheres.clientauth;
//
//import javax.annotation.security.RolesAllowed;
//import javax.enterprise.context.RequestScoped;
//import javax.servlet.http.HttpServletRequest;
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.PathParam;
//import javax.ws.rs.core.Context;
//import javax.ws.rs.core.UriInfo;
//
///**
// *
// * @author boris
// */
//@RolesAllowed("signee")
//@RequestScoped//SessionScoped?
//public class WebResources {
//
//    @Context
//    UriInfo ctx;
//
//    @GET
//    @Path("/web/{path}")
//    public void getJson(@PathParam("path") String path, @Context final HttpServletRequest request) {
//        ctx.getQueryParameters();
//        request.getServletContext().getContext("web").getRequestDispatcher(path);
//    }
//}
