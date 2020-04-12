/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import org.thespheres.betula.server.beans.MissingConfigurationResourceException;
import org.thespheres.betula.server.beans.config.CommonAppProperties;
import org.thespheres.betula.xmldefinitions.XmlMarkerConventionDefinition;
import org.thespheres.betula.xmldefinitions.XmlMarkerDefinition;

/**
 *
 * @author boris.heithecker
 */
class ServerXmlDefinitionSupport extends AbstractServerBundleSupport<XmlMarkerDefinition> {

    private static JAXBContext jaxb;
    protected XmlMarkerConventionDefinition definition;
    private final String fastName;

    @SuppressWarnings({"OverridableMethodCallInConstructor"})
    ServerXmlDefinitionSupport(String name, String resource) {
        super(resource);
        this.fastName = name;
    }

    @Override
    protected String getConvention() {
        if (fastName != null) {
            return fastName;
        }
        ensureLoaded();
        return definition.getName();
    }

    @Override
    protected String getDisplayName() {
        ensureLoaded();
        return definition.getDisplayName();
    }

    private static JAXBContext getJAXB() {
        synchronized (ServerXmlDefinitionSupport.class) {
            if (jaxb == null) {
                try {
                    jaxb = JAXBContext.newInstance(XmlMarkerConventionDefinition.class);
                } catch (JAXBException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
        return jaxb;
    }

    XmlMarkerConventionDefinition fetchResourceBundle(String file) throws IOException {

        final ProxyDirContext dc = CommonAppProperties.lookupAppResourcesContext();
        final Resource res;
        try {
            res = (Resource) dc.lookup(file);
        } catch (NamingException ex) {
            Logger.getLogger(ServerXmlDefinitionSupport.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
            throw new MissingConfigurationResourceException(file);
        }
        try {
            final ResourceAttributes attr = (ResourceAttributes) dc.getAttributes(file);
            if (attr != null) {
                resAttr = attr;
                resourceLm = attr.getCreationOrLastModifiedDate();
            }
        } catch (NamingException | ClassCastException ex) {
            resourceLm = null;
            Logger.getLogger(ServerXmlDefinitionSupport.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
        }
        try (final InputStream is = res.streamContent()) {
            final BufferedInputStream bis = new BufferedInputStream(is); //? BufferedEntity?
            try {
                return (XmlMarkerConventionDefinition) getJAXB().createUnmarshaller().unmarshal(bis);
            } catch (JAXBException ex) {
                throw new IOException(ex);
            }
        }

    }

    @Override
    protected synchronized void reload() {
        final boolean fireChange = elements != null;
        elements = new XmlMarkerDefinition[0];
        try {
            definition = fetchResourceBundle(resource);
            final XmlMarkerDefinition[] arr = definition.getMarkerSubsets().stream()
                    .flatMap(set -> set.getMarkerDefinitions().stream())
                    .toArray(XmlMarkerDefinition[]::new);
            elements = arr;
            Logger.getLogger("DEBUG").log(Level.INFO, "Reloaded: {0}", resource);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        if (fireChange && elements != null) {
            cSupport.fireChange();
        }
    }

}
