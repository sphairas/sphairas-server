/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.localbeans;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.SessionScoped;
//import jakarta.enterprise.inject.New;
import jakarta.enterprise.inject.Produces;
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
    //Vor Jakarta @New FastTargetDocuments2Session ret
    public FastTargetDocuments2 createSessionScopedFastTargetDocuments2(FastTargetDocuments2Session ret) {
        return ret;
    }

    @DocumentsRequest
    @Produces
    //Vor Jakarta @New FastTargetDocuments2Request ret
    public FastTargetDocuments2 createRequestScopedFastTargetDocuments2(FastTargetDocuments2Request ret) {
        return ret;
    }

}
