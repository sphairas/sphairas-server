/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.config;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import org.apache.naming.resources.FileDirContext;

/**
 *
 * @author boris.heithecker
 */
public class AppFileDirContext extends FileDirContext {

    @Override
    public void rebind(File file, Object obj, Attributes attrs) throws NamingException {
        super.rebind(file, obj, attrs);
        final Path relative = base.toPath().relativize(file.toPath());
        lookupAppResourcesNotificatorBean().notityConsumers(relative.toString());
    }

    @Override
    public void unbind(String name) throws NamingException {
        super.unbind(name);
        lookupAppResourcesNotificatorBean().notityConsumers(name);
    }

    private AppResourcesNotificator lookupAppResourcesNotificatorBean() {
        try {
            final Context c = new InitialContext();
            return (AppResourcesNotificator) c.lookup("java:global/Betula_Server/Betula_Persistence/AppResourcesNotificator!org.thespheres.betula.entities.config.AppResourcesNotificator");
        } catch (NamingException ne) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
            throw new RuntimeException(ne);
        }
    }

}
