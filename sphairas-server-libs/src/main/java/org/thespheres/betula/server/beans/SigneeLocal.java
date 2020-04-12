/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import javax.ejb.Local;
import org.thespheres.betula.document.Signee;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface SigneeLocal {

    public Signee getSigneePrincipal(final boolean requireSigneeEntity);

    public String getSigneeCommonName(Signee signee);
}
