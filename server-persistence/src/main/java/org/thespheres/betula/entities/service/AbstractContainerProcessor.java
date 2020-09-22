/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.server.beans.SigneeLocal;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.UnauthorizedException;

/**
 *
 * @author boris.heithecker
 */
public abstract class AbstractContainerProcessor {

    protected final String[][] paths;
    @EJB
    protected SigneeLocal login;
    @Resource
    protected SessionContext context;

    protected AbstractContainerProcessor(String[][] paths) {
        this.paths = paths;
    }

    public String[][] getPaths() {
        return paths;
    }

    protected abstract void process(String[] path, Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException;

}
