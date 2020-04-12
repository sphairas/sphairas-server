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
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.DocumentId.Version;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddableDocumentId implements Serializable {
    
    @Column(name = "DOCUMENT_AUTHORITY", length = 64)
    private String authority;
    @Column(name = "DOCUMENT_ID")
    private String id;
    @Column(name = "DOCUMENT_VERSION", length = 32)
    @Size(max = 32)
    private String version;
    
    public EmbeddableDocumentId() {
    }
    
    public EmbeddableDocumentId(DocumentId id) {
        this.authority = id.getAuthority();
        this.id = id.getId();
        this.version = id.getVersion().getVersion();
    }
    
    public DocumentId getDocumentId() {
        return new DocumentId(authority, id, Version.parse(version));
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.authority);
        hash = 31 * hash + Objects.hashCode(this.id);
        hash = 31 * hash + Objects.hashCode(this.version);
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
        final EmbeddableDocumentId other = (EmbeddableDocumentId) obj;
        if (!Objects.equals(this.authority, other.authority)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return Objects.equals(this.version, other.version);
    }
    
}
