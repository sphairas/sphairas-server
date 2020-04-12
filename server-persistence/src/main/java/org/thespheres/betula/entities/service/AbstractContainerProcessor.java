/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service;

import java.util.List;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.thespheres.betula.Identity;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.util.DocumentUtilities;
import org.thespheres.betula.server.beans.SigneeLocal;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.UnauthorizedException;

/**
 *
 * @author boris.heithecker
 */
public abstract class AbstractContainerProcessor implements ContainerProcessor {

    protected final String[][] paths;
    @EJB
    protected SigneeLocal login;
    @Resource
    protected SessionContext context;

    protected AbstractContainerProcessor(String[][] paths) {
        this.paths = paths;
    }

    @TransactionAttribute(value = TransactionAttributeType.MANDATORY)
    @Override
    public void process(Container container) throws UnauthorizedException, NotFoundException, SyntaxException {
        for (String[] p : paths) {
            List<Envelope> l = DocumentUtilities.findEnvelope(container, p);
            for (Envelope t : l) {
                process(container, p, t);
            }
        }
    }

//    @Deprecated
    protected abstract void process(Container container, String[] path, Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException;

    protected <I extends Identity> Entry<I, ?> toEntry(Envelope node, Class<I> idClass) throws SyntaxException {
        if (node instanceof Entry) {
            Entry entryNode = (Entry) node;
            if (idClass.isAssignableFrom(entryNode.getIdentity().getClass())) {
                return (Entry<I, ?>) node;
            }
        }
        throw ServiceUtils.createSyntaxException("");
    }

    protected <I extends Identity, V extends Object> Entry<I, V> toEntry(Envelope node, Class<I> idClass, Class<V> valueClass) throws SyntaxException {
        Entry<I, ?> entry = toEntry(node, idClass);
        if (entry.getValue() == null || valueClass.isAssignableFrom(entry.getValue().getClass())) {
            return (Entry<I, V>) entry;
        }
        throw ServiceUtils.createSyntaxException("");
    }

    protected <E extends Entry> E toEntryType(Envelope node, Class<E> entryType) throws SyntaxException {
        try {
            return entryType.cast(node);
        } catch (ClassCastException e) {
        }
        throw ServiceUtils.createSyntaxException("");
    }
}
