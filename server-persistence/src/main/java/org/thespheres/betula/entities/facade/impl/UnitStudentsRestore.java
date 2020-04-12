/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade.impl;

import java.util.Set;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.entities.BaseChangeLog;
import org.thespheres.betula.entities.RestoreVersion;
import org.thespheres.betula.entities.StudentIdCollectionChangeLog;

/**
 *
 * @author boris.heithecker
 */
class UnitStudentsRestore implements RestoreVersion {

    private final Set<StudentId> set;

    UnitStudentsRestore(final Set<StudentId> set) {
        this.set = set;
    }

    @Override
    public void applyChangeLog(BaseChangeLog bcl) {
        if (bcl instanceof StudentIdCollectionChangeLog && bcl.getProperty().equals("UNIT_DOCUMENT_STUDENTS")) {
            final StudentIdCollectionChangeLog log = (StudentIdCollectionChangeLog) bcl;
            final StudentId csid = log.getValue();
            if (csid != null) {
                switch (log.getAction()) {
                    case REMOVE:
                        set.add(csid);
                        break;
                    case ADD:
                        set.remove(csid);
                        break;
                }
            }
        }
    }

}
