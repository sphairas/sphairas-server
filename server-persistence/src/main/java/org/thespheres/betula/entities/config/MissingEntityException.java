/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.config;

import javax.ejb.ApplicationException;
import javax.ejb.EJBAccessException;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.thespheres.betula.Identity;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Signee;

/**
 *
 * @author boris.heithecker
 */
@Messages("MissingEntityException.message=Cannot find {0}.")
@ApplicationException(rollback = true)
public class MissingEntityException extends EJBAccessException {

    protected final Identity entity;

    public MissingEntityException(DocumentId doc) {
        super();
        this.entity = doc;
    }

    public MissingEntityException(Signee signee) {
        super();
        this.entity = signee;
    }

    @Override
    public String getMessage() {
        return NbBundle.getMessage(MissingEntityException.class, "MissingEntityException.message", entity.toString());
    }
}
