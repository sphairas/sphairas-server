/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.facade;

import java.util.List;
import java.util.Set;
import javax.ejb.Local;
import javax.persistence.LockModeType;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.calendar.reports.ZeugniskonferenzEntity;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.ical.CalendarComponent;
import org.thespheres.ical.ICalendar;
import org.thespheres.ical.UID;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface ZeugniskonferenzFacade {

    public boolean remove(UID id);

    public ZeugniskonferenzEntity find(UID id, LockModeType lmt);

    public List<ZeugniskonferenzEntity> findZeugnisEvents(UnitId unit, TermId term, String category);

    public ICalendar getZeugnisCalendar(UnitId unit, TermId term);

    public ZeugniskonferenzEntity create(UnitId unit, TermId term, List<DocumentId> report, CalendarComponent populate, Set<String> category);

    public boolean update(UID uid, CalendarComponent comp, Set<String> cat);
}
