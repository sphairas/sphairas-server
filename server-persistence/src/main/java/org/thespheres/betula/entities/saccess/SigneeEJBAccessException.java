/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.saccess;

import java.security.Principal;
import javax.ejb.ApplicationException;
import javax.ejb.EJBAccessException;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.thespheres.betula.Identity;

/**
 *
 * @author boris.heithecker
 */
@Messages("SigneeEJBAccessException.noDocumentAccess.message=\"{0}\" has no permission to access {1}.")
@ApplicationException(rollback = true)
public class SigneeEJBAccessException extends EJBAccessException {

    protected final String document;
    protected final Principal principal;

    public SigneeEJBAccessException(Identity doc, Principal principal) {
        super();
        this.document = doc.toString();
        this.principal = principal;
    }

    public SigneeEJBAccessException(String resourceName, Principal principal) {
        super();
        this.document = resourceName;
        this.principal = principal;
    }

    SigneeEJBAccessException() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getMessage() {
        return NbBundle.getMessage(SigneeEJBAccessException.class, "SigneeEJBAccessException.noDocumentAccess.message", principal.getName(), document);
    }
}
