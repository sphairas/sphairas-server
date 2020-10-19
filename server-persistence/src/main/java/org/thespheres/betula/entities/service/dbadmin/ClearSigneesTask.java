/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.dbadmin;

import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.openide.util.NbBundle.Messages;
import org.thespheres.betula.TermId;
import org.thespheres.betula.database.DBAdminTask;
import org.thespheres.betula.database.DBAdminTaskResult;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.entities.BaseChangeLog;
import org.thespheres.betula.entities.BaseTargetAssessmentEntity;
import org.thespheres.betula.entities.ChangeLogs;
import org.thespheres.betula.entities.EmbeddableTermId;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.SigneeInfoChangeLog;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.jmsimpl.DocumentsNotificator;
import org.thespheres.betula.services.jms.AbstractDocumentEvent;

/**
 *
 * @author boris.heithecker@gmx.net
 */
@Stateless
public class ClearSigneesTask {

    static final String QUERY = "SELECT DISTINCT t FROM TermGradeTargetAssessmentEntity t, IN(t.signeeInfoentries) si WHERE NOT EXISTS(SELECT e FROM TermAssessmentEntry2 e WHERE e.term=:term AND e.document=t)";
    public static final String NAME = "clear-signees";
    public static final String VERSION = "1.0";
    @PersistenceContext(unitName = "betula0")
    private EntityManager em;
    private final static Logger LOGGER = Logger.getLogger("dbadmin-clear-signees-task");
    @Inject
    protected DocumentsNotificator documentsNotificator;

    public String getName() {
        return NAME;
    }

    public String getVersion() {
        return VERSION;
    }

    @Messages({"ClearSigneesTask.success.message={1} entries have been removed from {0} document and {2} terms.",
        "ClearSigneesTask.log.removed=Removed {1} entries from document {0}.",
        "ClearSigneesTask.log.removed.entry=Removed from document {0} entry for student {1} and term {2} with value {3}."})
//    @RolesAllowed({"superadmin"})
    public DBAdminTaskResult process(final DBAdminTask task) {
        if (!task.getVersion().equals(VERSION)) {
            return new DBAdminTaskResult(false, "Version mismatch.");
        }
        final TermId term = task.getArg("term", TermId.class);
        final boolean onlyEmpty = task.getArg("if-target-empty", Boolean.class);
        if (!onlyEmpty) {
            return new DBAdminTaskResult(false, "Not supported.");
        }
        final boolean dryRun = task.getArg("dry-run", Boolean.class);
        //TODO support effective Date
        final Date effective = null;
        final String msg = em.createQuery(QUERY, TermGradeTargetAssessmentEntity.class)
                .setLockMode(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
                .setParameter("term", new EmbeddableTermId(term))
                .getResultStream()
                .flatMap(e -> checkTarget(e, term, effective, dryRun))
                .collect(Collectors.joining("\n"));
        return new DBAdminTaskResult(true, msg);
    }

    private Stream<String> checkTarget(final TermGradeTargetAssessmentEntity e, final TermId term, final Date effective, final boolean dryRun) {
        final boolean emptyInTerm = e.getEntries().stream()
                .noneMatch(e2 -> term.equals(e2.getGradeId()));
        final boolean hasSignees = !e.getSignees().isEmpty();
        if (emptyInTerm && hasSignees) {
            return removeSignees(e, effective, dryRun);
        }
        LOGGER.log(Level.WARNING, "False result {0}", e.getDocumentId().toString());
        return Stream.empty();
    }

    private Stream<String> removeSignees(final TermGradeTargetAssessmentEntity e, final Date effective, final boolean dryRun) {
        final String[] types = e.getSignees().keySet().stream().toArray(String[]::new);
        final Stream.Builder<String> builder = Stream.builder();
        for (final String type : types) {
            final Signee s = e.getSignees().getOrDefault(type, Signee.NULL);
            final String msg = type + ";" + s.getId() + ";" + e.getDocumentId().toString();
            builder.add(msg);
            if (!dryRun) {
                final SigneeEntity prev = e.addSigneeInfo(null, type);
                if (prev != null) {
                    final SigneeInfoChangeLog log = new SigneeInfoChangeLog(e, BaseTargetAssessmentEntity.BASE_TARGETASSESSMENT_DOCUMENT_SIGNEEINFO, type, prev, BaseChangeLog.Action.REMOVE);
                    ChangeLogs.addLogWithVersionChangeLog(e, Collections.singletonList(log), effective, 3);
                    em.merge(e);
                    documentsNotificator.notityConsumers(new AbstractDocumentEvent(e.getDocumentId(), AbstractDocumentEvent.DocumentEventType.CHANGE, null));
                } else {
                    LOGGER.log(Level.WARNING, "Previous was null for {0}/{1}", new String[]{e.getDocumentId().toString(), type});
                }
            }
        }
        return builder.build();
    }

}
