/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.lookup.ServiceProvider;
import org.thespheres.betula.services.LocalFileProperties;
import org.thespheres.betula.services.LocalProperties;
import org.thespheres.betula.services.ServiceConstants;

/**
 *
 * @author boris.heithecker
 */
@ServiceProvider(service = LocalProperties.Provider.class)
public class AppPropertiesProviderImpl implements LocalFileProperties.Provider {

    static final String DEFAULT_PROPERTIES = "default.properties";
    static final DefaultPropertiesImpl[] INSTANCE = new DefaultPropertiesImpl[]{null};

    @Override
    public LocalFileProperties find(final String name) {
        if (CommonAppProperties.provider().equals(name)) {
            synchronized (INSTANCE) {
                if (INSTANCE[0] == null) {
                    try {
                        final Path base = ServiceConstants.configBase();
                        final DefaultPropertiesImpl ret = new DefaultPropertiesImpl(name, base, DEFAULT_PROPERTIES);
                        ret.init();
                        INSTANCE[0] = ret;
                    } catch (IOException ex) {
                        Logger.getLogger(AppPropertiesProviderImpl.class.getCanonicalName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
                    }
                }
            }
            return INSTANCE[0];
        }
        return null;
    }
}
