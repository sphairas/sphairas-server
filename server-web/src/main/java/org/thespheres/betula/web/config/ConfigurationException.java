/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.config;

import javax.ejb.ApplicationException;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author boris.heithecker
 */
@ApplicationException(rollback = true)
@Messages({"ConfigurationException.message.cause=Verursachter Konfigurationsfehler für den Resourcen-Typ {0}. Fehlender Eigenschaften-Schlüssel {1}."})
public class ConfigurationException extends RuntimeException {

    private final String resourceName;
    private final String property;

    public ConfigurationException(String resourceName, String propertyKey) {
        this.property = propertyKey;
        this.resourceName = resourceName;
    }

    @Override
    public String getMessage() {
        return NbBundle.getMessage(ConfigurationException.class, "ConfigurationException.message.cause", resourceName, property);
    }

}
