/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.LocalBean;
import javax.ejb.Startup;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.openide.filesystems.FileObject;
import org.thespheres.betula.server.beans.config.LayerConfigUtilities;
import org.thespheres.betula.validation.impl.CareerAwareGradeCondition;
import org.thespheres.betula.validation.impl.VersetzungsValidation;
import org.thespheres.betula.validation.impl.VersetzungsValidationConfiguration;
import org.thespheres.betula.validation.impl.PolicyLegalHint;

/**
 *
 * @author boris.heithecker
 */
@Startup
@Singleton
@LocalBean
public class VersetzungsValidationBean {

    private VersetzungsValidationConfiguration config;

    @PostConstruct
    void init() {
        final FileObject configFo = LayerConfigUtilities.findLastConfigFile("/ValidationEngine/Configuration/org-thespheres-betula-niedersachsen-admin-ui-validate-VersetzungsValidation/");
        try {
            final JAXBContext ctx = JAXBContext.newInstance(VersetzungsValidationConfiguration.class, CareerAwareGradeCondition.class);
            config = (VersetzungsValidationConfiguration) ctx.createUnmarshaller().unmarshal(configFo.getInputStream());
        } catch (JAXBException | FileNotFoundException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Set<OneVersetzungsResult> validate(final OneHistory h) {
        class OneValidation extends VersetzungsValidation<ReportDoc, OneHistory, OneVersetzungsResult> {

            OneValidation(VersetzungsValidationConfiguration cfg) {
                super(h, cfg);
            }

            @Override
            protected OneVersetzungsResult createResult(ReportDoc report, List<PolicyLegalHint> hints) {
                return new OneVersetzungsResult(h.student, report, hints);
            }

        }
        final OneValidation validation = new OneValidation(config);
        validation.run();
        return validation;
    }

}
