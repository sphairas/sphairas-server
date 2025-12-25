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
//import javax.naming.NamingException;
//import jakarta.xml.bind.JAXBContext;
//import jakarta.xml.bind.JAXBException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
//import org.openide.util.Exceptions;
//import org.apache.naming.resources.ProxyDirContext;
//import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import org.openide.util.lookup.ServiceProvider;
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
    protected Attributes resAttr;
    private static javax.xml.bind.JAXBContext jaxb;

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
        return ((ResourceAttributes) resAttr).getCreationOrLastModifiedDate();
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
        final DirContext dc = CommonAppProperties.lookupAppResourcesContext();
//        final Resource res;
//        try {
//            res = (Resource) dc.lookup(resource);
//        } catch (final NamingException ex) {
//            Logger.getLogger(ServerReloadableAssessmentConvention.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
//            throw new MissingConfigurationResourceException(resource);
//        }
//        try {
//            final ResourceAttributes attr = (ResourceAttributes) dc.getAttributes(resource);
//            if (attr != null) {
//                resAttr = attr;
//                resourceLastModified = attr.getCreationOrLastModifiedDate();
//            }
//        } catch (final NamingException | ClassCastException ex) {
//            resourceLastModified = null;
//            Logger.getLogger(ServerReloadableAssessmentConvention.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
//        }
        try (final InputStream is = (InputStream) dc.lookup(resource)) { // res.streamContent()) {
            final BufferedInputStream bis = new BufferedInputStream(is); //? BufferedEntity?
            try {
                return (XmlAssessmentConventionDefintion) getJAXB().createUnmarshaller().unmarshal(bis);
            } catch (javax.xml.bind.JAXBException ex) {
                Logger.getLogger(ServerReloadableAssessmentConvention.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
                throw new IOException(ex);
            }
        } catch (NamingException ex) {
            Logger.getLogger(ServerReloadableAssessmentConvention.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
            throw new IOException(ex);
        }
    }

    private static javax.xml.bind.JAXBContext getJAXB() {
        synchronized (ServerReloadableAssessmentConvention.class) {
            if (jaxb == null) {
                try {
                    jaxb = javax.xml.bind.JAXBContext.newInstance(XmlAssessmentConventionDefintion.class);
                } catch (javax.xml.bind.JAXBException ex) {
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
