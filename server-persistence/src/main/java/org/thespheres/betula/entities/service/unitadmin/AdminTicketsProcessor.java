/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.unitadmin;

import org.thespheres.betula.entities.service.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.Dependent;
import javax.persistence.LockModeType;
import org.thespheres.betula.Identity;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.server.beans.NoEntityFoundException;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.Template;
import org.thespheres.betula.document.util.GenericXmlTicket;
import org.thespheres.betula.document.util.GenericXmlTicket.XmlTicketScope;
import org.thespheres.betula.document.util.TicketEntry;
import org.thespheres.betula.entities.BaseTicketEntity;
import org.thespheres.betula.entities.facade.TicketFacade;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.Paths;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.UnauthorizedException;
import org.thespheres.betula.util.CollectionUtil;

/**
 *
 * @author boris.heithecker
 */
@Dependent
@Stateless
public class AdminTicketsProcessor extends AbstractAdminContainerProcessor {

    @EJB
    private TicketFacade tickets;

    public AdminTicketsProcessor() {
        super(new String[][]{Paths.TICKETS_PATH, Paths.UNITS_TICKETS_PATH});
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    @Override
    public void process(String[] path, Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException {
        if (Arrays.equals(path, Paths.UNITS_TICKETS_PATH)) {
            processUnitsTickets(template);
        } else if (Arrays.equals(path, Paths.TICKETS_PATH)) {
            processTickets(template);
        }
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void process(Container container, String[] path, Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void processUnitsTickets(Envelope node) throws SyntaxException {
        final Action a = node.getAction();
        if (a != null && a.equals(Action.REQUEST_COMPLETION)) {
            addTickets(node, null);
        } else if (a == null) {
            for (Template<?> t : node.getChildren()) {
                final Action uea = t.getAction();
                if (uea != null && uea.equals(Action.REQUEST_COMPLETION)) {
                    final Entry<UnitId, ?> ue = ServiceUtils.toEntry(node, UnitId.class);
                    addTickets(ue, ue.getIdentity());
                }
            }
        }
    }

    private void addTickets(Envelope node, UnitId scope) {
        final List<BaseTicketEntity> l = tickets.getUnitTickets(scope);
        node.getChildren().clear();
        node.setAction(Action.RETURN_COMPLETION);
        l.stream()
                .map(TicketsProcessorUtils::toEntry)
                .filter(Objects::nonNull)
                .forEach(node.getChildren()::add);
    }

    private BaseTicketEntity createUnitTicket(GenericXmlTicket xml) throws SyntaxException {
        final List<XmlTicketScope> scope = xml.getScope();
        UnitId unit;
        TermId term;
        String signeeType;
        try {
            unit = extractSingleScopeDefinitionValue(scope, UnitId.class, "unit", "include");
            term = extractSingleScopeDefinitionValue(scope, TermId.class, "term", "include");
            signeeType = extractSingleScopeDefinitionTextValue(scope, "entitlement", "include");
        } catch (IllegalStateException e) {
            throw ServiceUtils.createSyntaxException(e);
        }
        List<StudentId> exempt = extractMultipleScopeDefinitionValue(scope, StudentId.class, "student", "exclude");
        List<String> targetTypes = extractMultipleScopeDefinitionValue(scope, "target-type", "include");
        String[] tt = (targetTypes == null || targetTypes.isEmpty()) ? null : targetTypes.stream().toArray(String[]::new);
        StudentId[] es = (exempt == null || exempt.isEmpty()) ? null : exempt.stream().toArray(StudentId[]::new);
        return tickets.createUnitTicketEntity(unit, term, signeeType, tt, es);
    }

    private BaseTicketEntity createStudentTicket(GenericXmlTicket xml) throws SyntaxException {
        final List<XmlTicketScope> scope = xml.getScope();
        TermId term;
        String signeeType;
        try {
            term = extractSingleScopeDefinitionValue(scope, TermId.class, "term", "include");
            signeeType = extractSingleScopeDefinitionTextValue(scope, "entitlement", "include");
        } catch (IllegalStateException e) {
            throw ServiceUtils.createSyntaxException(e);
        }
        List<StudentId> incl = extractMultipleScopeDefinitionValue(scope, StudentId.class, "student", "include");
        StudentId[] es = (incl == null || incl.isEmpty()) ? null : incl.stream().toArray(StudentId[]::new);
        return tickets.createStudentTicket(es, term, signeeType);
    }

    private BaseTicketEntity createTermGradeTargetTicket(GenericXmlTicket xml) throws SyntaxException, NotFoundException {
        final List<XmlTicketScope> scope = xml.getScope();
        DocumentId targetDoc;
        TermId term;
        StudentId stud;
        String signeeType;
        try {
            targetDoc = extractSingleScopeDefinitionValue(scope, DocumentId.class, "target", "include");
            term = extractSingleScopeDefinitionValue(scope, TermId.class, "term", "include");
            signeeType = extractSingleScopeDefinitionTextValue(scope, "entitlement", "include");
            stud = extractSingleScopeDefinitionValue(scope, StudentId.class, "student", "include");
        } catch (IllegalStateException e) {
            throw ServiceUtils.createSyntaxException(e);
        }
        try {
            return tickets.createTermGradeTargetAssessmentTicketEntity(targetDoc, term, stud, signeeType);
        } catch (NoEntityFoundException ex) {
            throw ServiceUtils.createNotFoundException(targetDoc);
        }
    }

    private <I extends Identity> void processTickets(Envelope t) throws NotFoundException, SyntaxException {
        final TicketEntry te = ServiceUtils.toEntryType(t, TicketEntry.class);
        final Ticket ticket = te.getIdentity();
        final GenericXmlTicket xmlTicket = te.getValue();
        final Action action = te.getAction();
        if (action != null) {
            if (action.equals(Action.REQUEST_COMPLETION) && ticket != null) {
                BaseTicketEntity bte;
                try {
                    bte = tickets.getTicket(ticket, LockModeType.OPTIMISTIC);
                } catch (NoEntityFoundException ex) {
                    throw ServiceUtils.createNotFoundException(ticket);
                }
                final TicketEntry entry = TicketsProcessorUtils.toEntry(bte);
                te.setAction(Action.RETURN_COMPLETION);
                te.setValue(entry.getValue());
            } else if (action.equals(Action.FILE) && xmlTicket != null && ticket == null) {
                final String tc = xmlTicket.getTicketClass().getValue();
                final BaseTicketEntity bte;
                switch (tc) {
                    case "unit-ticket":
                        bte = createUnitTicket(xmlTicket);
                        break;
                    case "student-ticket":
                        bte = createStudentTicket(xmlTicket);
                        break;
                    case "target-document-ticket":
                        bte = createTermGradeTargetTicket(xmlTicket);
                        break;
                    default:
                        throw ServiceUtils.createSyntaxException("Not a valid ticket class: " + tc);
                }
                te.setAction(Action.CONFIRM);
                te.setIdentity(bte.getTicket());
            } else if (action.equals(Action.ANNUL) && ticket != null) {
                try {
                    tickets.removeTicket(ticket);
                } catch (NoEntityFoundException ex) {
                    throw ServiceUtils.createNotFoundException(ticket);
                }
                te.setAction(Action.CONFIRM);
                te.setIdentity(ticket);
                te.setValue(null);
            }
        }
    }

    static <I extends Identity> I extractSingleScopeDefinitionValue(final List<XmlTicketScope> l, Class<I> clz, String scope, String action) {
        return l.stream()
                .filter(s -> scope.equals(s.getScope()))
                .filter(s -> s.getAction().equals(action))
                .map(s -> clz.cast(s.getValue()))
                .collect(CollectionUtil.requireSingleOrNull());
    }

    static String extractSingleScopeDefinitionTextValue(final List<XmlTicketScope> l, String scope, String action) {
        return l.stream()
                .filter(s -> scope.equals(s.getScope()))
                .filter(s -> s.getAction().equals(action))
                .map(s -> s.getTextValue())
                .collect(CollectionUtil.requireSingleOrNull());
    }

    static <I extends Identity> List<I> extractMultipleScopeDefinitionValue(final List<XmlTicketScope> l, Class<I> clz, String scope, String action) {
        return l.stream()
                .filter(s -> scope.equals(s.getScope()))
                .filter(s -> s.getAction().equals(action))
                .map(s -> clz.cast(s.getValue()))
                .collect(Collectors.toList());
    }

    static List<String> extractMultipleScopeDefinitionValue(final List<XmlTicketScope> l, String scope, String action) {
        return l.stream()
                .filter(s -> scope.equals(s.getScope()))
                .filter(s -> s.getAction().equals(action))
                .map(s -> s.getTextValue())
                .collect(Collectors.toList());
    }

}
