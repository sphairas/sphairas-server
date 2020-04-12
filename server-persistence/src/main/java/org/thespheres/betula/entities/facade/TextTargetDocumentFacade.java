/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade;

import java.util.Collection;
import java.util.List;
import javax.ejb.Local;
import javax.persistence.LockModeType;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.entities.BaseDocumentEntity;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.TermTextTargetAssessmentEntity;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface TextTargetDocumentFacade {

    public void edit(TermTextTargetAssessmentEntity target);

    public TermTextTargetAssessmentEntity find(DocumentId id, LockModeType lmt);

    public BaseDocumentEntity findBaseDocumentEntity(DocumentId id, LockModeType lmt);

    public Collection<TermTextTargetAssessmentEntity> findForPrimaryUnit(UnitId primaryUnit, LockModeType lmt);

    public List<TermTextTargetAssessmentEntity> findAll(LockModeType lmt);

    public List<TermTextTargetAssessmentEntity> findAll(SigneeEntity signee, LockModeType lmt);

    public boolean submit(DocumentId id, StudentId student, TermId gradeId, Marker section, String grade, java.sql.Timestamp ts, long lock);

    public TermTextTargetAssessmentEntity create(DocumentId docId, UnitId related);

    public long getLock(DocumentId id, long timeout);

    public void releaseLock(DocumentId id, long lock);
}
