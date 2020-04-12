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
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.MapKeyJoinColumns;
import javax.persistence.Table;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;

@Entity
@Table(name = "SIGNEE_UNIT_MAP_DOCUMENT")
@Access(AccessType.FIELD)
public class SigneeUnitMapDocumentEntity extends BaseDocumentEntity implements Serializable {

    @Embedded
    @ElementCollection
    @MapKey(name = "mapKey")
    @MapKeyJoinColumns({//ZGN
        @MapKeyJoinColumn(name = "SIGNEEUNITMAP_SIGNEE_ID", referencedColumnName = "SIGNEE_ID"),
        @MapKeyJoinColumn(name = "SIGNEEUNITMAP_SIGNEE_AUTHORITY", referencedColumnName = "SIGNEE_AUTHORITY"),
        @MapKeyJoinColumn(name = "SIGNEEUNITMAP_SIGNEE_ALIAS", referencedColumnName = "SIGNEE_ALIAS")})
    @CollectionTable(name = "SIGNEE_UNIT_MAP_DOCUMENT_ENTRIES",
            joinColumns = {
                @JoinColumn(name = "SIGNEEUNITMAPDOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", updatable = false, insertable = false),
                @JoinColumn(name = "SIGNEEUNITMAPDOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", updatable = false, insertable = false),
                @JoinColumn(name = "SIGNEEUNITMAPDOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION", updatable = false, insertable = false)})
    protected Map<SigneeEntity, SigneeUnitMapDocumentEntityEntry> signeeUnitMap = new HashMap<>();

    public SigneeUnitMapDocumentEntity() {
    }

    public SigneeUnitMapDocumentEntity(DocumentId id, SigneeEntity creator) {
        super(id, creator);
    }

    public SigneeUnitMapDocumentEntity(DocumentId id, SigneeEntity creator, Date creationTime) {
        super(id, creator, creationTime);
    }

    public Map<SigneeEntity, SigneeUnitMapDocumentEntityEntry> map() {
        return signeeUnitMap;
    }

    public UnitId get(SigneeEntity key) {
//        EmbeddableIdentity eid = createEmbeddableIdentity(key);
//        return Optional.ofNullable(values.get(eid)).map((v) -> v.getMarker()).orElse(null); KEIN JAVA 8 !!!!
        return signeeUnitMap.containsKey(key) ? signeeUnitMap.get(key).getUnitId() : null;
    }

    public UnitId put(SigneeEntity key, UnitId value) {
        SigneeUnitMapDocumentEntityEntry ret;
        if (value == null) {
            ret = signeeUnitMap.remove(key);
        } else {
            SigneeUnitMapDocumentEntityEntry ev = new SigneeUnitMapDocumentEntityEntry(key, value);
            ret = signeeUnitMap.put(ev.getKey(), ev);
        }
        return ret != null ? ret.getUnitId() : null;
    }

}
