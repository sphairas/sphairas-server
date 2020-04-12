/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import org.thespheres.betula.UnitId;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class SigneeUnitMapDocumentEntityEntry implements Serializable {

    @JoinColumns({
        @JoinColumn(name = "SIGNEEUNITMAP_SIGNEE_ID", referencedColumnName = "SIGNEE_ID"),
        @JoinColumn(name = "SIGNEEUNITMAP_SIGNEE_AUTHORITY", referencedColumnName = "SIGNEE_AUTHORITY"),
        @JoinColumn(name = "SIGNEEUNITMAP_SIGNEE_ALIAS", referencedColumnName = "SIGNEE_ALIAS")
    })
    private SigneeEntity mapKey;
    @Embedded
    private EmbeddableUnitId mappedValue;

    public SigneeUnitMapDocumentEntityEntry() {
    }

    public SigneeUnitMapDocumentEntityEntry(SigneeEntity key, UnitId value) {
        this.mapKey = key;
        this.mappedValue = new EmbeddableUnitId(value);
    }

    public UnitId getUnitId() {
        return mappedValue.getUnitId();
    }

    public SigneeEntity getKey() {
        return mapKey;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.mapKey);
        return 97 * hash + Objects.hashCode(this.mappedValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SigneeUnitMapDocumentEntityEntry other = (SigneeUnitMapDocumentEntityEntry) obj;
        if (!Objects.equals(this.mapKey, other.mapKey)) {
            return false;
        }
        return Objects.equals(this.mappedValue, other.mappedValue);
    }

}
