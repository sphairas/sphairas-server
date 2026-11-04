package org.thespheres.betula.server.beans.config;

import java.io.IOException;
import java.io.InputStream;

/**
 * Optional CDI bean that supplies application resources where the JNDI
 * AppResourcesContext is not available (e.g. the web WAR, which reads them
 * through the service REST API).
 */
public interface AppResourceProvider {

    /**
     * @return the content, or null if the resource does not exist
     */
    public InputStream open(String file) throws IOException;
}
