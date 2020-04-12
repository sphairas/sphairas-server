/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.config;

import java.util.Collections;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import org.thespheres.betula.Identity;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.entities.facade.UnitDocumentFacade;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.ProviderInfo;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.ws.CommonDocuments;

/**
 *
 * @author boris.heithecker
 */
@Default
@Dependent
public class NamingResolverImpl implements NamingResolver {

    @Inject
    private UnitDocumentFacade ubean;
    @Inject
    private DocumentsModel dm;
    @Inject
    private CommonDocuments cd;
    @Delegate
    @Inject
    private NamingResolver delegate;

    @Override
    public Result resolveDisplayNameResult(Identity id) throws IllegalAuthorityException {
        UnitId uid = null;
        if (id instanceof UnitId) {
            uid = (UnitId) id;

        } else if (id instanceof DocumentId) {
            uid = dm.convertToUnitId((DocumentId) id);
        }
        final DocumentId cNames = cd.forName(CommonDocuments.COMMON_NAMES_DOCID);
        if (uid != null && cNames != null) {
            final String cn = ubean.getCommonName(cNames, uid);
            if (cn != null) {
                return new SimpleResult(cn);
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
