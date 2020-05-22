/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
public class RemoteBetulaService implements BetulaWebService {

    @PersistenceContext(unitName = "betula0")
    private EntityManager em;
    @Inject
    private UnitsProcessor unitsProcessor;
    @Inject
    private TargetsProcessor targetsProcessor;

    @Override
    public Container fetch(final DocumentId ticket) {
        return new Container();
    }

    @Override
    public Container solicit(final Container container) throws UnauthorizedException, NotFoundException, SyntaxException {
        unitsProcessor.process(container);
        targetsProcessor.process(container);
        return container;
    }

}
