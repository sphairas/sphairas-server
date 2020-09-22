/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.services.ws.BetulaWebService;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.UnauthorizedException;

/**
 *
 * @author boris.heithecker
 */
//@SchemaValidation
@WebService(serviceName = "BetulaService", portName = "BetulaServicePort", targetNamespace = "http://web.service.betula.thespheres.org/")
@Stateless
@DeclareRoles({"signee"})
@RolesAllowed({"signee"})
public class BetulaService extends AbstractBetulaService implements BetulaWebService {

    public BetulaService() {
    }

    @WebMethod(operationName = "fetch")
    @WebResult(targetNamespace = "http://www.thespheres.org/xsd/betula/container.xsd")
    @Override
    public Container fetch(@WebParam(name = "ticket") DocumentId ticket) {
        return super.fetch(ticket);
    }

    @WebMethod(operationName = "solicit")
    @WebResult(targetNamespace = "http://www.thespheres.org/xsd/betula/container.xsd")
    @RolesAllowed({"signee"})
    @Override
    public Container solicit(@WebParam(name = "container", targetNamespace = "http://www.thespheres.org/xsd/betula/container.xsd") Container container) throws UnauthorizedException, NotFoundException, SyntaxException {
        return super.solicit(container);
    }

}
