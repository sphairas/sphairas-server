/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.localbeans;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import org.thespheres.betula.server.beans.FastTargetDocuments2;
import org.thespheres.betula.server.beans.annot.DocumentsRequest;
import org.thespheres.betula.server.beans.annot.DocumentsSession;

/**
 *
 * @author boris.heithecker
 */
@ApplicationScoped
public class FastTargetDocumentProducers {

    @DocumentsSession
    @SessionScoped
    @Produces
    public FastTargetDocuments2 createSessionScopedFastTargetDocuments2(@New FastTargetDocuments2Session ret) {
        return ret;
    }

    @DocumentsRequest
    @Produces
    public FastTargetDocuments2 createRequestScopedFastTargetDocuments2(@New FastTargetDocuments2Request ret) {
        return ret;
    }

}
