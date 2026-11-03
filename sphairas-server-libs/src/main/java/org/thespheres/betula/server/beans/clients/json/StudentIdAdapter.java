package org.thespheres.betula.server.beans.clients.json;

import jakarta.json.bind.adapter.JsonbAdapter;
import org.thespheres.betula.StudentId;

public class StudentIdAdapter implements JsonbAdapter<StudentId, String> {

    @Override
    public String adaptToJson(final StudentId student) {
        return student != null ? student.toString() : null;
    }

    @Override
    public StudentId adaptFromJson(final String value) {
        return value != null ? StudentId.valueOf(value) : null;
    }
}
