/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.util;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.thespheres.betula.document.AbstractMarker;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.MarkerFactory;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddableMarker implements Marker, Serializable {

    @Column(name = "MARKER_CONVENTION", length = 64)
    protected String convention;
    @Column(name = "MARKER_SUBSET", length = 64)
    protected String subset;
    @Column(name = "MARKER_ID", length = 64)
    protected String markerId;

    protected EmbeddableMarker() {
    }

    public EmbeddableMarker(Marker original) {
        this.convention = original.getConvention();
        this.markerId = original.getId();
        this.subset = original.getSubset();
    }

    public EmbeddableMarker(String convention, String markerId, String subset) {
        this.convention = convention;
        this.subset = subset;
        this.markerId = markerId;
    }

    @Override
    public String getConvention() {
        return convention;
    }

    @Override
    public String getSubset() {
        return subset;
    }

    @Override
    public String getId() {
        return markerId;
    }

    @Override
    public String getLongLabel(Object... formattingArgs) {
        return getMarker() != null ? getMarker().getLongLabel() : "";
    }

    @Override
    public String getShortLabel() {
        return getMarker() != null ? getMarker().getShortLabel() : "";
    }

    public Marker getMarker() {
        Marker ret = MarkerFactory.find(getConvention(), getId(), getSubset());
        if (ret == null) {
            ret = new AbstractMarker(getConvention(), getId(), getSubset());
        }
        return ret;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.convention);
        hash = 41 * hash + Objects.hashCode(this.subset);
        return 41 * hash + Objects.hashCode(this.markerId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Marker)) {
            return false;
        }
        final Marker other = (Marker) obj;
        if (!Objects.equals(convention, other.getConvention())) {
            return false;
        }
        if (!Objects.equals(subset, other.getSubset())) {
            return false;
        }
        return Objects.equals(markerId, other.getId());
    }

}
