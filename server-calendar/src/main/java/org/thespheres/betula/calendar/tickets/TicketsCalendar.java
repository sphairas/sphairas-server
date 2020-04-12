/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.tickets;

import java.io.Serializable;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.thespheres.betula.calendar.BaseCalendarEntity;
import org.thespheres.betula.calendar.UniqueCalendarComponentEntity;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "TICKETS_CALENDAR")
@Access(AccessType.FIELD)
public class TicketsCalendar extends BaseCalendarEntity<UniqueCalendarComponentEntity> implements Serializable {

    public TicketsCalendar() {
    }

    public TicketsCalendar(DocumentId id) {
        super(id);
    }

}
