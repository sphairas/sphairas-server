/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.facade;

import java.util.List;
import javax.ejb.Local;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.calendar.lesson.Lesson;
import org.thespheres.betula.calendar.lesson.LessonCalendar;
import org.thespheres.betula.document.Signee;
import org.thespheres.ical.ICalendar;
import org.thespheres.ical.UID;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface LessonCalendarFacade {

    public LessonCalendar getCalendar();

    public ICalendar getPublished(UID[] restrict, CalendarCompatibilities compat);

    public ICalendar getPublished(UnitId unit, CalendarCompatibilities compat);

    public List<Lesson> findLessonsForSignee(Signee signee);
}
