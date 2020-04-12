/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.naming.NamingException;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import org.thespheres.betula.server.beans.MissingConfigurationResourceException;
import org.thespheres.betula.server.beans.config.CommonAppProperties;
import org.thespheres.betula.services.util.BundleMarker;

/**
 *
 * @author boris.heithecker
 */
class ServerBundleSupport extends AbstractServerBundleSupport<BundleMarker> {

    protected ResourceBundle bundle;
    private final String convention;

    @SuppressWarnings({"OverridableMethodCallInConstructor"})
    ServerBundleSupport(String name, String resource) {
        super(resource);
        this.convention = name;
    }

    @Override
    public String getConvention() {
        return convention;
    }

    PropertyResourceBundle fetchResourceBundle(String file) throws IOException {

        final ProxyDirContext dc = CommonAppProperties.lookupAppResourcesContext();
        final Resource res;
        try {
            res = (Resource) dc.lookup(file);
        } catch (NamingException ex) {
            Logger.getLogger(ServerBundleSupport.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
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
            Logger.getLogger(ServerBundleSupport.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
        }
        try (final InputStream is = res.streamContent()) {
            final BufferedInputStream bis = new BufferedInputStream(is); //? BufferedEntity?
            return new PropertyResourceBundle(bis);
        }

    }

    @Override
    protected synchronized void reload() {
        final boolean fireChange = elements != null;
        elements = new BundleMarker[0];
        try {
            bundle = fetchResourceBundle(resource);
            //Do not use bundle.keySet() --> ordering not guaranteed
            //Ordering of elements
            final Set<String> ids = Collections.list(bundle.getKeys()).stream()
                    .filter(key -> IDPATTERN.matcher(key).matches())
                    .collect(Collectors.toSet());
            final Set<String> retain = Arrays.stream(elements)
                    .map(BundleMarker::getId)
                    .filter(ids::contains)
                    .collect(Collectors.toSet());
            final List<BundleMarker> update = new ArrayList<>();
            for (BundleMarker old : elements) {
                if (retain.contains(old.getId())) {
                    old.setMessage(bundle.getString(old.getId()));
                    update.add(old);
                }
            }
            ids.stream()
                    .filter(id -> !retain.contains(id))
                    .map(id -> new BundleMarker(convention, id, bundle.getString(id)))
                    .forEach(update::add);
            final BundleMarker[] arr = update.stream().toArray(BundleMarker[]::new);
            this.elements = arr;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        if (fireChange) {
            cSupport.fireChange();
        }
    }

}
