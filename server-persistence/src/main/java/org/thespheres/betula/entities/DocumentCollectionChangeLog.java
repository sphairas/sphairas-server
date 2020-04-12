/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "DOCUMENT_COLLECTION_CHANGELOG")
@Access(AccessType.FIELD)
public class DocumentCollectionChangeLog extends BaseChangeLog<DocumentId> implements Serializable {

    @Column(name = "LOG_DOCUMENT_ID")
    private String documentId;
    @Column(name = "LOG_DOCUMENT_AUTHORITY", length = 64)
    private String documentAuthority;
    @Column(name = "LOG_DOCUMENT_VERSION", length = 32)
    private String documentVersion;

    public DocumentCollectionChangeLog() {
    }

    public DocumentCollectionChangeLog(BaseDocumentEntity parent, String property, DocumentId value, Action action) {
        super(parent, property, action);
        this.documentId = value.getId();
        this.documentAuthority = value.getAuthority();
        this.documentVersion = value.getVersion().getVersion();
    }

    @Override
    public DocumentId getValue() {
        return new DocumentId(documentAuthority, documentId, DocumentId.Version.parse(documentVersion));
    }
}
