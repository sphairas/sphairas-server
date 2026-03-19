package org.thespheres.betula.server.beans.clients;

import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
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

    private Map<String, Object> properties() {
        return Optional.ofNullable(configuration)
                .map(Configuration::getProperties)
                .orElse(Collections.EMPTY_MAP);
    }

    protected ParamConverter<Marker> getMarkerConverter() {
        return new MarkerConverter(properties());
    }

    protected ParamConverter<Grade> getGradeConverter() {
        return new GradeConverter(properties());
    }
}
