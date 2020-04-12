/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import org.thespheres.betula.StudentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.services.vcard.VCardStudent;

/**
 *
 * @author boris.heithecker
 */
public class MappedStudent extends VCardStudent {

    private final Marker career;

    public MappedStudent(StudentId id, Marker career) {
        super(id);
        this.career = career;
    }

    public Marker getCareer() {
        return career;
    }

    public String getDisplayName() {
        String ret = getFullName();
        if (career != null) {
            ret += " (" + career.getShortLabel().replace("KGS ", "") + ")";
        }
        return ret;
    }

}
