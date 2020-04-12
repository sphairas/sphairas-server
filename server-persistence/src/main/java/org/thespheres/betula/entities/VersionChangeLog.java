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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "DOCUMENT_VERSION_CHANGELOG")
@Access(AccessType.FIELD)
public class VersionChangeLog extends BaseChangeLog<String> implements Serializable {

    @Column(name = "VERSION_CHANGELOG_TIMESTAMP")
    private java.sql.Timestamp timeStamp;
    @Column(name = "VERSION_CHANGELOG_VERSION")
    private String version;
    public static final String VALUE_REVISION = "revision";

    public VersionChangeLog() {
    }

    @Deprecated //use static method
    public VersionChangeLog(BaseDocumentEntity parent, DocumentId.Version version, final Date timestamp) {
        super(parent, "CURRENTVERSION", Action.UPDATE);
        this.version = version != null ? version.getVersion() : VALUE_REVISION;
        this.timeStamp = timestamp != null ? new java.sql.Timestamp(timestamp.getTime()) : new java.sql.Timestamp(System.currentTimeMillis());
    }

    @Override
    public String getValue() {
        return version;
    }

    public DocumentId.Version getValueAsVersion() {
        return isRevision() ? null : DocumentId.Version.parse(version);
    }

//Never returns null , check null because of corrupted DB
    public Date getTimeStamp() {
        return timeStamp != null ? new Date(timeStamp.getTime()) : null;
    }

    public boolean isRevision() {
        return getValue().equals(VALUE_REVISION);
    }

    @Override
    public String toString() {
        return StudentMarkerMapChangeLog.class.getName() + " : " + getProperty() + " : " + getAction().toString() + " : " + version + " : " + timeStamp.toString();
    }
}
