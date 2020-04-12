/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "DOCUMENT_STRING_MAP_DOCUMENT")
@Access(AccessType.FIELD)
public class DocumentStringMapDocumentEntity extends BaseIdentityStringMapDocumentEntity<DocumentId> implements Serializable {

    private static final long serialVersionUID = 1L;

    public DocumentStringMapDocumentEntity() {
    }

    public DocumentStringMapDocumentEntity(DocumentId id, SigneeEntity creator) {
        super(id, creator);
    }

    public DocumentStringMapDocumentEntity(DocumentId id, SigneeEntity creator, Date creationTime) {
        super(id, creator, creationTime);
    }

    @Override
    protected EmbeddableIdentity toEmbeddableIdentity(DocumentId key) {
        if (!key.getVersion().equals(DocumentId.Version.LATEST)) {
            throw new IllegalArgumentException("Cannot use DocumentStringMapDocumentEntity for DocumentId keys with DocumentId.Version unequal to \"latest\"");
        }
        return super.toEmbeddableIdentity(key);
    }

    @Override
    protected String getId(DocumentId id) {
        return id.getId();
    }

}
