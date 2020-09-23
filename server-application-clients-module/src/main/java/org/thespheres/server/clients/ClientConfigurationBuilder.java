/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients;

import java.util.Properties;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import org.thespheres.betula.services.LocalProperties;

/**
 *
 * @author boris.heithecker@gmx.net
 */
@ApplicationScoped
public class ClientConfigurationBuilder {
    
    @Inject
    private LocalProperties properties;
    @Named("common-import")
    @Inject
    private Properties importProperties;
    
    @Produces
    @Dependent
    public ClientConfiguration buildClientConfiguration() {
        final String authority = properties.getProperty("authority");
        final String studentsAuthority = importProperties.getProperty("students.authority");
        final String termAuthority = properties.getProperty("termSchedule.providerURL");
        final ClientConfiguration ret = new ClientConfiguration(authority, studentsAuthority, termAuthority);
        ret.setGradeConvention("de.notensystem");
        return ret;
    }
}
