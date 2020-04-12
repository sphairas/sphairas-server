/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.config;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import org.thespheres.betula.server.beans.config.CommonAppProperties;
import org.thespheres.betula.services.LocalFileProperties;
import org.thespheres.betula.services.LocalProperties;
import org.thespheres.betula.services.NoProviderException;

/**
 *
 * @author boris.heithecker
 */
@Dependent
public class LocalConfigProperties implements LocalProperties {

    private String parentProvider;
    private LocalFileProperties parentProperties;
    @Inject
    private Properties instanceProperties;

    @Override
    public String getName() {
        return LocalConfigProperties.class.getSimpleName();
    }

    @PostConstruct
    void initialize() {
        final String sp = instanceProperties.getProperty("providerURL", CommonAppProperties.provider());
        parentProvider = instanceProperties.getProperty("super.providerURL", System.getProperty("super.providerURL", sp));
    }

    @Override
    public String getProperty(String name) {
        String value = instanceProperties.getProperty(name);
        if (value == null) {
            final LocalProperties par = parentProperties();
            if (par != null) {
                value = par.getProperty(name);
            }
        }
        if (value == null) {
            value = System.getProperty(name);
        }
        return value;
    }

    private LocalProperties parentProperties() {
        if (parentProvider != null && parentProperties == null) {
            try {
                parentProperties = LocalFileProperties.find(parentProvider);
            } catch (NoProviderException e) {
                throw new ConfiguredModelException(LocalFileProperties.class.getName(), e);
            }
        }
        return parentProperties;
    }

    @Override
    public Map<String, String> getProperties() {
        final Map<String, String> ret = new HashMap<>();
//        loadIfModified();
        final Enumeration<?> es = System.getProperties().propertyNames();
        while (es.hasMoreElements()) {
            String name = (String) es.nextElement();
            ret.put(name, System.getProperty(name));
        }
        final LocalProperties parent = parentProperties();
        if (parent != null) {
            parent.getProperties()
                    .forEach((name, value) -> ret.put(name, value));
        }
        final Enumeration<?> e = instanceProperties.propertyNames();
        while (e.hasMoreElements()) {
            final String name = (String) e.nextElement();
            ret.put(name, instanceProperties.getProperty(name));
        }
        return ret;
    }
}
