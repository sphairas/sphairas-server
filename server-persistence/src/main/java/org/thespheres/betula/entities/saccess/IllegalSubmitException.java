/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.saccess;

import java.security.Principal;
import javax.ejb.ApplicationException;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
@Messages("IllegalSubmitException.message=\"{0}\" has no permission to submit {2} to {1}.")
@ApplicationException(rollback = true)
public class IllegalSubmitException extends SigneeEJBAccessException {

    protected final Grade illegalSubmit;

    public IllegalSubmitException(DocumentId doc, Principal principal, Grade prohibited) {
        super(doc, principal);
        illegalSubmit = prohibited;
    }

    @Override
    public String getMessage() {
        String dv = illegalSubmit == null ? "null" : "{" + illegalSubmit.getConvention() + "}" + illegalSubmit.getId();
        return NbBundle.getMessage(SigneeEJBAccessException.class, "IllegalSubmitException.message", principal.getName(), document, dv);
    }

}
