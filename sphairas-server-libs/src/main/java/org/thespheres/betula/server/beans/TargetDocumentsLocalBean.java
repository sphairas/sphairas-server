/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import java.util.Collection;
import javax.ejb.Local;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface TargetDocumentsLocalBean {

    public Collection<Marker> getDocumentMarkers(DocumentId d);
}
