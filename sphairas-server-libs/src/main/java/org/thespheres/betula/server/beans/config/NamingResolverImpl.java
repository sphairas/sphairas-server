/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans.config;

import org.thespheres.betula.server.beans.clients.ServiceInternalClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.thespheres.betula.server.beans.annot.Delegate;
import java.util.Collections;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.io.Serializable;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.thespheres.betula.Identity;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.ProviderInfo;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.ws.CommonDocuments;

/**
 *
 * @author boris.heithecker
 */
@Default
@ApplicationScoped
public class NamingResolverImpl implements Serializable, NamingResolver {

    @Inject
    private DocumentsModel dm;
    @Inject
    private CommonDocuments cd;
    @Delegate
    @Inject
    private NamingResolver delegate;
    private ServiceInternalClient client;

    @PostConstruct
    public void initiatlize() {
        client = RestClientBuilder.newBuilder()
                .baseUri(URI.create(ServiceInternalClient.URI_SERVICE_API))
                .hostnameVerifier((hostname, session) -> true) // Optional: specific verifier
                .build(ServiceInternalClient.class);
    }

    @Override
    public Result resolveDisplayNameResult(Identity id) throws IllegalAuthorityException {
        UnitId uid = null;
        if (id instanceof UnitId unitId) {
            uid = unitId;

        } else if (id instanceof DocumentId documentId) {
            uid = dm.convertToUnitId(documentId);
        }
        final DocumentId cNames = cd.forName(CommonDocuments.COMMON_NAMES_DOCID);
        if (uid != null && cNames != null) {
            try {
                final String cn = client.getUnitCommonName(cNames, uid);
                if (!StringUtils.isBlank(cn)) {
                    return new SimpleResult(cn);
                }
            } catch (final WebApplicationException wex) {
                Logger.getLogger(NamingResolverImpl.class.getName()).log(Level.SEVERE, "Error invoking unit-common-name on " + uid.toString() + " with common names document " + cNames.toString(), wex);
            }
        }
        if (delegate != null) {
            return delegate.resolveDisplayNameResult(id);
        }
        final String val = id.getId().toString();
        return new SimpleResult(val);
    }

    @Override
    public ProviderInfo getInfo() {
        return delegate.getInfo();
    }

    //do not keep reference to ubean!
    static class SimpleResult extends NamingResolver.Result {

        private final String name;

        private SimpleResult(String name) {
            super(Collections.EMPTY_MAP);
            this.name = name;
        }

        @Override
        public String getResolvedName(Object... params) {
            return name;
        }

    }
}
