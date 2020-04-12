/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.util.Objects;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.DocumentId.Version;

/**
 *
 * @author boris.heithecker
 */
public class DocumentIdWrapper {

    private final String id;
    private final String authority;
    private final String version;

    public DocumentIdWrapper(String authority, String id, String version) {
        this.id = id;
        this.authority = authority;
        this.version = version;
    }

    public DocumentId unwrap() {
        return new DocumentId(id, authority, Version.parse(version));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.id);
        hash = 41 * hash + Objects.hashCode(this.authority);
        return 41 * hash + Objects.hashCode(this.version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DocumentIdWrapper other = (DocumentIdWrapper) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.authority, other.authority)) {
            return false;
        }
        return Objects.equals(this.version, other.version);
    }

}
