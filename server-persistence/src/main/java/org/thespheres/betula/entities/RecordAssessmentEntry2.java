/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.thespheres.betula.RecordId;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "RECORD_TARGETASSESSMENT_DOCUMENT_ENTRIES2",
        //        uniqueConstraints = {
        //            @UniqueConstraint(columnNames = {"RECORD_AUTHORITY", "RECORD_ID", "STUDENT_AUTHORITY", "STUDENT_ID", "RECORD_TARGETASSESSMENT_DOCUMENT_ID", "RECORD_TARGETASSESSMENT_DOCUMENT_AUTHORITY", "RECORD_TARGETASSESSMENT_DOCUMENT_VERSION"})
        //        },
        indexes = {
            @Index(name = "INDEX_RCRDTRGTASSMNTDCMNT_NTRS2_RCRDATHRTY_RCRDID", columnList = "RECORD_AUTHORITY, RECORD_ID"),
            @Index(columnList = "STUDENT_AUTHORITY, STUDENT_ID")})
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Access(AccessType.FIELD)
public class RecordAssessmentEntry2 extends BaseAssessmentEntry<RecordId> implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected long id;
    @JoinColumns({
        @JoinColumn(name = "RECORD_TARGETASSESSMENT_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", nullable = false),
        @JoinColumn(name = "RECORD_TARGETASSESSMENT_DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", nullable = false),
        @JoinColumn(name = "RECORD_TARGETASSESSMENT_DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION", nullable = false)})
    @ManyToOne(fetch = FetchType.LAZY)
    private RecordTargetAssessmentEntity document;
    @Embedded
    private EmbeddableRecordId record;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "RECORD_TARGETASSESSMENT_DOCUMENT_ENTRIES2_TEXT_NOTES",
            joinColumns = {
                @JoinColumn(name = "RECORD_ENTRIY_ID", referencedColumnName = "ID")},
            inverseJoinColumns = {
                @JoinColumn(name = "NOTE_ID", referencedColumnName = "ID")})
    private Set<TextNote> notes = new HashSet<>();

    public RecordAssessmentEntry2() {
    }

    public RecordAssessmentEntry2(StudentId student) {
        super(student);
    }

    public RecordAssessmentEntry2(final RecordTargetAssessmentEntity parent, final StudentId student, final RecordId record) {
        this(student);
        this.document = parent;
        this.record = new EmbeddableRecordId(record);
    }

    public DocumentId getDocument() {
        return document.getDocumentId();
    }

    @Override
    public RecordId getGradeId() {
        return record.getRecordId();
    }

    public Set<TextNote> getNotes() {
        return notes;
    }
    
}
