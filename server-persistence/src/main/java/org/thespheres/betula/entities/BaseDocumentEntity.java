/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import org.thespheres.betula.document.Document;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.DocumentId.Version;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.Timestamp;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "BASE_DOCUMENT")
@IdClass(DocumentId.class)
@Inheritance(strategy = InheritanceType.JOINED)
@Access(AccessType.FIELD)
public class BaseDocumentEntity implements Serializable, Document, Document.Validity {

    private static final long serialVersionUID = 1L;
    private static final java.sql.Timestamp MAX = java.sql.Timestamp.valueOf("2999-12-31 23:59:59");
    @Id
    @Column(name = "DOCUMENT_ID")
    protected String id;
    @Id
    @Size(max = 64)
    @Column(name = "DOCUMENT_AUTHORITY", length = 64)
    protected String authority;
    @Id
    @Size(max = 32)
    @Column(name = "DOCUMENT_VERSION", length = 32)
    protected String version = Version.LATEST.getVersion();
    @Size(max = 32)
    @Column(name = "CURRENTVERSION", length = 32)
    private String currentVersion;
    @javax.persistence.Version
    @Column(name = "BASE_DOCUMENT_VERSION")
    private long entityVersion;
    @JoinColumns({
        @JoinColumn(name = "CREATOR_ID", referencedColumnName = "SIGNEE_ID"),
        @JoinColumn(name = "CREATOR_AUTHORITY", referencedColumnName = "SIGNEE_AUTHORITY"),
        @JoinColumn(name = "CREATOR_ALIAS", referencedColumnName = "SIGNEE_ALIAS")
    })
    @OneToOne
    private SigneeEntity creator;
    @Column(name = "BASE_DOCUMENT_CREATION_TIMESTAMP")
    private java.sql.Timestamp creationTime = new java.sql.Timestamp(System.currentTimeMillis());
    @Column(name = "BASE_DOCUMENT_EXPIRATION")
    private java.sql.Timestamp expirationTime = MAX;
    @Embedded
    @ElementCollection()
    @CollectionTable(name = "BASE_DOCUMENT_MARKERS")
    protected Set<EmbeddableMarker> markerSet = new HashSet<>();
    @OrderColumn(name = "BASE_DOCUMENT_CHANGELOG_ORDER")
    @OneToMany(mappedBy = "baseDocumentEntity", cascade = {CascadeType.ALL}, orphanRemoval = true)
    protected List<BaseChangeLog> changeLog = new ArrayList<>();

    public BaseDocumentEntity() {
    }

    public BaseDocumentEntity(DocumentId id, SigneeEntity creator) {
        this.id = id.getId();
        this.authority = id.getAuthority();
        this.currentVersion = "1";
        this.creator = creator;
    }

    public BaseDocumentEntity(DocumentId id, SigneeEntity creator, Date creationTime) {
        this(id, creator);
        this.creationTime = new java.sql.Timestamp(creationTime.getTime());
    }

    public DocumentId getDocumentId() {
        return new DocumentId(authority, id, Version.parse(version));
    }

    @Override
    public boolean isFragment() {
        return false;
    }

    @Override
    public SigneeInfo getCreationInfo() {
        return new SigneeInfo() {

            @Override
            public Timestamp getTimestamp() {
                return EmbeddableSigneeInfo.timestampOrNull(creationTime);
            }

            @Override
            public Signee getSignee() {
                return creator != null ? creator.getSignee() : null;
            }

        };
    }

    @Override
    public Validity getDocumentValidity() {
        return this;
    }

    @Override
    public boolean isValid() {
        return new Date().after(expirationTime);
    }

    @Override
    public ZonedDateTime getExpirationDate() {
        return expirationTime != null ? ZonedDateTime.from(expirationTime.toInstant().atZone(ZoneId.systemDefault())) : null;
    }

    public void setExpirationDate(final ZonedDateTime date) {
        this.expirationTime = date != null ? java.sql.Timestamp.from(date.toInstant()) : null;
    }

    public void addMarker(Marker m) {
        if (m != null) {
            final EmbeddableMarker em = new EmbeddableMarker(m);
            markerSet.add(em);
        }
    }

    public void addMarker(final String convention, final String id, final String subset) {
        final EmbeddableMarker em = new EmbeddableMarker(convention, id, subset);
        markerSet.add(em);
    }

    public boolean removeMarker(Marker m) {
        final Iterator<EmbeddableMarker> it = markerSet.iterator();
        while (it.hasNext()) {
            if (it.next().equals(m)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public boolean removeMarker(final String convention, final String id, final String subset) {
        final Iterator<EmbeddableMarker> it = markerSet.iterator();
        while (it.hasNext()) {
            final EmbeddableMarker em = it.next();
            if (Objects.equals(em.getConvention(), convention) && Objects.equals(em.getId(), id) && Objects.equals(em.getSubset(), subset)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public Set<EmbeddableMarker> getEmbeddableMarkers() {
        return Collections.unmodifiableSet(markerSet);
    }

    @Override
    public Marker[] markers() {
        final HashSet<Marker> ret = new HashSet<>();
        for (EmbeddableMarker em : markerSet) {
            ret.add(em.getMarker());
        }
        return ret.toArray(new Marker[ret.size()]);
    }

    public void clearMarkerSet() {
        markerSet.clear();
    }

    public Version getCurrentVersion() {
        return Version.parse(currentVersion);
    }

    public void setCurrentVersion(Version v) {
        currentVersion = v.getVersion();
    }

    public void addChangeLog(BaseChangeLog log) {
        changeLog.add(log);
    }

    public void addChangeLogAt(BaseChangeLog log, final int index) {
        changeLog.add(index, log);
    }

    public List<BaseChangeLog> getChangeLog() {
        return Collections.unmodifiableList(changeLog);
    }

    public void applyRestoreVersion(final Date date, final RestoreVersion restore) {
        final List<BaseChangeLog> cl = getChangeLog();
        if (!cl.isEmpty()) {
            int stop = -1;
            for (int i = cl.size() - 1; i >= 0; i--) {
                final BaseChangeLog bcl = cl.get(i);
                if (bcl instanceof VersionChangeLog) {
                    final VersionChangeLog log = (VersionChangeLog) bcl;
                    Date ts = log.getTimeStamp();
                    if (ts == null) {
                        ts = new Date(117, 10, 3, 13, 0, 0);//log.getTimeStamp() never returns null, but: CORRUPTED DATABASE
                    }
                    if (ts.before(date)) {
                        break;
                    } else {
                        stop = i;
                    }
                }
            }
            if (stop != -1) {
                for (int i = cl.size() - 1; i > stop; i--) {
                    final BaseChangeLog bcl = cl.get(i);
                    if (bcl.isIgnore()) {
                        continue;
                    }
                    restore.applyChangeLog(bcl);
                }
            }
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 23 * hash + (this.authority != null ? this.authority.hashCode() : 0);
        return 23 * hash + (this.version != null ? this.version.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BaseDocumentEntity other = (BaseDocumentEntity) obj;
        if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
            return false;
        }
        if ((this.authority == null) ? (other.authority != null) : !this.authority.equals(other.authority)) {
            return false;
        }
        return !((this.version == null) ? (other.version != null) : !this.version.equals(other.version));
    }

    @Override
    public String toString() {
        return "org.thespheres.betula.entities.BaseDocumentEntity[ id=" + id + " ]";
    }
}
