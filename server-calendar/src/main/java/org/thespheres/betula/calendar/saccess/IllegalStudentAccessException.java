/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.saccess;

import java.security.Principal;
import javax.ejb.ApplicationException;
import javax.ejb.EJBAccessException;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.thespheres.betula.StudentId;

/**
 *
 * @author boris.heithecker
 */
@Messages("SigneeEJBAccessException.noDocumentAccess.message=\"{0}\" has no permission to access {1}.")
@ApplicationException(rollback = true)
public class IllegalStudentAccessException extends EJBAccessException {

    protected final StudentId document;
    protected final Principal principal;

    public IllegalStudentAccessException(StudentId stud, Principal principal) {
        super();
        this.document = stud;
        this.principal = principal;
    }

    @Override
    public String getMessage() {
        return NbBundle.getMessage(IllegalStudentAccessException.class, "SigneeEJBAccessException.noDocumentAccess.message", principal.getName(), document);
    }
}
