/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service;

import org.thespheres.betula.assess.AbstractGrade;
import org.thespheres.betula.util.GradeAdapter;

/**
 *
 * @author boris.heithecker
 */
public final class ProxyGrade extends AbstractGrade {

    private ProxyGrade(final GradeAdapter adapter) {
        super(adapter.getConvention(), adapter.getId());
    }
    
    public static ProxyGrade create(final GradeAdapter adapter) {
        return new ProxyGrade(adapter);
    }

    @Override
    public String getShortLabel() {
        throw new UnsupportedOperationException("Must not be called.");
    }

    @Override
    public String getLongLabel(Object... formattingArgs) {
        throw new UnsupportedOperationException("Must not be called.");
    }

}
