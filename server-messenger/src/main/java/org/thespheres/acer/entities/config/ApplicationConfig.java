/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.config;

import java.util.Set;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;

/**
 *
 * @author boris.heithecker
 */
@javax.ws.rs.ApplicationPath("resource")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
        addRestResourceClasses(resources);
        resources.add(MoxyJsonFeature.class);
        return resources;
    }

    /**
     * Do not modify addRestResourceClasses() method. It is automatically
     * populated with all resources defined in the project. If required, comment
     * out calling this method in getClasses().
     */
    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(org.thespheres.acer.entities.config.CORSFilter.class);
        resources.add(org.thespheres.acer.entities.config.JAXBJSONContextResolver.class);
        resources.add(org.thespheres.acer.entities.resource.AdminChannelsResource.class);
        resources.add(org.thespheres.acer.entities.resource.AdminMailResource.class);
        resources.add(org.thespheres.acer.entities.resource.AdminMessagesResource.class);
        resources.add(org.thespheres.acer.entities.resource.MessagesResource.class);
    }

}
