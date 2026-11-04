package org.thespheres.betula.web.config;

import java.io.IOException;
import java.io.InputStream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.thespheres.betula.server.beans.config.AppResourceProvider;

/**
 * Reads app resources from the service WAR via REST instead of JNDI.
 */
@ApplicationScoped
public class WebAppResourceProvider implements AppResourceProvider {

    @Inject
    private AppConfiguration appConfiguration;

    @Override
    public InputStream open(final String file) throws IOException {
        return appConfiguration.openAppResource(file);
    }
}
