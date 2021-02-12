/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.Local;
import javax.persistence.LockModeType;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.UnitDocumentEntity;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface UnitDocumentFacade {

    public UnitDocumentEntity create(UnitId unit, String suffix, SigneeEntity creator);

    public void edit(UnitDocumentEntity unitDocumentEntity, boolean notifyDocumentUpdate);

    public void remove(UnitDocumentEntity unitDocumentEntity);

    public UnitDocumentEntity find(DocumentId id, LockModeType lmt);

    public List<UnitDocumentEntity> findAll(LockModeType lmt);

    public List<UnitDocumentEntity> findAll(Marker selector, LockModeType lmt);

    public UnitId findPrimaryUnitForStudent(StudentId sid, Date asOf, LockModeType lmt);

    public Set<UnitDocumentEntity> findForStudents(Set<StudentId> students);

    public Map<UnitId, List<Signee>> getPrimaryUnitsSignees(DocumentId document);

    public UnitId getPrimaryUnit(DocumentId document, Signee signee);

    public void setPrimaryUnit(DocumentId document, Signee signee, UnitId unit, String propagationId);

    public StudentId[] getIntersection(DocumentId docId, StudentId[] my);

    public String getCommonName(DocumentId d, UnitId uid);

    public void setCommonName(DocumentId d, UnitId uid, String cn);

    public List<UnitDocumentEntity> getAllPrimaryUnits(final java.sql.Timestamp expringBefore);

    public void adoptStudentsToVersionAsOf(UnitDocumentEntity ude, Date date, Set<StudentId> set);

}
