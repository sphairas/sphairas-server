/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans.service;

import java.util.Date;
import java.util.Map;
import org.apache.naming.resources.ResourceAttributes;
import org.openide.util.lookup.ServiceProvider;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.services.util.BundleSupport;

/**
 *
 * @author boris.heithecker
 * @param <M>
 */
public abstract class AbstractServerBundleSupport<M extends Marker> extends BundleSupport<M> {

    protected final String resource;
    protected Date resourceLm;
    protected ResourceAttributes resAttr;

    protected AbstractServerBundleSupport(String resource) {
        super();
        this.resource = resource;
    }

    @Override
    protected synchronized void ensureLoaded() {
        if (resourceLm == null || getModified().after(resourceLm)) {
            reload();
        }
    }

    protected Date getModified() {
        return resAttr.getCreationOrLastModifiedDate();
    }

    @Override
    protected synchronized void markForReload() {
        resourceLm = null;
    }


    @ServiceProvider(service = BundleSupport.Factory.class, position = 500)
    public static class Factory extends BundleSupport.Factory {

        @Override
        protected BundleSupport create(String name, String provider, String resource, Map<String, String> arg) {
            if (resource.endsWith(".xml")) {
                return new ServerXmlDefinitionSupport(name, resource);
            } else {
                return new ServerBundleSupport(name, resource);
            }
        }
    }

}
