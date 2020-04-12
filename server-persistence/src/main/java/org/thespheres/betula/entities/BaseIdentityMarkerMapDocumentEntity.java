/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.MapKey;
import javax.persistence.Table;
import org.thespheres.betula.Identity;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;

@Entity
@Table(name = "BASE_IDENTITY_MARKER_MAP_DOCUMENT")
@Access(AccessType.FIELD)
public abstract class BaseIdentityMarkerMapDocumentEntity<I extends Identity> extends BaseDocumentEntity implements Serializable {

    public static final String BASE_IDENTITY_MARKER_MAP_DOCUMENT_VALUES = "BASE_IDENTITY_MARKER_MAP_DOCUMENT_VALUES";
    @Embedded
    @ElementCollection
    @MapKey(name = "mapKey")
    @CollectionTable(name = "BASE_IDENTITY_MARKER_MAP_DOCUMENT_VALUES",
            joinColumns = {
                @JoinColumn(name = "BASEIDENTITYMARKERMAP_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", updatable = false, insertable = false),
                @JoinColumn(name = "BASEIDENTITYMARKERMAP_DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", updatable = false, insertable = false),
                @JoinColumn(name = "BASEIDENTITYMARKERMAP_DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION", updatable = false, insertable = false)})
    protected Map<EmbeddableIdentity, BaseIdentityMarkerMapDocumentEntityEntry> mapValues = new HashMap<>();
//    protected Map<EmbeddableIdentity, EmbeddableMarker> mapValues = new HashMap<>();  BUG IN ECLIPSELINK ==> database not updated correctly if it was not empty

    public BaseIdentityMarkerMapDocumentEntity() {
    }

    public BaseIdentityMarkerMapDocumentEntity(DocumentId id, SigneeEntity creator) {
        super(id, creator);
    }

    public BaseIdentityMarkerMapDocumentEntity(DocumentId id, SigneeEntity creator, Date creationTime) {
        super(id, creator, creationTime);
    }

    public Marker get(I key) {
        EmbeddableIdentity eid = createEmbeddableIdentity(key);
//        return Optional.ofNullable(values.get(eid)).map((v) -> v.getMarker()).orElse(null); KEIN JAVA 8 !!!!
        return mapValues.containsKey(eid) ? mapValues.get(eid).getMarker() : null;
    }

    public Marker put(final I id, final Marker value) {
        final EmbeddableIdentity key = createEmbeddableIdentity(id);
        final BaseIdentityMarkerMapDocumentEntityEntry ret;
        if (!Marker.isNull(value)) {
            final BaseIdentityMarkerMapDocumentEntityEntry ev = new BaseIdentityMarkerMapDocumentEntityEntry(key, value);
            ret = mapValues.put(ev.getKey(), ev);
        } else {
            ret = mapValues.remove(key);
        }
        return ret != null ? ret.getMarker() : null;
    }

    protected abstract I createIdentity(String authority, String id);

    protected abstract EmbeddableIdentity createEmbeddableIdentity(I id);
}
