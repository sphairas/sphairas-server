/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import java.lang.reflect.Type;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.json.JsonValue;
import javax.json.bind.annotation.JsonbTypeDeserializer;
import javax.json.bind.annotation.JsonbTypeSerializer;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

/**
 *
 * @author boris.heithecker
 */
public class Property {

    private String name;
    private String value;

    public Property() {
    }

    public Property(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    Property(final Map.Entry<String, JsonValue> e) {
        this(e.getKey(), e.getValue().toString());
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @JsonbTypeSerializer(value = PropertyListSerializer.class)
    @JsonbTypeDeserializer(value = PropertyListSerializer.class)
    public static class PropertyList extends AbstractList<Property> {

        private final List<Property> del = new ArrayList<>();

        @Override
        public Property get(final int index) {
            return del.get(index);
        }

        @Override
        public void add(final int index, final Property element) {
            del.add(index, element);
        }

        @Override
        public Property remove(final int index) {
            return del.remove(index);
        }

        @Override
        public Property set(final int index, final Property element) {
            return del.set(index, element);
        }

        @Override
        public int size() {
            return del.size();
        }

    }

    public static class PropertyListSerializer implements JsonbSerializer<PropertyList>, JsonbDeserializer<PropertyList> {

        @Override
        public void serialize(final PropertyList l, final JsonGenerator gen, final SerializationContext ctx) {
            gen.writeStartObject();
            l.forEach(p -> gen.write(p.getName(), p.getValue()));
            gen.writeEnd();
        }

        @Override
        public PropertyList deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
            final PropertyList ret = new PropertyList();
            parser.getObjectStream()
                    .map(e -> new Property(e))
                    .forEach(ret::add);
            return ret;
        }

    }
}
