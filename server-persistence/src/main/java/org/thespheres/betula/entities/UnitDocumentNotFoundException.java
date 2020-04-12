/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import javax.persistence.EntityNotFoundException;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.thespheres.betula.UnitId;

/**
 *
 * @author boris.heithecker
 */
@Messages("MessageNotFoundException.message=UnitDocumentEntity for unit {0} (Authority: {1}) not found in database.")
public class UnitDocumentNotFoundException extends EntityNotFoundException {

    private final UnitId unit;

    public UnitDocumentNotFoundException(UnitId message) {
        this.unit = message;
    }

    @Override
    public String getMessage() {
        return NbBundle.getMessage(UnitDocumentNotFoundException.class, "MessageNotFoundException.message", unit != null ? unit.getId() : null, unit != null ? unit.getAuthority() : null);
    }

}
