/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.util.StringJoiner;
import org.openide.util.NbBundle;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.model.MultiSubject;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.services.vcard.VCardStudent;
import org.thespheres.betula.validation.impl.ZensurensprungResult;

/**
 *
 * @author boris.heithecker@gmx.net
 */
class OneZensurensprungResult extends ZensurensprungResult<VCardStudent, FastTermTargetDocument> {

    private final MultiSubject subject;

    OneZensurensprungResult(VCardStudent student, TermId term, Grade before, Grade current, DocumentId did, FastTermTargetDocument document, MultiSubject sub) {
        super(student, term, before, current, did, document);
        this.subject = sub;
    }

    @Override
    public String getMessage() {
//Zensurensprung im Fach X (Y auf Z)   FopFormatter.formatDetails.ZensurensprungValidation.message
        final StringJoiner sub = new StringJoiner(", ");
        subject.getSubjectMarkerSet().stream()
                .map(m -> m.getLongLabel())
                .forEach(sub::add);
        final String fach = subject.getRealmMarker() == null ? sub.toString() : String.join(sub.toString(), " (", subject.getRealmMarker().getLongLabel(), ")");
        return NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.ZensurensprungValidation.message", fach, getGradeBefore().getLongLabel(), getGradeCurrent().getLongLabel());
    }

}
