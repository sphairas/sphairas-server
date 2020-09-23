/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.config;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import org.thespheres.betula.Identity;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.document.util.DocumentUtilities;
import org.thespheres.betula.document.util.UnitEntry;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.ProviderInfo;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.ws.BetulaWebService;
import org.thespheres.betula.services.ws.Paths;
import org.thespheres.betula.services.ws.ServiceException;
import org.thespheres.betula.util.CollectionUtil;
import org.thespheres.betula.util.ContainerBuilder;

/**
 *
 * @author boris.heithecker
 */
@Default
@Dependent
public class NamingResolverImpl implements NamingResolver {

    @Inject
    private DocumentsModel dm;
    @Delegate
    @Inject
    private NamingResolver delegate;
    @Inject
    private BetulaWebService service;

    @Override
    public Result resolveDisplayNameResult(final Identity id) throws IllegalAuthorityException {
        final Result res = delegate.resolveDisplayNameResult(id);
        if (res.hasResolverHint(NamingResolver.Result.HINT_UNRESOLVED)) {
            UnitId uid = null; 
            if (id instanceof UnitId) {
                uid = (UnitId) id;

            } else if (id instanceof DocumentId) {
                uid = dm.convertToUnitId((DocumentId) id);
            }
            if (uid != null) {
                try {
                    final String cn = findCommenName(uid);
                    if (cn != null) {
                        return new SimpleResult(cn);
                    }
                } catch (final IOException ex) {//log
                }
            }
        }
        return res;
    }

    @Override
    public ProviderInfo getInfo() {
        return delegate.getInfo();
    }

    private String findCommenName(final UnitId unit) throws IOException {
        final ContainerBuilder builder = new ContainerBuilder();
        final String[] path = Paths.UNITS_PARTICIPANTS_PATH;
        final DocumentId udoc = dm.convertToUnitDocumentId(unit);
        final UnitEntry uEntry = builder.updateUnitAction(udoc, unit, null, path, null, null, false, true);
        uEntry.getHints().put("request-completion.no-children", "true");
        try {
            final Container response = service.solicit(builder.getContainer());
            return extractResponseUnitEntry(response, unit);
        } catch (final ServiceException ex) {
            throw new IOException(ex);
        }
    }

    private String extractResponseUnitEntry(final Container response, final UnitId requested) {
        final List<Entry<UnitId, ?>> l = DocumentUtilities.findEntry(response, Paths.UNITS_PARTICIPANTS_PATH, UnitId.class);
        return l.stream()
                .filter(e -> e.getIdentity().equals(requested))
                .collect(CollectionUtil.requireSingleton())
                .filter(e -> e.getException() == null)
                .map(u -> u.getChildren().stream())
                .flatMap(s -> s.collect(CollectionUtil.singleton()))
                .filter(t -> t.getAction().equals(Action.RETURN_COMPLETION))
                .filter(UnitEntry.class::isInstance)
                .map(UnitEntry.class::cast)
                .map(UnitEntry::getCommonUnitName)
                .orElse(null);
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
