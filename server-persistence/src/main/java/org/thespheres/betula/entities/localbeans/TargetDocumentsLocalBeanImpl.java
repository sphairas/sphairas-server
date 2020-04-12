/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.localbeans;

import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.LockModeType;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.entities.BaseDocumentEntity;
import org.thespheres.betula.entities.EmbeddableMarker;
import org.thespheres.betula.entities.facade.GradeTargetDocumentFacade;
import org.thespheres.betula.server.beans.TargetDocumentsLocalBean;

/**
 *
 * @author boris.heithecker
 */
@RolesAllowed({"signee", "unitadmin"})
@LocalBean
@Stateless
public class TargetDocumentsLocalBeanImpl implements TargetDocumentsLocalBean {

    @EJB
    private GradeTargetDocumentFacade facade;

    @Override
    public Collection<Marker> getDocumentMarkers(DocumentId d) {//TODO: securityCheck
        final BaseDocumentEntity e = facade.findBaseDocumentEntity(d, LockModeType.OPTIMISTIC);
        if (e != null) {
            return e.getEmbeddableMarkers().stream()
                    .map(EmbeddableMarker::getMarker)
                    .collect(Collectors.toSet());
        }
        return null;
    }
}
