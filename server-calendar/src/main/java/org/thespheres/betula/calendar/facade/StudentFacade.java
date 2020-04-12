/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.facade;

import java.util.Collection;
import javax.ejb.Local;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.ical.CardComponentProperty;
import org.thespheres.ical.VCard;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface StudentFacade {

    public void create(StudentId id, String fn);

    public VCard findVCard(StudentId id);

    public Collection<VCard> findAllVCards();

    public void update(StudentId id, CardComponentProperty property);

    public Collection<VCard> findAllVCards(UnitId unit);

    public Collection<VCard> findVCards(Collection<StudentId> students);

    public void remove(StudentId student);

}
