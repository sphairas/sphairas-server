/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade.impl;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.facade.SigneeFacade;
import org.thespheres.betula.entities.localbeans.AbstractSigneeFacade;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class SigneeFacadeImpl extends AbstractSigneeFacade implements SigneeFacade {

    @Override
    public SigneeEntity getCurrent() {
        if (!context.isCallerInRole("signee")) {
            return null;
        }
        return super.getCurrent();
    }

    @Override
    public SigneeEntity find(final Signee signee) {
        return em.find(SigneeEntity.class, signee);
    }
}
