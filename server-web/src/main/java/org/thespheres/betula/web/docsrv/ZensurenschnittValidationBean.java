/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.io.FileNotFoundException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.LocalBean;
import javax.ejb.Startup;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.openide.filesystems.FileObject;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.model.Subject;
import org.thespheres.betula.server.beans.config.LayerConfigUtilities;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.scheme.PrecedingTermGradeReference;
import org.thespheres.betula.validation.impl.CareerAwareGradeToDoubleConverter;
import org.thespheres.betula.validation.impl.ZensurenschnittValidation;
import org.thespheres.betula.validation.impl.ZensurenschnittValidationConfiguration;

/**
 *
 * @author boris.heithecker
 */
@Startup
@Singleton
@LocalBean
public class ZensurenschnittValidationBean {

    private ZensurenschnittValidationConfiguration config;

    @PostConstruct
    void init() {
        final FileObject configFo = LayerConfigUtilities.findLastConfigFile("/ValidationEngine/Configuration/org-thespheres-betula-validation-impl-ZensurenschnittValidation/");
        if (configFo != null) {
            try {
                final JAXBContext ctx = JAXBContext.newInstance(ZensurenschnittValidationConfiguration.class, CareerAwareGradeToDoubleConverter.class);
                config = (ZensurenschnittValidationConfiguration) ctx.createUnmarshaller().unmarshal(configFo.getInputStream());
            } catch (JAXBException | FileNotFoundException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    public Set<OneZensurenschnittResult> validate(final OneHistory h) {
        class OneValidation extends ZensurenschnittValidation<ReportDoc, OneHistory, OneZensurenschnittResult> {

            OneValidation(ZensurenschnittValidationConfiguration cfg) {
                super(h, cfg);
            }

            @Override
            protected OneZensurenschnittResult createResult(ReportDoc report, ZensurenschnittValidationConfiguration config) {
                return new OneZensurenschnittResult(h.student, report, config);
            }

            @Override
            protected Grade adjustGrade(Grade g, ReportDoc r, Subject s) {
                if (g instanceof PrecedingTermGradeReference) {
                    try {
                        final TermId btid = ((PrecedingTermGradeReference) g).findPrecedingTermId(r.getTerm());
                        if (btid != null) {
                            final Grade beforeGrade = r.getBeforeGrades().get(btid).get(s);
                            if (beforeGrade != null) {
                                return super.adjustGrade(beforeGrade, r, s);
                            }
                        }
                    } catch (IllegalAuthorityException ex) {
                        Logger.getLogger(ZensurenschnittValidationBean.class.getName()).log(Level.WARNING, ex.getLocalizedMessage(), ex);
                    }
                }
                return super.adjustGrade(g, r, s);
            }

        }
        final OneValidation validation = new OneValidation(config);
        validation.run();
        return validation;
    }

}
