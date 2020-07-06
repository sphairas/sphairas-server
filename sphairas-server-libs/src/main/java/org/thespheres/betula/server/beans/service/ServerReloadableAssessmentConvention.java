/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import org.openide.util.lookup.ServiceProvider;
import org.thespheres.betula.server.beans.MissingConfigurationResourceException;
import org.thespheres.betula.server.beans.config.CommonAppProperties;
import org.thespheres.betula.services.util.AbstractReloadableAssessmentConvention;
import org.thespheres.betula.xmldefinitions.XmlAssessmentConventionDefintion;

/**
 *
 * @author boris.heithecker
 */
public class ServerReloadableAssessmentConvention extends AbstractReloadableAssessmentConvention {

    protected final String resource;
    private final AtomicReference<XmlAssessmentConventionDefintion> definition = new AtomicReference<>();
    protected Date resourceLastModified;
    protected ResourceAttributes resAttr;
    private static JAXBContext jaxb;

    ServerReloadableAssessmentConvention(final String provider, final String name, final String resource) {
        super(provider, name);
        this.resource = resource;
    }

    @Override
    protected XmlAssessmentConventionDefintion getDefinition() {
        ensureLoaded();
        return definition.get();
    }

    protected synchronized void ensureLoaded() {
        if (resourceLastModified == null || getModified().after(resourceLastModified)) {
            reload();
        }
    }

    protected Date getModified() {
        return resAttr.getCreationOrLastModifiedDate();
    }

    @Override
    protected synchronized void markForReload() {
        resourceLastModified = null;
    }

    protected void reload() {        
        final XmlAssessmentConventionDefintion result;
        try {
            result = fetchResourceBundle();
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }

        if (result != null) {
            synchronized (definition) {
                definition.set(result);
            }
            cSupport.fireChange();
        }
    }

    XmlAssessmentConventionDefintion fetchResourceBundle() throws IOException {
        final ProxyDirContext dc = CommonAppProperties.lookupAppResourcesContext();
        final Resource res;
        try {
            res = (Resource) dc.lookup(resource);
        } catch (final NamingException ex) {
            Logger.getLogger(ServerReloadableAssessmentConvention.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
            throw new MissingConfigurationResourceException(resource);
        }
        try {
            final ResourceAttributes attr = (ResourceAttributes) dc.getAttributes(resource);
            if (attr != null) {
                resAttr = attr;
                resourceLastModified = attr.getCreationOrLastModifiedDate();
            }
        } catch (final NamingException | ClassCastException ex) {
            resourceLastModified = null;
            Logger.getLogger(ServerReloadableAssessmentConvention.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
        }
        try (final InputStream is = res.streamContent()) {
            final BufferedInputStream bis = new BufferedInputStream(is); //? BufferedEntity?
            try {
                return (XmlAssessmentConventionDefintion) getJAXB().createUnmarshaller().unmarshal(bis);
            } catch (final JAXBException ex) {
                throw new IOException(ex);
            }
        }
    }

    private static JAXBContext getJAXB() {
        synchronized (ServerReloadableAssessmentConvention.class) {
            if (jaxb == null) {
                try {
                    jaxb = JAXBContext.newInstance(XmlAssessmentConventionDefintion.class);
                } catch (JAXBException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
        return jaxb;
    }

    @ServiceProvider(service = AbstractReloadableAssessmentConvention.Factory.class)
    public static class ServerReloadableAssessmentConventionFactory extends AbstractReloadableAssessmentConvention.Factory {

        @Override
        protected AbstractReloadableAssessmentConvention create(final String provider, final String name, final String resource, final Map<String, String> arg) throws IllegalStateException {
            return new ServerReloadableAssessmentConvention(provider, name, resource);
        }

    }

}
