/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import org.thespheres.betula.services.vcard.VCardStudent;
import org.thespheres.betula.validation.impl.ZensurenschnittResult;
import org.thespheres.betula.validation.impl.ZensurenschnittValidationConfiguration;

/**
 *
 * @author boris.heithecker
 */
class OneZensurenschnittResult extends ZensurenschnittResult<VCardStudent, ReportDoc> {

    OneZensurenschnittResult(VCardStudent student, ReportDoc report, ZensurenschnittValidationConfiguration config) {
        super(student, report, config);
    }

}
