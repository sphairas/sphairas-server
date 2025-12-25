package org.thespheres.clientauth;

import org.thespheres.server.clients.RemoteServiceClient;
import java.util.Set;
import jakarta.annotation.security.DeclareRoles;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.thespheres.betula.services.ws.BetulaWebService;

/**
 * Configures JAX-RS for the application.
 *
 * @author Juneau
 */
@DeclareRoles("signee")
@ApplicationPath("")
public class Configuration extends Application {

    @Inject
    private RemoteServiceClient service;

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> resources = new java.util.HashSet<>();
        addRestResourceClasses(resources);
        return resources;
    }


    @Produces
    public BetulaWebService createService() {
        try {
            final Object lookup = new InitialContext().lookup("java:global/Betula_Server/Betula_Persistence/RemoteBetulaService!org.thespheres.betula.services.ws.BetulaWebService");
//            lookup = service.getBetulaServicePort("http://localhost:8080/service/betulaws"); //no caller propagation
            return (BetulaWebService) lookup;
        } catch (NamingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(org.thespheres.clientauth.CORSFilter.class);
        resources.add(org.thespheres.server.clients.JavaEE8Resource.class);
        resources.add(org.thespheres.server.clients.ServiceClient.class);
        resources.add(org.thespheres.server.clients.SettingsResource.class);
        resources.add(org.thespheres.server.clients.TargetsResource.class);
        resources.add(org.thespheres.server.clients.UnitsResource.class);
        resources.add(org.thespheres.server.clients.config.JSONConfigurator.class);
    }
}
