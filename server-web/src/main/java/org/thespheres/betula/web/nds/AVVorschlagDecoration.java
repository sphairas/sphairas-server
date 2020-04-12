/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.nds;

import java.io.Serializable;
import javax.ejb.Stateless;
import org.thespheres.betula.niedersachsen.ASVAssessmentConvention;
import org.thespheres.betula.niedersachsen.vorschlag.VorschlagDecoration;
import org.thespheres.betula.web.config.Extra;

/**
 *
 * @author boris.heithecker
 */
@Extra(targetType = "arbeitsverhalten")
//@SessionScoped
@Stateless
public class AVVorschlagDecoration extends VorschlagDecorationImpl implements Serializable, VorschlagDecoration {

    @Override
    protected String getConvention() {
        return ASVAssessmentConvention.AV_NAME;
    }

}
