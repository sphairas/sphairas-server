/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.util.Objects;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.document.Marker;

/**
 *
 * @author boris.heithecker
 */
public class AvailableStudent {

    private final String dirName;
    private final StudentId id;
    private final Marker sgl;

    AvailableStudent(StudentId sid, String dirName, Marker sgl) {
        this.id = sid;
        this.dirName = dirName;
        this.sgl = sgl;
//        Logger.getLogger(AvailableStudent.class.getCanonicalName()).info("Student " + id.getId() + "  " + dirName);
    }

    public String getDirectoryName() {
        String ret = this.dirName;
        if (sgl != null) {
            ret += " (" + sgl.getShortLabel() + ")";
        }
        return ret;
    }

    public void setDirectoryName(String name) {
    }

    //Growl message notification
    public String getFullname() {
        String[] pp = this.dirName.split(",");
        if (pp.length == 2) {
            String ret = pp[1] + " " + pp[0];
            return ret.trim();
        }
        return this.dirName;
    }

    public StudentId getId() {
        return id;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AvailableStudent other = (AvailableStudent) obj;
        return Objects.equals(this.id, other.id);
    }

}
