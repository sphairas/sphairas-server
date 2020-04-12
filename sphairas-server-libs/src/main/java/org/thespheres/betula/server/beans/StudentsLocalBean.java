/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import java.util.Collection;
import javax.ejb.Local;
import org.thespheres.betula.StudentId;
import org.thespheres.ical.VCard;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface StudentsLocalBean {

    public VCard get(StudentId student);

    public Collection<VCard> getAll();

}
