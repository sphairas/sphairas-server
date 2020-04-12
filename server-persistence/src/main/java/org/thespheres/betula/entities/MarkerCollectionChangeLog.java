/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Table;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "MARKER_COLLECTION_CHANGELOG")
@Access(AccessType.FIELD)
public class MarkerCollectionChangeLog extends BaseChangeLog<EmbeddableMarker> implements Serializable {

    @Embedded
    @JoinColumns({
        @JoinColumn(name = "MARKER_CONVENTION", referencedColumnName = "MARKER_CONVENTION"),
        @JoinColumn(name = "MARKER_SUBSET", referencedColumnName = "MARKER_SUBSET"),
        @JoinColumn(name = "MARKER_ID", referencedColumnName = "MARKER_ID")
    })
    private EmbeddableMarker marker;

    public MarkerCollectionChangeLog() {
    }

    public MarkerCollectionChangeLog(BaseDocumentEntity parent, String property, EmbeddableMarker value, Action action) {
        super(parent, property, action);
        this.marker = value;
    }

    @Override
    public EmbeddableMarker getValue() {
        return marker;
    }

}
