/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.local;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import org.thespheres.acer.entities.facade.ChannelFacade;
import org.thespheres.acer.entities.BaseChannel;
import org.thespheres.acer.entities.DynamicChannel;
import org.thespheres.acer.entities.StaticChannel;
import org.thespheres.betula.Identity;
import org.thespheres.betula.server.beans.annot.Current;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.scheme.spi.Term;

/**
 *
 * @author boris.heithecker
 */
@Singleton
@LocalBean
public class Naming {

    @EJB
    private ChannelFacade facade;
    @Default
    @Inject
    private NamingResolver namingResolver;
    @Current
    @Inject
    private Term currentTerm;

    @Lock(LockType.READ)
    public String resolveDisplayName(final Identity id, final String namingProvider, final String termScheduleProvider) {
//        Term t;
        try {
            return namingResolver.resolveDisplayNameResult(id).getResolvedName(currentTerm);
        } catch (IllegalAuthorityException ex) {
            return id.getId().toString();
        }
    }

    @Lock(LockType.READ)
    public String getChannelDisplayName(String name) {
        BaseChannel bc = facade.find(name, BaseChannel.class, LockModeType.OPTIMISTIC);
        if (bc instanceof DynamicChannel) {
            return facade.getCurrentDisplayName((DynamicChannel) bc);
        } else if (bc instanceof StaticChannel) {
            return ((StaticChannel) bc).getDisplayName();
        }
        return bc.getName();
    }
}
