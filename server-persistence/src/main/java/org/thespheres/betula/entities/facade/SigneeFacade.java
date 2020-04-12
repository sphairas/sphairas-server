/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade;

import javax.ejb.Local;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.entities.SigneeEntity;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface SigneeFacade {

    public SigneeEntity getCurrent();
    
    public SigneeEntity find(Signee signee);
}
