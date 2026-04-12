package org.thespheres.betula.web.config;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.server.beans.AmbiguousDateException;
import org.thespheres.betula.server.beans.ReportsBean.CustomNote;
import org.thespheres.betula.server.beans.TermReportDataException;
import org.thespheres.betula.server.beans.clients.BadRequest;
import org.thespheres.ical.InvalidComponentException;
import org.thespheres.ical.VCard;
import org.thespheres.ical.builder.VCardBuilder;

/**
 *
 * @author boris.heithecker
 */
//@Dependent
public class InternalServices { //implements CalendarsBean, StudentsLocalBean, ReportsBean, StudentsListsLocalBean, TargetDocumentsLocalBean {

    @Inject
    private AppConfiguration config;

    public Ticket[] findApplicableTickets(DocumentId docId, TermId termId, StudentId studId) {
        final String t = config.getInternalClient().findApplicableTickets(docId, termId, studId);
        return Arrays.stream(t.split("\n"))
                .filter(StringUtils::isNotBlank)
                .map(Ticket::valueOf)
                .toArray(Ticket[]::new);
    }

    public VCard getStudentVCard(StudentId student) {
        try {
            final String c = config.getInternalClient().getStudentVCard(student);
            return VCardBuilder.parseCards(new StringReader(c)).getFirst();
        } catch (IOException ex) {
            return null;
        } catch (ParseException | InvalidComponentException e) {
            return null;
        }
    }

    public Date getDate(String category, UnitId unit, TermId termId, DocumentId zgn, String moreCategories) throws BadRequest {
        final Date ret = config.getInternalClient().getReportDate(category, unit, termId, zgn, moreCategories);
        return ret;
    }


    public Date getDate(String category, UnitId unit, TermId termId, DocumentId zgn, String[] cat) throws AmbiguousDateException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public Collection<VCard> getAll() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public DocumentId[] findTermReports(StudentId student, TermId term, boolean create) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public StudentId getStudent(DocumentId zgnId) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public TermId getTerm(DocumentId zgnId) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public Marker[] getMarkers(DocumentId zeugnis) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public boolean addMarker(DocumentId zeugnis, Marker m) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public boolean removeMarker(DocumentId zeugnis, Marker m) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public Grade getKopfnote(DocumentId zeugnis, String convention) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

  public boolean setKopfnote(DocumentId zeugnis, String convention, Grade grade) throws TermReportDataException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public Integer getIntegerValue(DocumentId zeugnis, String type) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public boolean setIntegerValue(DocumentId zeugnis, String type, Integer value) throws TermReportDataException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public CustomNote[] getCustomNotes(DocumentId zeugnis) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public void setCustomNotes(DocumentId zeugnis, CustomNote[] notes) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public String getNote(DocumentId zgn, String key) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public void setNote(DocumentId zgn, String key, String value) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public String[] getAGs(StudentId student, TermId term) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public String getStringEntry(StudentId id, DocumentId document, Date asOf) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public Marker getMarkerEntry(StudentId id, DocumentId document, Date asOf) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public UnitId findPrimaryUnit(StudentId id, Date asOf) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public Collection<Marker> getDocumentMarkers(DocumentId d) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public String getSubjectAlternativeName(DocumentId d) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
