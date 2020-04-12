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
import javax.validation.constraints.Size;
import org.thespheres.betula.services.scheme.spi.PeriodId;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddablePeriodId implements Serializable {

    @Column(name = "PERIOD_ID")
    private int periodId;
    @Column(name = "PERIOD_AUTHORITY", length = 64)
    private String periodAuthority;
    @Column(name = "PERIOD_VERSION", length = 32)
    @Size(max = 32)
    private String version;

    public EmbeddablePeriodId() {
    }

    public EmbeddablePeriodId(PeriodId record) {
        this.periodId = record.getId();
        this.periodAuthority = record.getAuthority();
        this.version = record.getVersion().getVersion();
    }

    public PeriodId getPeriodId() {
        return new PeriodId(periodAuthority, periodId, PeriodId.Version.parse(version));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + this.periodId;
        hash = 97 * hash + Objects.hashCode(this.periodAuthority);
        return 97 * hash + Objects.hashCode(this.version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EmbeddablePeriodId other = (EmbeddablePeriodId) obj;
        if (this.periodId != other.periodId) {
            return false;
        }
        if (!Objects.equals(this.periodAuthority, other.periodAuthority)) {
            return false;
        }
        return Objects.equals(this.version, other.version);
    }

}
