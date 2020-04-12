/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.Local;
import javax.persistence.LockModeType;
import org.thespheres.betula.Identity;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.entities.BaseDocumentEntity;
import org.thespheres.betula.entities.GradeTargetAssessmentEntity;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.UnitDocumentEntity;
import org.thespheres.betula.util.GradeEntry;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface GradeTargetDocumentFacade {

    public BaseDocumentEntity findBaseDocumentEntity(DocumentId id, LockModeType lmt);

    public GradeTargetAssessmentEntity find(DocumentId id, LockModeType lmt);

    public <T extends GradeTargetAssessmentEntity> List<T> findAll(LockModeType lmt, Class<T> type);

    public <T extends GradeTargetAssessmentEntity> List<T> findAll(SigneeEntity signee, Class<T> type, LockModeType lmt);

    @Deprecated
    public <I extends Identity> Map<StudentId, Grade> findAll(DocumentId id, I gradeId);

    public <I extends Identity> Map<StudentId, GradeEntry> findAllEntries(DocumentId id, I gradeId);

    public <I extends Identity> boolean submit(DocumentId id, StudentId student, I gradeId, Grade grade, java.sql.Timestamp ts);

    //This will fire a bulk document event!
    public <I extends Identity> boolean submitAll(DocumentId id, I gradeId, Map<StudentId, Grade> grades, Map<StudentId, java.sql.Timestamp> timestamps);

    //This will fire a bulk document event!
    public <I extends Identity> boolean clearAll(DocumentId id, I gradeId, Set<StudentId> students);

    public <I extends Identity> GradeTargetAssessmentEntity<I> create(Class<? extends GradeTargetAssessmentEntity<I>> entityClass, DocumentId docId, UnitId unit);

    public DocumentId[] findDocument(StudentId student, TermId term, Marker fach);

    public List<TermGradeTargetAssessmentEntity> findForUnitDocument(UnitDocumentEntity related, TermId term);

    public Collection<TermGradeTargetAssessmentEntity> findForStudent(final StudentId related);

    public Collection<TermGradeTargetAssessmentEntity> findForStudents(Set<StudentId> related, TermId term);

    public boolean linkPrimaryUnits(DocumentId docId, StudentId[] toArray);

//    public UnitJoinDocumentEntity[] findJoinedUnits(UnitId unit);

    public boolean remove(DocumentId docId);

    public String getStringValue(DocumentId stringValueDocument, DocumentId base);

    public void setStringValue(DocumentId stringValueDocument, DocumentId base, String value);
}
