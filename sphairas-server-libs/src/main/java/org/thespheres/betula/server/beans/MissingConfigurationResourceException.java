/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import javax.ejb.ApplicationException;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author boris.heithecker
 */
@ApplicationException(rollback = true)
@Messages({"MissingConfigurationResourceException.message.cause=Die Resource {0} wird vermisst."})
public class MissingConfigurationResourceException extends RuntimeException {

    private final String resourceName;

    public MissingConfigurationResourceException(String resource) {
        super();
        this.resourceName = resource;
    }

    @Override
    public String getMessage() {
        return NbBundle.getMessage(MissingConfigurationResourceException.class, "MissingConfigurationResourceException.message.cause", resourceName);
    }

}
