/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.config.PropertyOrderStrategy;

/**
 *
 * @author boris.heithecker
 */
@JsonbPropertyOrder(PropertyOrderStrategy.ANY)
public class Column {

    @JsonbProperty("name")
    private String name;
    @JsonbProperty("label")
    private String label;
    @JsonbProperty("term")
    private JSONTerm term;
    @JsonbProperty("document")
    private JSONDocument document;

    public Column() {
    }

    public Column(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public JSONTerm getTerm() {
        return term;
    }

    public void setTerm(JSONTerm term) {
        this.term = term;
    }

    public JSONDocument getDocument() {
        return document;
    }

    public void setDocument(JSONDocument document) {
        this.document = document;
    }

}
