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

@Entity
@Table(name = "BASE_IDENTITY_STRING_MAP_DOCUMENT")
@Access(AccessType.FIELD)
public abstract class BaseIdentityStringMapDocumentEntity<I extends Identity> extends BaseDocumentEntity implements Serializable {

    @Embedded
    @ElementCollection
    @MapKey(name = "mapKey")
    @CollectionTable(name = "BASE_IDENTITY_STRING_MAP_DOCUMENT_VALUES",
            joinColumns = {
                @JoinColumn(name = "BASEIDENTITYSTRINGMAP_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", updatable = false, insertable = false),
                @JoinColumn(name = "BASEIDENTITYSTRINGMAP_DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", updatable = false, insertable = false),
                @JoinColumn(name = "BASEIDENTITYSTRINGMAP_DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION", updatable = false, insertable = false)})
    protected Map<EmbeddableIdentity, BaseIdentityStringMapDocumentEntityEntry> mapValues = new HashMap<>();

    public BaseIdentityStringMapDocumentEntity() {
    }

    public BaseIdentityStringMapDocumentEntity(DocumentId id, SigneeEntity creator) {
        super(id, creator);
    }

    public BaseIdentityStringMapDocumentEntity(DocumentId id, SigneeEntity creator, Date creationTime) {
        super(id, creator, creationTime);
    }

    public String get(final I key) {
        final EmbeddableIdentity eid = toEmbeddableIdentity(key);
        if (mapValues.containsKey(eid)) {
            return mapValues.get(eid).getString();
        }
        return null;
    }

    public String put(final I key, final String value) {
        final EmbeddableIdentity eid = toEmbeddableIdentity(key);
        if (value != null) {
            final BaseIdentityStringMapDocumentEntityEntry ev = new BaseIdentityStringMapDocumentEntityEntry(eid, value);
            final BaseIdentityStringMapDocumentEntityEntry ret = mapValues.put(ev.getKey(), ev);
            return ret != null ? ret.getString() : null;
        } else {
            final BaseIdentityStringMapDocumentEntityEntry ret = mapValues.remove(eid);
            return ret != null ? ret.getString() : null;
        }
    }

    protected EmbeddableIdentity toEmbeddableIdentity(I key) {
        return new EmbeddableIdentity(key.getAuthority(), getId(key));
    }

    protected abstract String getId(I id);
}
