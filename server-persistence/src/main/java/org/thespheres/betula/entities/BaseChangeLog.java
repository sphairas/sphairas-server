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
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 *
 * @author boris.heithecker
 * @param <T>
 */
@Entity
@Table(name = "BASE_CHANGELOG")
@Inheritance(strategy = InheritanceType.JOINED)
@Access(AccessType.FIELD)
public abstract class BaseChangeLog<T> implements Serializable {

    public enum Action {
        UPDATE,
        ADD,
        REMOVE
    }
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected long id;
    @JoinColumns({
        @JoinColumn(name = "DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID"),
        @JoinColumn(name = "DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY"),
        @JoinColumn(name = "DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION")}
    )
    @ManyToOne(fetch = FetchType.LAZY)
    private BaseDocumentEntity baseDocumentEntity;
    @Column(name = "PROPERTY", length = 64)
    protected String property;
    @Enumerated
    @Column(name = "LOG_ACTION")
    protected Action action;
    @Column(name = "FLAGS")
    protected int flags;
//    @Column(name = "LOG_EFFECTIVE_DATE")
//    private java.sql.Timestamp effective;

    public BaseChangeLog() {
    }

    protected BaseChangeLog(BaseDocumentEntity parent, String property, Action action) {
        this.baseDocumentEntity = parent;
        this.property = property;
        this.action = action;
    }

    public String getProperty() {
        return property;
    }

    public abstract T getValue();

    public BaseDocumentEntity getBaseDocumentEntity() {
        return baseDocumentEntity;
    }

    public Action getAction() {
        return action;
    }

    public boolean isIgnore() {
        return (flags & 1) == 1;
    }

    public void setIgnore(final boolean ignore) {
        if (ignore) {
            flags |= 1;
        } else {
            flags &= ~1;
        }
    }
}
