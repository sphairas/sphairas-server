/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.util.Objects;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.model.MultiSubject;

/**
 *
 * @author boris.heithecker@gmx.net
 */
class MultiSubjectExt extends MultiSubject {

    private final String altSubject;

    MultiSubjectExt(final Marker realm, final String alt) {
        super(realm);
        this.altSubject = alt;
    }

    public String getAltSubject() {
        return altSubject;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.altSubject);
        hash = 29 * hash + Objects.hashCode(getSubjectMarkerSet());
        return 29 * hash + Objects.hashCode(getDiscrimatorMap());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MultiSubject)) {
            return false;
        }
        final MultiSubject other = (MultiSubject) obj;
        if (!Objects.equals(getSubjectMarkerSet(), other.getSubjectMarkerSet())) {
            return false;
        }
        if (!Objects.equals(getDiscrimatorMap(), other.getDiscrimatorMap())) {
            return false;
        }
        final String alt = other instanceof MultiSubjectExt ? ((MultiSubjectExt) other).altSubject : null;
        return Objects.equals(this.altSubject, alt);
    }

}
