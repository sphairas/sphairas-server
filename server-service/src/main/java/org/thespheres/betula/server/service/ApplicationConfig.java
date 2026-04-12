/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.service;

import java.util.Set;
import jakarta.ws.rs.core.Application;

/**
 *
 * @author boris.heithecker
 */
@jakarta.ws.rs.ApplicationPath("api")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
        addRestResourceClasses(resources);
        return resources;
    }

    /**
     * Do not modify addRestResourceClasses() method. It is automatically
     * populated with all resources defined in the project. If required, comment
     * out calling this method in getClasses().
     */
    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(org.thespheres.betula.server.beans.clients.AmbiguousDateExceptionMapper.class);
        resources.add(org.thespheres.betula.server.beans.clients.InternalParamConverterProvider.class);
        resources.add(org.thespheres.betula.server.service.DbServiceResource.class);
        resources.add(org.thespheres.betula.server.service.FastTargetDocumentsRestService.class);
        resources.add(org.thespheres.betula.server.service.InternalAPI.class);
        resources.add(org.thespheres.betula.server.service.UnitsServiceResource.class);
        resources.add(org.thespheres.betula.server.service.dav.AppResourcesResource.class);
    }

}
