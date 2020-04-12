/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.rest;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author boris.heithecker
 */
@Priority(Priorities.AUTHENTICATION)
@PreMatching
@Provider
public class CORSFilter implements ContainerResponseFilter {

    public static final String CORS_ORIGIN = "http://jupiter:3000/";//"http://jupiter:3000/,http://127.0.1.1/3000/,http://localhost:3000/";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", CORS_ORIGIN);
        responseContext.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE"); //HEAD PUT, 
        responseContext.getHeaders().putSingle("Access-Control-Allow-Headers", "Authorization, Content-Type, Access-Control-Allow-Origin");
        responseContext.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
        //
        responseContext.getHeaders().putSingle("Access-Control-Expose-Headers", "Date, Last-Modified");
    }

}
