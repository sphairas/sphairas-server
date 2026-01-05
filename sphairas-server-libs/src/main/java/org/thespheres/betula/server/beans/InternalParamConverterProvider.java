package org.thespheres.betula.server.beans;

import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.Marker;

/**
 *
 * @author boris
 */
@Provider
public class InternalParamConverterProvider implements ParamConverterProvider {

    @Context
    private Configuration configuration;

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (Marker.class.equals(rawType)) {
            return (ParamConverter<T>) getMarkerConverter();
        } else if (Grade.class.equals(rawType)) {
            return (ParamConverter<T>) getGradeConverter();
        }
        return null;
    }

    protected ParamConverter<Marker> getMarkerConverter() {
        final Map<String, Object> props = configuration.getProperties();
        return new MarkerConverter(props);
    }

    protected ParamConverter<Grade> getGradeConverter() {
        final Map<String, Object> props = configuration.getProperties();
        return new GradeConverter(props);
    }
}
