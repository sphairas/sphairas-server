/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

/**
 *
 * @author boris.heithecker
 * @param <T>
 */
@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Access(AccessType.FIELD)
public abstract class BaseNoteEntity<T> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected long id;
    @JoinColumns({
        @JoinColumn(name = "DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID"),
        @JoinColumn(name = "DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY"),
        @JoinColumn(name = "DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION")}
    )
    @ManyToOne(fetch = FetchType.LAZY)
    private BaseDocumentEntity document;
    @Column(name = "NOTE_KEY", length = 64, nullable = true)
    private String key;

    protected BaseNoteEntity() {
    }

    protected BaseNoteEntity(final BaseDocumentEntity document, final String key) {
        this.document = document;
        this.key = key;
    }

    public BaseDocumentEntity getDocument() {
        return document;
    }

    public long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (int) (this.id ^ (this.id >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof BaseNoteEntity)) {
            return false;
        }
        final BaseNoteEntity<?> other = (BaseNoteEntity<?>) obj;
        return this.id == other.id;
    }

}
