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
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.thespheres.betula.RecordId;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddableRecordId implements Serializable {

    @Column(name = "RECORD_ID", length = 16)
    private String recordId;
    @Column(name = "RECORD_AUTHORITY", length = 64)
    private String recordAuthority;

    public EmbeddableRecordId() {
    }

    public EmbeddableRecordId(RecordId record) {
        this.recordId = record.getId();
        this.recordAuthority = record.getAuthority();
    }

    public RecordId getRecordId() {
        return new RecordId(recordAuthority, recordId);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 73 * hash + Objects.hashCode(this.recordId);
        return 73 * hash + Objects.hashCode(this.recordAuthority);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EmbeddableRecordId other = (EmbeddableRecordId) obj;
        if (!Objects.equals(this.recordId, other.recordId)) {
            return false;
        }
        return Objects.equals(this.recordAuthority, other.recordAuthority);
    }

}
