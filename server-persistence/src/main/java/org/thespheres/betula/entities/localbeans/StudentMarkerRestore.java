/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.localbeans;

import org.thespheres.betula.StudentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.entities.BaseChangeLog;
import org.thespheres.betula.entities.BaseIdentityMarkerMapDocumentEntity;
import org.thespheres.betula.entities.RestoreVersion;
import org.thespheres.betula.entities.StudentMarkerMapChangeLog;

/**
 *
 * @author boris.heithecker
 */
class StudentMarkerRestore implements RestoreVersion {

    private Marker restored;
    private final StudentId id;

    StudentMarkerRestore(StudentId student, Marker orig) {
        id = student;
        restored = orig;
    }

    @Override
    public void applyChangeLog(BaseChangeLog bcl) {
        if (bcl instanceof StudentMarkerMapChangeLog
                && bcl.getProperty().equals(BaseIdentityMarkerMapDocumentEntity.BASE_IDENTITY_MARKER_MAP_DOCUMENT_VALUES)) {
            final StudentMarkerMapChangeLog log = (StudentMarkerMapChangeLog) bcl;
            if (id.equals(log.getStudent())) {
                switch (log.getAction()) {
                    case REMOVE:
                        restored = log.getValue();
                        break;
                    case ADD:
                        restored = null;
                        break;
                    case UPDATE:
                        restored = log.getValue();
                        break;
                }
            }
        }
    }

    Marker getRestored() {
        return restored;
    }

}
