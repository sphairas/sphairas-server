/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients;

import java.io.Serializable;
import java.util.function.Supplier;
import javax.enterprise.context.SessionScoped;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris
 */
@SessionScoped
public class ClientConfiguration implements Serializable {

    public String getAuthority() {
        return "demo/1";
    }

    public static DocumentId parseDocumentId(final String representation, final Supplier<String> getAuthority) {
        if (representation == null || representation.isEmpty()) {
            return DocumentId.NULL;
        }
        final String[] authParts = representation.split("@");
        final String authority;
        switch (authParts.length) {
            case 1:
                authority = getAuthority.get();
                break;
            case 2:
                authority = authParts[1];
                break;
            default:
                throw new IllegalArgumentException("DocumentId can have only one @ character.");
        }
        final String[] versionParts = authParts[0].split("#");

        final DocumentId.Version version;
        switch (versionParts.length) {
            case 1:
                version = DocumentId.Version.LATEST;
                break;
            case 2:
                version = DocumentId.Version.parse(authParts[0]);
                break;
            default:
                throw new IllegalArgumentException("DocumentId id part can have only one # character.");
        }
        final String id = versionParts[0];
        return new DocumentId(authority, id, version);
    }

}
