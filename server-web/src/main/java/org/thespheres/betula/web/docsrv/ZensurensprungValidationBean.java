/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.model.MultiSubject;
import org.thespheres.betula.document.model.UnitsModel;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.services.vcard.VCardStudent;
import org.thespheres.betula.validation.impl.ZensurensprungValidation;

/**
 *
 * @author boris.heithecker@gmx.net
 */
//@Startup
//@Singleton
//@LocalBean
@ApplicationScoped
public class ZensurensprungValidationBean {

    @Inject
    DocumentMapper documentMapper;

    public Set<OneZensurensprungResult> validate(final UnitsModel<VCardStudent, FastTermTargetDocument> oum, TermId term) {

        class OneValidation extends ZensurensprungValidation<VCardStudent, FastTermTargetDocument, UnitsModel<VCardStudent, FastTermTargetDocument>, OneZensurensprungResult> {

            private final TermId term;

            OneValidation(UnitsModel<VCardStudent, FastTermTargetDocument> model, TermId term) {
                super(model, null);
                this.term = term;
            }

            @Override
            protected OneZensurensprungResult createResult(VCardStudent s, TermId termid, FastTermTargetDocument d, Grade grade, Grade before) {
                final MultiSubject sub = documentMapper.getSubject(d.getDocument());
                return new OneZensurensprungResult(s, termid, grade, before, d.getDocument(), d, sub);
            }

            @Override
            protected void processOneDocument(FastTermTargetDocument rtad) {
                processOneDocument(rtad, null, term);
            }

        }
        final OneValidation validation = new OneValidation(oum, term);
        validation.run();
        return validation;
    }
}
