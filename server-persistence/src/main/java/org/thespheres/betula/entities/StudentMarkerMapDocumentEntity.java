/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.document.DocumentId;

@Entity
@Table(name = "STUDENT_MARKER_MAP_DOCUMENT")
@Access(AccessType.FIELD)
public class StudentMarkerMapDocumentEntity extends BaseIdentityMarkerMapDocumentEntity<StudentId>  implements Serializable {//BaseIdentityMarkerMapDocumentEntity<StudentId> 

    public StudentMarkerMapDocumentEntity() {
    }

    public StudentMarkerMapDocumentEntity(DocumentId id, SigneeEntity creator) {
        super(id, creator);
    }

    public StudentMarkerMapDocumentEntity(DocumentId id, SigneeEntity creator, Date creationTime) {
        super(id, creator, creationTime);
    }

    @Override
    protected StudentId createIdentity(String authority, String id) {
        return new StudentId(authority, Long.parseLong(id));
    }

    @Override
    protected EmbeddableIdentity createEmbeddableIdentity(StudentId id) {
        return new EmbeddableIdentity(id.getAuthority(), Long.toString(id.getId()));
    }

}
