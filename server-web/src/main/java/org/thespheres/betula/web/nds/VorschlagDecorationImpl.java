/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.nds;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.thespheres.betula.assess.GradeFactory;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeReference;
import org.thespheres.betula.assess.TargetDocument;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.niedersachsen.vorschlag.AVSVVorschlag;
import org.thespheres.betula.server.beans.ReportsBean;

/**
 *
 * @author boris.heithecker
 */
abstract class VorschlagDecorationImpl {

    @EJB(beanName = "ReportsBeanImpl")
    private ReportsBean zeugnisBean;
//    private final Map<TermId, Map<StudentId, Grade>> vorschlaege = new HashMap<>();
    private final static Grade PENDING = GradeFactory.find("niedersachsen.ersatzeintrag", "pending");

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Grade resolveReference(final GradeReference proxy, final StudentId student, final TermId term, TargetDocument target) {
        if (proxy.getConvention().equals(AVSVVorschlag.NAME) && "vorschlag".equals(proxy.getId())) {
//            final Grade g = vorschlaege
//                    .computeIfAbsent(term, t -> new HashMap<>())
//                    .get(student);
//            if (g == null) {
            final Grade gf = findKopfnote(student, term);
            if (gf != null) {
//                    vorschlaege.get(term).put(student, gf);
                return gf;
            }
            return PENDING;
//            }
        }
        return null;
    }

    private Grade findKopfnote(final StudentId student, final TermId term) {
        final DocumentId[] d = zeugnisBean.findTermReports(student, term, false);
        Grade g = null;
        if (d != null && d.length == 1) {
            DocumentId z = d[0];
            g = zeugnisBean.getKopfnote(z, getConvention());
        }
        if (g == null) {
            g = PENDING;
        }
        return g;
    }

    //Proxyable bean must have default constructor!
    protected abstract String getConvention();

}
