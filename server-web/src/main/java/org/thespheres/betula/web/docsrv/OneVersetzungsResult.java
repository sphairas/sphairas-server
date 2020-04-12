/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.util.List;
import org.thespheres.betula.services.vcard.VCardStudent;
import org.thespheres.betula.validation.impl.PolicyLegalHint;
import org.thespheres.betula.validation.impl.VersetzungsResult;

/**
 *
 * @author boris.heithecker
 */
class OneVersetzungsResult extends VersetzungsResult<VCardStudent, ReportDoc> {

    OneVersetzungsResult(VCardStudent student, ReportDoc report, List<PolicyLegalHint> hints) {
        super(student, report, hints);
    }

}
