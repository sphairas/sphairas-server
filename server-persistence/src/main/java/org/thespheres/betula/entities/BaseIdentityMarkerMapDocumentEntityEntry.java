/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import org.thespheres.betula.document.Marker;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class BaseIdentityMarkerMapDocumentEntityEntry implements Serializable {

    @Embedded
    private EmbeddableIdentity mapKey;
    @Embedded
    private EmbeddableMarker mappedValue;

    public BaseIdentityMarkerMapDocumentEntityEntry() {
    }

    public BaseIdentityMarkerMapDocumentEntityEntry(EmbeddableIdentity key, Marker value) {
        this.mapKey = key;
        this.mappedValue = new EmbeddableMarker(value);
    }

    public Marker getMarker() {
        return mappedValue.getMarker();
    }

    public EmbeddableIdentity getKey() {
        return mapKey;
    }

}
