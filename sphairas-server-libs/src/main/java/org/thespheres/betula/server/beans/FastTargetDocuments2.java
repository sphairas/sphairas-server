/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import javax.ejb.Local;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface FastTargetDocuments2 {

    public FastTermTargetDocument getFastTermTargetDocument(DocumentId d);

    public FastTextTermTargetDocument getFastTextTermTargetDocument(DocumentId d);

    public Collection<DocumentId> getTargetAssessmentDocuments();

    public Collection<StudentId> getStudents();

    public Collection<StudentId> getPrimaryUnitStudents();

    public Collection<UnitId> getUnits();

    public UnitId getPrimaryUnit();

    public Collection<DocumentId> getTargetAssessmentDocuments(UnitId primaryUnit);

    public Collection<DocumentId> getTargetAssessmentDocumentsForTerm(UnitId unit, TermId term);

    public Collection<Marker> getDocumentMarkers(DocumentId d);

    public Collection<StudentId> getStudents(UnitId pu, Date asOf);

    public Ticket[] getTickets(DocumentId docId, TermId termId, StudentId studId);

    public Grade selectSingle(DocumentId d, StudentId student, TermId term);

    public Grade findSingle(StudentId student, TermId term, Marker fach, String suffix);

    public boolean submitSingle(DocumentId docId, StudentId studId, TermId termId, Grade grade);

    public StudentId[] getIntersection(UnitId unit);

    public StudentId[] getIntersection(StudentId[] student);

    public Collection<String> getPatternChannels();

//    public JoinedUnitsEntry getJoinedUnits(DocumentId base);

    public Grade[] findSingleChecked(UnitId unit, TermId term, StudentId student, Collection<DocumentId> selectFrom);

    Collection<DocumentId> getTargetAssessmentDocumentsForTerm(final UnitId unit, final TermId term, final Map<DocumentId, FastTermTargetDocument> map);

}
