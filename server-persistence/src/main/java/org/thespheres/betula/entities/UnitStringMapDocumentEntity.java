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
import javax.persistence.Entity;
import javax.persistence.Table;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "UNIT_STRING_MAP_DOCUMENT")
@Access(AccessType.FIELD)
public class UnitStringMapDocumentEntity extends BaseIdentityStringMapDocumentEntity<UnitId> implements Serializable {

    private static final long serialVersionUID = 1L;

    public UnitStringMapDocumentEntity() {
    }

    public UnitStringMapDocumentEntity(DocumentId id, SigneeEntity creator) {
        super(id, creator);
    }

    public UnitStringMapDocumentEntity(DocumentId id, SigneeEntity creator, Date creationTime) {
        super(id, creator, creationTime);
    }

    @Override
    protected String getId(UnitId id) {
        return id.getId();
    }

}
