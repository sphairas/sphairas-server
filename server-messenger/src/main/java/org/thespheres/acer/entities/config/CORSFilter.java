/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.config;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author boris.heithecker
 */
@Provider
public class CORSFilter implements ContainerResponseFilter { // ContainerRequestFilter,
//
//    @Override
//    public void filter(ContainerRequestContext requestContext) {
//    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
        responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS, DELETE");
        responseContext.getHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type, Access-Control-Allow-Origin");
        //
        responseContext.getHeaders().add("Access-Control-Expose-Headers", "Date, Last-Modified");
//        X-Powered-By:	Servlet/3.1 JSP/2.3 (GlassFish Server Open Source Edition 4.1 Java/Oracle Corporation/1.8)
//Server:	GlassFish Server Open Source Edition 4.1
//Pragma:	No-cache
//Expires:	Thu, 01 Jan 1970 01:00:00 CET
//Date:	Tue, 01 Sep 2015 18:25:55 GMT
//Content-Type:	application/json
//Content-Length:	114
//Cache-Control:	no-cache
//Access-Control-Allow-Origin:	*
//access-control-allow-methods:	GET, POST, PUT, OPTIONS, DELETE
//access-control-allow-headers:	Authorization
    }

}
