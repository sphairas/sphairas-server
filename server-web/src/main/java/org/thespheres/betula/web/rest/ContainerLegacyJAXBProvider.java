package org.thespheres.betula.web.rest;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.xml.bind.JAXBContext;
import org.thespheres.betula.document.Container;

/**
 *
 * @author boris
 */
@Provider
@Produces(MediaType.APPLICATION_XML)
@Consumes(MediaType.APPLICATION_XML)
public class ContainerLegacyJAXBProvider implements MessageBodyWriter<Container>, MessageBodyReader<Container> {

    private JAXBContext jaxb;

    @PostConstruct
    public void initialize() {
        try {
            jaxb = javax.xml.bind.JAXBContext.newInstance(Container.class);
        } catch (javax.xml.bind.JAXBException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return Container.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(Container container, Class< ?> type,
            Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream)
            throws IOException, WebApplicationException {
        try {
            javax.xml.bind.Marshaller m = jaxb.createMarshaller();
            m.marshal(container, entityStream);
        } catch (javax.xml.bind.JAXBException e) {
            throw new WebApplicationException("Legacy marshaling failed", e);
        }
    }

    @Override
    public boolean isReadable(Class<?> clz, Type type, Annotation[] antns, MediaType mt) {
        return Container.class.isAssignableFrom(clz);
    }

    @Override
    public Container readFrom(Class<Container> clz, Type type, Annotation[] antns, MediaType mt, MultivaluedMap<String, String> mm, InputStream in) throws IOException, WebApplicationException {
        try {
            javax.xml.bind.Unmarshaller u = jaxb.createUnmarshaller();
            return (Container) u.unmarshal(in);
        } catch (javax.xml.bind.JAXBException e) {
            throw new WebApplicationException("Legacy unmarshaling failed", e);
        }
    }

}
