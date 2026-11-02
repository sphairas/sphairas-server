package org.thespheres.betula.web.config;

import jakarta.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.MarkerFactory;
import org.thespheres.betula.server.beans.clients.BadRequest;
import org.thespheres.betula.server.beans.AmbiguousDateException;
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

    public Date getDate(String category, UnitId unit, TermId termId, DocumentId zgn, String moreCategories) throws BadRequest, AmbiguousDateException {
        final Date ret = config.getInternalClient().getReportDate(category, unit, termId, zgn, moreCategories);
        return ret;
    }

    public Marker getMarkerEntry(StudentId id, DocumentId document, Date asOf) {
        final String m = config.getInternalClient().getStudentMarkerEntry(id, document, asOf);
        return StringUtils.isNotBlank(m) ? MarkerFactory.resolve(m.trim()) : null;
    }

    public UnitId findPrimaryUnit(StudentId id, Date asOf) {
        final String u = config.getInternalClient().getStudentPrimaryUnit(id, asOf);
        return StringUtils.isNotBlank(u) ? UnitId.valueOf(u.trim()) : null;
    }

    public String getSubjectAlternativeName(DocumentId d) {
        return config.getInternalClient().getSubjectAlternativeName(d);
    }

}
