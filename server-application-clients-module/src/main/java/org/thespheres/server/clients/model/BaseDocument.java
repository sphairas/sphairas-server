/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.config.PropertyOrderStrategy;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.server.clients.ClientConfiguration;

/**
 *
 * @author boris.heithecker@gmx.net
 */
@JsonbPropertyOrder(PropertyOrderStrategy.ANY)
public abstract class BaseDocument {

    protected final String document;
    private DocumentId identity;
    private String name;
    protected ClientConfiguration config;

    protected BaseDocument(final String id) {
        this.document = id;
    }

    protected BaseDocument(final DocumentId id, final ClientConfiguration config) {
        this(config.toString(id));
        this.config = config;
        this.identity = id;
    }

    @JsonbProperty(value = "document")
    public String getDocument() {
        return document;
    }

    @JsonbTransient
    public DocumentId getIdentity() {
        return identity;
    }

    @JsonbProperty("name")
    public String getName() {
        return name;
    }

    @JsonbProperty("name")
    public void setName(final String name) {
        this.name = name;
    }

    @JsonbProperty(value = "configuration")
    public ClientConfiguration getConfiguration() {
        return config;
    }

}
