package org.thespheres.betula.server.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.CreationException;
import javax.xml.bind.JAXBException;
import org.thespheres.betula.database.DBAdminTask;
import org.thespheres.betula.database.DBAdminTaskResult;
import org.thespheres.betula.document.Container;

/**
 *
 * @author boris
 */
@ApplicationScoped
public class AppResources {

    private javax.xml.bind.JAXBContext context;

    @PostConstruct
    public void init() {
        try {
            context = javax.xml.bind.JAXBContext.newInstance(Container.class, DBAdminTask.class, DBAdminTaskResult.class);
        } catch (JAXBException ex) {
            throw new CreationException(ex);
        }
    }

    public javax.xml.bind.JAXBContext getJAXBContext() {
        return context;
    }

}
