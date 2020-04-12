/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
public class CrossMarkSubject {

    private final DocumentId doc;
    private final Marker subject;

    CrossMarkSubject(final DocumentId doc, final Marker subject) {
        this.doc = doc;
        this.subject = subject;
    }

    public String getDisplayName() {
        return subject.getLongLabel();
    }

    public Marker getSubject() {
        return subject;
    }

    public DocumentId getDocument() {
        return doc;
    }

}
