/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.config;

import java.util.Arrays;
import java.util.stream.Collectors;
import javax.ejb.ApplicationException;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author boris.heithecker
 */
@Deprecated //User server-libs
@ApplicationException(rollback = true)
@Messages({"ConfigurationException.message=Die Resource {0} wird vermisst.",
    "ConfigurationException.message.properties=Die Eigenschaft(en) {1} der Resource {0} wird/werden vermisst."})
public class ConfigurationException extends IllegalStateException {

    private final String resourceName;
    private final String[] properties;

    public ConfigurationException(final String resource) {
        this(resource, null, null);
    }

    public ConfigurationException(final String resource, final String[] properties) {
        this(resource, properties, null);
    }

    public ConfigurationException(final String resource, final Exception cause) {
        this(resource, null, cause);
    }

    public ConfigurationException(final String resource, final String[] properties, final Exception cause) {
        super();
        this.resourceName = resource;
        this.properties = properties;
        if (cause != null) {
            initCause(cause);
        }
    }

    @Override
    public String getMessage() {
        if (properties == null) {
            return NbBundle.getMessage(ConfigurationException.class, "ConfigurationException.message", resourceName);
        } else {
            final String props = Arrays.stream(properties).collect(Collectors.joining(", "));
            return NbBundle.getMessage(ConfigurationException.class, "ConfigurationException.message.properties", resourceName, props);
        }
    }

}
