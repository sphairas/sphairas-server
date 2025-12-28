/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
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
@Stateless
@DeclareRoles({"signee"})
@RolesAllowed({"signee"})
public class BetulaService extends AbstractBetulaService implements BetulaWebService {

    public BetulaService() {
    }

    @Override
    public Container fetch(DocumentId ticket) {
        return super.fetch(ticket);
    }

    @RolesAllowed({"signee"})
    @Override
    public Container solicit(Container container) throws UnauthorizedException, NotFoundException, SyntaxException {
        return super.solicit(container);
    }

}
