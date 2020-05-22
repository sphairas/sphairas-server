/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import java.util.ArrayList;
import java.util.List;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.config.PropertyOrderStrategy;

/**
 *
 * @author boris.heithecker
 */
@JsonbPropertyOrder(PropertyOrderStrategy.ANY)
public class Row {

    @JsonbProperty("student")
    private final JSONStudent student;
    @JsonbProperty("grades")
    private final List<JSONGradeValue> grades = new ArrayList<>();

    public Row(@JsonbProperty("student") final JSONStudent student) {
        this.student = student;
    }

    public JSONStudent getStudent() {
        return student;
    }

    public List<JSONGradeValue> getGrades() {
        return grades;
    }

}
