package org.thespheres.betula.calendar.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("resource")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> resources = new HashSet<>();
        addRestResourceClasses(resources);
        return resources;
    }

    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(org.thespheres.betula.calendar.rest.ReportDatesResource.class);
        resources.add(org.thespheres.betula.calendar.rest.StudentsResource.class);
        resources.add(org.thespheres.betula.calendar.rest.TicketsResource.class);
        resources.add(org.thespheres.betula.server.beans.clients.AmbiguousDateExceptionMapper.class);
        resources.add(org.thespheres.betula.server.beans.clients.InternalParamConverterProvider.class);
    }
}