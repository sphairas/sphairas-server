/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade;

import java.util.Set;
import javax.ejb.Local;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface DocumentsFacade {

    public Set<DocumentId> findAllExpired(long before);

    public Set<DocumentId> findAll();

}
