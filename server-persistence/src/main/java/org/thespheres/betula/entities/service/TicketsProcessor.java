/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.util.TicketEntry;
import org.thespheres.betula.entities.BaseAssessmentEntry;
import org.thespheres.betula.entities.BaseTicketEntity;
import org.thespheres.betula.entities.GradeTargetAssessmentEntity;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.facade.GradeTargetDocumentFacade;
import org.thespheres.betula.entities.facade.TicketFacade;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.Paths;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.UnauthorizedException;

/**
 *
 * @author boris.heithecker
 */
@Dependent
@Stateless
public class TicketsProcessor extends AbstractContainerProcessor {

    @EJB
    private TicketFacade tickets;
    @Inject
    private GradeTargetDocumentFacade targets;

    public TicketsProcessor() {
        super(new String[][]{Paths.TICKETS_PATH, Paths.TARGETS_TICKETS_PATH});
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    @Override
    public void process(final String[] path, final Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException {
        if (Arrays.equals(path, Paths.TARGETS_TICKETS_PATH)) {
            processTargetTickets(template);
        }
    }

    private void processTargetTickets(final Envelope node) throws SyntaxException {
        final Action a = node.getAction();
        if (Objects.equals(a, Action.REQUEST_COMPLETION)) {
            node.setAction(Action.RETURN_COMPLETION);
            node.getChildren().clear();
            if (ServiceUtils.isEntryOf(node, DocumentId.class)) {
                final Entry<DocumentId, ?> te = ServiceUtils.toEntry(node, DocumentId.class);
                final GradeTargetAssessmentEntity find = targets.find(te.getIdentity(), LockModeType.OPTIMISTIC);
                try {
                    addTickets(te, (TermGradeTargetAssessmentEntity) find);
                } catch (final ClassCastException e) {
                    throw ServiceUtils.createSyntaxException(e);
                }
            } else {
                targets.findAll(LockModeType.OPTIMISTIC, TermGradeTargetAssessmentEntity.class)
                        .forEach(td -> {
                            final Entry<DocumentId, ?> te = new Entry<>(null, td.getDocumentId());
                            node.getChildren().add(te);
                            addTickets(te, td);
                        });
            }
        }
    }

    private void addTickets(final Envelope node, final TermGradeTargetAssessmentEntity document) {
        final Map<TermId, Map<StudentId, List<BaseTicketEntity>>> ret = new HashMap<>();
        if (document != null) {
            final Set<? extends BaseAssessmentEntry<?>> entries = document.getEntries();
            for (final BaseAssessmentEntry e : entries) {
                if (!(e.getGradeId() instanceof TermId)) {
                    continue;
                }
                final TermId t = (TermId) e.getGradeId();
                final StudentId s = e.getStudentId();
                final List<BaseTicketEntity> l = tickets.getTickets(document.getDocumentId(), t, s, "entitled.signee");//TODO: lookup permitted signeeTypes
                if (!l.isEmpty()) {
                    ret.computeIfAbsent(t, k -> new HashMap<>())
                            .put(s, l);
                }
            }
        }
        for (final Map.Entry<TermId, Map<StudentId, List<BaseTicketEntity>>> mte : ret.entrySet()) {
            final Entry<TermId, ?> te = new Entry<>(null, mte.getKey());
            node.getChildren().add(te);
            for (final Map.Entry<StudentId, List<BaseTicketEntity>> mse : mte.getValue().entrySet()) {
                final Entry<StudentId, ?> se = new Entry<>(null, mse.getKey());
                te.getChildren().add(se);
                for (final BaseTicketEntity bte : mse.getValue()) {
                    final TicketEntry ticket = TicketsProcessorUtils.toEntry(bte);
                    se.getChildren().add(ticket);
                }
            }
        }
    }

}
