/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.facade;

import java.util.List;
import javax.ejb.Local;
import javax.persistence.LockModeType;
import org.thespheres.betula.Identity;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.server.beans.NoEntityFoundException;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.entities.BaseTicketEntity;
import org.thespheres.betula.entities.StudentsTicketEntity;
import org.thespheres.betula.entities.TermGradeTargAssessTicketEnt;
import org.thespheres.betula.entities.UnitTicketEntity;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface TicketFacade {

    public List<BaseTicketEntity> getTickets(DocumentId targetDoc, TermId term, StudentId student, String signeeType);

    public List<BaseTicketEntity> getTickets(Identity scope);

    public BaseTicketEntity getTicket(Ticket ticket, LockModeType lmt) throws NoEntityFoundException;

    public UnitTicketEntity createUnitTicketEntity(UnitId identity, TermId term, String signeeType, String[] targetTypes, StudentId[] exempted);

    public TermGradeTargAssessTicketEnt createTermGradeTargetAssessmentTicketEntity(DocumentId targetDoc, TermId term, StudentId student, String signeeType) throws NoEntityFoundException;

    public StudentsTicketEntity createStudentTicket(StudentId[] studs, TermId term, String signeeType);

    public void removeTicket(Ticket ticket) throws NoEntityFoundException;
}
