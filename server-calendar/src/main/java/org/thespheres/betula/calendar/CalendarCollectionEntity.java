/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
@Entity
@IdClass(DocumentId.class)
@Table(name = "CALENDARCOLLECTION")
@Access(AccessType.FIELD)
public class CalendarCollectionEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "CALENDAR_ID")
    private String id;
    @Id
    @Column(name = "CALENDAR_AUTHORITY", length = 64)
    private String authority;
    @Id
    @Column(name = "CALENDAR_VERSION", length = 32)
    private String version;
    @ManyToMany
    @JoinTable(name = "CALENDARCOLLECTION_CALENDARFILES",
            joinColumns = {
                @JoinColumn(name = "COLLECTION_ID", referencedColumnName = "CALENDAR_ID"),
                @JoinColumn(name = "COLLECTION_AUTHORITY", referencedColumnName = "CALENDAR_AUTHORITY"),
                @JoinColumn(name = "COLLECTION_VERSION", referencedColumnName = "CALENDAR_VERSION")},
            inverseJoinColumns = {
                @JoinColumn(name = "CALENDARCOMPONENT_SYSUID", referencedColumnName = "UID_SYSID"),
                @JoinColumn(name = "CALENDARCOMPONENT_HOST", referencedColumnName = "UID_HOST")})
    protected final Set<UniqueCalendarComponentEntity> collectionComponents = new HashSet<>();

    public CalendarCollectionEntity() {
        this(null);
    }

    public CalendarCollectionEntity(DocumentId id) {
        if (id != null) {
            this.id = id.getId();
            this.authority = id.getAuthority();
            this.version = id.getVersion().getVersion();
        }
    }

    public DocumentId getDocumentId() {
        return new DocumentId(authority, id, DocumentId.Version.parse(version));
    }

    public Set<UniqueCalendarComponentEntity> getCollectionComponents() {
        return collectionComponents;
    }
}
