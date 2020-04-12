/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.saccess;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.ejb.SessionContext;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.server.beans.SigneeLocal;

/**
 *
 * @author boris.heithecker
 */
@Decorator
public abstract class SigneeFacadeSigneeDecorator implements SigneeLocal {

    @Inject
    @Delegate
    private SigneeLocal delegate;

    private SessionContext getDecoratedSessionContext() {
        InitialContext ic;
        try {
            ic = new InitialContext();
            return (SessionContext) ic.lookup("java:comp/EJBContext");
        } catch (NamingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Signee getSigneePrincipal(final boolean requireSigneeEntity) {
        final SessionContext context = getDecoratedSessionContext();
//        if (context.isCallerInRole("unitadmin") && SystemProperties.AUTHENTICATE_UNKNOWN_TRUSTED_X500PRINCIPALS) {
//        com.sun.enterprise.security.SecurityContext sc = com.sun.enterprise.security.SecurityContext.getCurrent();
//        if (sc != null) {
//            for (Principal p : sc.getSubject().getPrincipals()) {
//                String n = p.getName();
//                p.getName();
//            }
//            for (Object o : sc.getSubject().getPublicCredentials()) {
//                String s = o.toString();
//            }
//        }

        if (!context.isCallerInRole("signee") && context.isCallerInRole("unitadmin")) {
            return null;
        }
        return delegate.getSigneePrincipal(requireSigneeEntity);
    }

}
