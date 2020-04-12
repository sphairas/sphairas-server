/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.config;

import javax.enterprise.util.AnnotationLiteral;

/**
 *
 * @author boris.heithecker
 */
public class ExtraAnnotation extends AnnotationLiteral<Extra> implements Extra {

    private final String targetType;

    public ExtraAnnotation(String targetType) {
        this.targetType = targetType;
    }

    @Override
    public String targetType() {
        return targetType;
    }
}
