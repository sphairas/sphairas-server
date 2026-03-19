package org.thespheres.betula.server.beans.clients;

import jakarta.ws.rs.ext.ParamConverter;
import java.util.Collections;
import java.util.Map;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeFactory;

/**
 *
 * @author boris
 */
public class GradeConverter implements ParamConverter<Grade> {

    private final boolean convertToAbstract;

    public GradeConverter() {
        this(Collections.EMPTY_MAP);
    }

    public GradeConverter(Map<String, Object> props) {
        this.convertToAbstract = (boolean) props.getOrDefault("convert.abstract", Boolean.FALSE);
    }

    @Override
    public Grade fromString(String value) {
        return convertToAbstract ? GradeFactory.resolveAbstract(value) : GradeFactory.resolve(value);
    }

    @Override
    public String toString(Grade value) {
        return value.toString();
    }

}
