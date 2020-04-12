/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.util.Date;
import java.util.List;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Documents;

/**
 *
 * @author boris.heithecker
 */
public class ChangeLogs {

    private ChangeLogs() {
    }

    public static DocumentId.Version addLogWithVersionChangeLog(final BaseDocumentEntity doc, final List<BaseChangeLog> l, final Date effective, final Integer magnitude) {
        if (magnitude != null && magnitude < 1) {
            throw new IllegalArgumentException("Magnitude less than 1");
        }
        DocumentId.Version ret = null;
        final boolean revision = magnitude == null;
        VersionChangeLog ceiling = null;
        VersionChangeLog ceilingNonRevision = null;
        VersionChangeLog floorRevision = null;
        VersionChangeLog floorNonRevision = null;
        final List<BaseChangeLog> cl = doc.getChangeLog();
        int index = cl.size();
        for (int i = cl.size() - 1; i >= 0; i--) {
            final BaseChangeLog bcl = cl.get(i);
            if (bcl instanceof VersionChangeLog) {
                final VersionChangeLog log = (VersionChangeLog) bcl;
                if (effective != null && effective.before(log.getTimeStamp())) {
                    ceiling = log;
                    if (!ceiling.isRevision()) {
                        ceilingNonRevision = ceiling;
                    }
                    floorRevision = null;
                    floorNonRevision = null;
                    index = i;
                } else if (ceiling != null && floorRevision == null && floorNonRevision == null && log.isRevision()) {
                    floorRevision = log;
                } else if (ceiling != null && floorNonRevision == null && !log.isRevision()) {
                    floorNonRevision = log;
                }
            }
        }
        if (ceiling == null) {
            //Simply add to the end;
            final DocumentId.Version ov = doc.getCurrentVersion();
            final Date d = effective != null ? effective : new Date(System.currentTimeMillis());
            final VersionChangeLog vcl = new VersionChangeLog(doc, revision ? null : ov, d);
            if (!revision) {
                ret = Documents.inc(ov, magnitude - 1);
                doc.setCurrentVersion(ret);
            }
            doc.addChangeLog(vcl);
            l.forEach(doc::addChangeLog);
        } else {
            if (!revision) {
                if (floorNonRevision == null || !floorNonRevision.getTimeStamp().equals(effective)) {
//The difficult task: we have to insert a prior log entry WITH version inc. 
                    final DocumentId.Version floorVersion = floorNonRevision != null ? floorNonRevision.getValueAsVersion() : doc.getCurrentVersion();
                    ret = Documents.inc(floorVersion, magnitude - 1);
                    if (ceilingNonRevision != null) {
                        //Check possible conflict
                        if (Documents.compare(ceilingNonRevision.getValueAsVersion(), ret) <= 0) {
                            ret = null;
                            //TODO Warning
                            if (floorRevision == null || !floorRevision.getTimeStamp().equals(effective)) {
                                final VersionChangeLog vcl = new VersionChangeLog(doc, ret, effective);
                                doc.addChangeLogAt(vcl, index);
                            }
                        } else {
                            final VersionChangeLog vcl = new VersionChangeLog(doc, ret, effective);
                            doc.addChangeLogAt(vcl, index);
                        }
                    }
                }
                for (int i = l.size() - 1; i >= 0; i--) {
                    doc.addChangeLogAt(l.get(i), index + 1);
                }
            } else {
                if (!(floorRevision != null && floorRevision.getTimeStamp().equals(effective)
                        || floorNonRevision != null && floorNonRevision.getTimeStamp().equals(effective))) {
                    final VersionChangeLog vcl = new VersionChangeLog(doc, null, effective);
                    doc.addChangeLogAt(vcl, index);
                }
                for (int i = l.size() - 1; i >= 0; i--) {
                    doc.addChangeLogAt(l.get(i), index + 1);
                }
            }
        }
        return ret;
    }

}
