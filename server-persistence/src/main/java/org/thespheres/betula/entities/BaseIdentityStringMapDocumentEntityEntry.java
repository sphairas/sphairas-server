/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class BaseIdentityStringMapDocumentEntityEntry implements Serializable {

    @Embedded
    private EmbeddableIdentity mapKey;
    @Column(name = "BASE_IDENTITY_STRING_MAP_VALUE")
    private String mapValue;

    public BaseIdentityStringMapDocumentEntityEntry() {
    }

    public BaseIdentityStringMapDocumentEntityEntry(EmbeddableIdentity key, String value) {
        this.mapKey = key;
        this.mapValue = value;
    }

    public String getString() {
        return mapValue;
    }

    public EmbeddableIdentity getKey() {
        return mapKey;
    }

}
