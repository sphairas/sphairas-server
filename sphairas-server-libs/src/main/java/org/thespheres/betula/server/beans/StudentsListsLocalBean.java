/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import java.util.Date;
import javax.ejb.Local;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface StudentsListsLocalBean {

    public String getStringEntry(StudentId id, DocumentId document, Date asOf);

    public Marker getMarkerEntry(StudentId id, DocumentId document, Date asOf);
    
    public UnitId findPrimaryUnit(StudentId id, Date asOf);
}
