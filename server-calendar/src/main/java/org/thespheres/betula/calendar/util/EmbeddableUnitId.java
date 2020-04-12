/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.util;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.thespheres.betula.UnitId;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddableUnitId implements Serializable {

    @Column(name = "UNIT_AUTHORITY", length = 64)
    private String unitAuthority;
    @Column(name = "UNIT_ID", length = 64)
    private String unitId;

    public EmbeddableUnitId() {
    }

    public EmbeddableUnitId(UnitId id) {
        this.unitAuthority = id.getAuthority();
        this.unitId = id.getId();
    }

    public UnitId getUnitId() {
        return new UnitId(unitAuthority, unitId);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.unitAuthority);
        hash = 47 * hash + Objects.hashCode(this.unitId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EmbeddableUnitId other = (EmbeddableUnitId) obj;
        if (!Objects.equals(this.unitAuthority, other.unitAuthority)) {
            return false;
        }
        return Objects.equals(this.unitId, other.unitId);
    }

}
