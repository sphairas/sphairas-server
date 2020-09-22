/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
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
@RolesAllowed({"signee"})
@Remote(BetulaWebService.class)
@EJB(beanName = "RemoteBetulaService", name = "java:global/Betula_Server/Betula_Persistence/RemoteBetulaService!org.thespheres.betula.services.ws.BetulaWebService", beanInterface = BetulaWebService.class)
@Stateless
public class RemoteBetulaService extends AbstractBetulaService implements BetulaWebService {

    @Override
    public Container fetch(final DocumentId ticket) {
        return super.fetch(ticket);
    }

    @Override
    public Container solicit(final Container container) throws UnauthorizedException, NotFoundException, SyntaxException {
        return super.solicit(container);
    }

}
