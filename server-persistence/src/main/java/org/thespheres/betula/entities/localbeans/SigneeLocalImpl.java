/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.localbeans;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.server.beans.SigneeLocal;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class SigneeLocalImpl extends AbstractSigneeFacade implements SigneeLocal {

    @Override
    public Signee getSigneePrincipal(final boolean requireSigneeEntity) {
        //We need to override this because otherwise the method will no be decorated.
        return super.getSigneePrincipal(requireSigneeEntity); 
    }

}
