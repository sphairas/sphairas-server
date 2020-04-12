/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.dbadmin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.Resource;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.server.beans.config.CommonAppProperties;

/**
 *
 * @author boris.heithecker
 */
@ApplicationScoped
public class ContainerBean {

    private JAXBContext containerJAXB;

    @PostConstruct
    public void initialize() {
        try {
            containerJAXB = JAXBContext.newInstance(Container.class);
        } catch (JAXBException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void writeBackupFile(final Container container, final String file) throws IOException {
        final ProxyDirContext dc = CommonAppProperties.lookupAppResourcesContext();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            final Marshaller m = containerJAXB.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            m.marshal(container, baos);
        } catch (JAXBException ex) {
            throw new IOException(ex);
        }
        try {
            final Resource res = new Resource(baos.toByteArray());
            dc.rebind(file, res);
        } catch (NamingException ex) {
            throw new IOException(ex);
        }
    }

}
