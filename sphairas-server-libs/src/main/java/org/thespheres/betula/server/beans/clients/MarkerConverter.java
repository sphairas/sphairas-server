package org.thespheres.betula.server.beans.clients;

import jakarta.ws.rs.ext.ParamConverter;
import java.util.Collections;
import java.util.Map;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.MarkerFactory;

/**
 *
 * @author boris
 */
public class MarkerConverter implements ParamConverter<Marker> {

    private final boolean convertToAbstract;

    public MarkerConverter() {
        this(Collections.EMPTY_MAP);
    }

    public MarkerConverter(Map<String, Object> props) {
        this.convertToAbstract = (boolean) props.getOrDefault("convert.abstract", Boolean.FALSE);
    }

    @Override
    public Marker fromString(String value) {
        return convertToAbstract ? MarkerFactory.resolveAbstract(value) : MarkerFactory.resolve(value);
    }

    @Override
    public String toString(Marker value) {
        return value.toString();
    }

}
