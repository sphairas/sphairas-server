/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service;

import java.util.Arrays;
import java.util.List;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.util.GenericXmlTicket;
import org.thespheres.betula.document.util.TicketEntry;
import org.thespheres.betula.entities.BaseTicketEntity;
import org.thespheres.betula.entities.StudentsTicketEntity;
import org.thespheres.betula.entities.TermGradeTargAssessTicketEnt;
import org.thespheres.betula.entities.UnitTicketEntity;

/**
 *
 * @author boris.heithecker@gmx.net
 */
public class TicketsProcessorUtils {

    public static TicketEntry toEntry(BaseTicketEntity entity) {
        final TicketEntry ret;
        if (entity instanceof UnitTicketEntity) {
            ret = new TicketEntry(null, entity.getTicket(), "unit-ticket", "1.0");
            addTicketData(ret, (UnitTicketEntity) entity);
        } else if (entity instanceof StudentsTicketEntity) {
            ret = new TicketEntry(null, entity.getTicket(), "student-ticket", "1.0");
            addTicketData(ret, (StudentsTicketEntity) entity);
        } else if (entity instanceof TermGradeTargAssessTicketEnt) {
            ret = new TicketEntry(null, entity.getTicket(), "target-document-ticket", "1.0");
            addTicketData(ret, (TermGradeTargAssessTicketEnt) entity);
        } else {
            ret = null;
        }
        return ret;
    }

    public static void addTicketData(TicketEntry entry, UnitTicketEntity ute) {
        final List<GenericXmlTicket.XmlTicketScope> scope = entry.getValue().getScope();
        final UnitId unit = ute.getUnit();
        if (unit != null) {
            scope.add(new GenericXmlTicket.XmlTicketScope("unit", unit, "include"));
        }
        final TermId term = ute.getTerm();
        if (term != null) {
            scope.add(new GenericXmlTicket.XmlTicketScope("term", term, "include"));
        }
        final String signeeType = ute.getSigneeType();
        if (signeeType != null) {
            scope.add(new GenericXmlTicket.XmlTicketScope("entitlement", signeeType, "include"));
        }
        final String targetType = ute.getTargetType();
        if (targetType != null) {
            Arrays.stream(targetType.split(",")).map(tt -> new GenericXmlTicket.XmlTicketScope("target-type", tt, "include")).forEach(scope::add);
        }
        ute.getExemptedStudents().stream().map(es -> new GenericXmlTicket.XmlTicketScope("student", es.getStudentId(), "exclude")).forEach(scope::add);
    }

    public static void addTicketData(TicketEntry entry, StudentsTicketEntity ste) {
        final List<GenericXmlTicket.XmlTicketScope> scope = entry.getValue().getScope();
        final TermId term = ste.getTerm();
        if (term != null) {
            scope.add(new GenericXmlTicket.XmlTicketScope("term", term, "include"));
        }
        final String signeeType = ste.getSigneeType();
        if (signeeType != null) {
            scope.add(new GenericXmlTicket.XmlTicketScope("entitlement", signeeType, "include"));
        }
        ste.getStudents().stream().map(es -> new GenericXmlTicket.XmlTicketScope("student", es.getStudentId(), "include")).forEach(scope::add);
    }

    public static void addTicketData(TicketEntry entry, TermGradeTargAssessTicketEnt tgt) {
        final List<GenericXmlTicket.XmlTicketScope> scope = entry.getValue().getScope();
        scope.add(new GenericXmlTicket.XmlTicketScope("target", tgt.getTarget().getDocumentId(), "include"));
        final TermId term = tgt.getTerm();
        if (term != null) {
            scope.add(new GenericXmlTicket.XmlTicketScope("term", term, "include"));
        }
        final String signeeType = tgt.getSigneeType();
        if (signeeType != null) {
            scope.add(new GenericXmlTicket.XmlTicketScope("entitlement", signeeType, "include"));
        }
        final StudentId student = tgt.getStudent();
        if (student != null) {
            scope.add(new GenericXmlTicket.XmlTicketScope("student", student, "include"));
        }
    }

}
