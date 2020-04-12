/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.beanimpl;

import java.util.Collection;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.calendar.facade.StudentFacade;
import org.thespheres.betula.server.beans.StudentsLocalBean;
import org.thespheres.ical.VCard;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class StudentVCardsImpl implements StudentsLocalBean {

    @EJB
    private StudentFacade facade;

    @RolesAllowed({"signee", "unitadmin", "remoteadmin"})
    @Override
    public VCard get(StudentId student) {
        return facade.findVCard(student);
    }

    @RolesAllowed({"signee", "unitadmin", "remoteadmin"})
    @Override
    public Collection<VCard> getAll() {
        return facade.findAllVCards();
    }
}
