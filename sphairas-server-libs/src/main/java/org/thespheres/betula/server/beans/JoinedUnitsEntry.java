/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import org.thespheres.betula.UnitId;

/**
 *
 * @author boris.heithecker
 */
@Deprecated
public final class JoinedUnitsEntry implements Serializable {

    public static JoinedUnitsEntry EMPTY = new JoinedUnitsEntry(null, new UnitId[0]);
    private final UnitId joinUnit;
    private final UnitId[] joinedUnits;

    public JoinedUnitsEntry(UnitId joinUnit, UnitId[] joinedUnits) {
        this.joinUnit = joinUnit;
        this.joinedUnits = joinedUnits;
    }

    public UnitId getJoinUnit() {
        return joinUnit;
    }

    public UnitId[] getJoinedUnits() {
        return joinedUnits;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.joinUnit);
        hash = 83 * hash + Arrays.deepHashCode(this.joinedUnits);
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
        final JoinedUnitsEntry other = (JoinedUnitsEntry) obj;
        if (!Objects.equals(this.joinUnit, other.joinUnit)) {
            return false;
        }
        return Arrays.deepEquals(this.joinedUnits, other.joinedUnits);
    }

}
