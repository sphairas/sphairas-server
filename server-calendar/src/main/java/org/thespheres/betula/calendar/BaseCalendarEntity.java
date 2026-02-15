/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 * @param <C>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "BASE_CALENDAR")
@IdClass(DocumentId.class)
@Access(AccessType.FIELD)
public abstract class BaseCalendarEntity<C extends UniqueCalendarComponentEntity>  implements Serializable {

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
    @Version
    @Column(name = "BASE_CALENDAR_VERSION")
    private long entityVersion;
    @OneToMany(mappedBy = "calendar")
    @OrderColumn(name = "BASE_CALENDAR_COMPONENT_ORDER")
    private List<UniqueCalendarComponentEntity> components = new ArrayList<>();
    @ManyToMany(mappedBy = "calendars")
    @OrderColumn(name = "BASE_CALENDAR_TIMEZONE_ORDER")
    protected List<TimezoneEntity> timezones = new ArrayList<>();

    public BaseCalendarEntity() {
    }

    protected BaseCalendarEntity(DocumentId id) {
        this.id = id.getId();
        this.authority = id.getAuthority();
        this.version = id.getVersion().getVersion();
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public DocumentId getDocumentId() {
        return new DocumentId(authority, id, DocumentId.Version.parse(version));
    }

    public List<UniqueCalendarComponentEntity> getComponents() {
        return components;
    }

    public List<TimezoneEntity> getTimezones() {
        return timezones;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.id);
        hash = 59 * hash + Objects.hashCode(this.authority);
        hash = 59 * hash + Objects.hashCode(this.version);
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
        final BaseCalendarEntity other = (BaseCalendarEntity) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.authority, other.authority)) {
            return false;
        }
        return Objects.equals(this.version, other.version);
    }

}
