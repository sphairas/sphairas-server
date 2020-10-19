/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.dbadmin;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.database.DBAdminTask;
import org.thespheres.betula.database.DBAdminTaskResult;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.entities.TermAssessmentEntry2;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;

/**
 *
 * @author boris.heithecker
 */
@Stateless
public class CleanUpTask {

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public static final String NAME = "clean-up";
    public static final String VERSION = "1.0";
    @PersistenceContext(unitName = "betula0")
    private EntityManager em;
    private final static Logger LOGGER = Logger.getLogger("dbadmin-clean-up-task");

    public String getName() {
        return NAME;
    }

    public String getVersion() {
        return VERSION;
    }

    @Messages({"CleanUpTask.success.message={1} entries have been removed from {0} document and {2} terms.",
        "CleanUpTask.log.removed=Removed {1} entries from document {0}.",
        "CleanUpTask.log.removed.entry=Removed from document {0} entry for student {1} and term {2} with value {3}."})
//    @RolesAllowed({"superadmin"})
    public DBAdminTaskResult process(final DBAdminTask task) {
        if (!task.getVersion().equals(VERSION)) {
            //Ex
        }
        final String targetType = task.getArg("target-type", String.class);
        final List<TermId> term = task.getArgs("term", TermId.class);
        final int maxDocs = task.getArg("max-documents", Integer.class, Integer.MAX_VALUE);
        final int maxEntries = task.getArg("max-entries", Integer.class, Integer.MAX_VALUE);
        int[] entriesRemoved = new int[]{0};
        final List<Removed> rem = new ArrayList<>();

        if (targetType != null && !term.isEmpty()) {
            final CriteriaBuilder cb = em.getCriteriaBuilder();
            final CriteriaQuery<TermGradeTargetAssessmentEntity> cq = cb.createQuery(TermGradeTargetAssessmentEntity.class);
            final Root<TermGradeTargetAssessmentEntity> pet = cq.from(TermGradeTargetAssessmentEntity.class);

            final List<Predicate> list = new ArrayList<>();
//        list.add(cb.equal(pet.join("markerSet").get("convention"), "betula-db"));
//        list.add(cb.equal(pet.join("markerSet").get("markerId"), "primary-unit"));
//        list.add(cb.isNull(pet.join("markerSet").get("subset")));
            list.add(cb.equal(pet.get("targetType"), targetType));

            final CriteriaQuery<TermGradeTargetAssessmentEntity> where = cq.select(pet).distinct(true).where(list.stream().toArray(Predicate[]::new));
            final List<TermGradeTargetAssessmentEntity> l = em.createQuery(where).setLockMode(LockModeType.OPTIMISTIC_FORCE_INCREMENT).getResultList();

            for (TermGradeTargetAssessmentEntity tgtae : l) {
                if (rem.size() >= maxDocs || entriesRemoved[0] >= maxEntries) {
                    break;
                }
                final Removed r = new Removed(tgtae.getDocumentId());
                final Iterator<TermAssessmentEntry2> it = tgtae.getEntries().iterator();
                entries:
                while (it.hasNext()) {
                    final TermAssessmentEntry2 next = it.next();
                    for (TermId t : term) {
                        if (Objects.equals(next.getGradeId(), t)) {
                            final Entry e = new Entry(next.getGrade(), next.getTimestamp(), next.getGradeId());
                            r.entriesRemoved.add(e);
                            it.remove();
                            final String msg = NbBundle.getMessage(CleanUpTask.class, "CleanUpTask.log.removed.entry", r.document, next.getStudentId(), e.term, e.grade);
                            LOGGER.log(Level.FINE, msg);
                            if (++entriesRemoved[0] >= maxEntries) {
                                break entries;
                            }
                        }
                    }
                }
                if (!r.entriesRemoved.isEmpty()) {
                    rem.add(r);
                }
                em.merge(tgtae);
                final String msg = NbBundle.getMessage(CleanUpTask.class, "CleanUpTask.log.removed", r.document, r.entriesRemoved.size());
                LOGGER.log(Level.INFO, msg);
            }
        }
        final int docs = rem.size();
        final long terms = rem.stream()
                .flatMap(r -> r.entriesRemoved.stream())
                .map(e -> e.term)
                .distinct()
                .count();
        final String msg = NbBundle.getMessage(CleanUpTask.class, "CleanUpTask.success.message", docs, entriesRemoved[0], terms);
        LOGGER.log(Level.INFO, msg);
        return new DBAdminTaskResult(true, msg);
    }

    private class Removed {

        private final DocumentId document;
        private final List<Entry> entriesRemoved = new ArrayList<>();

        Removed(DocumentId document) {
            this.document = document;
        }

    }

    class Entry {

        private final Grade grade;
        private final Timestamp time;
        private final TermId term;

        Entry(Grade grade, Timestamp time, TermId term) {
            this.grade = grade;
            this.time = time;
            this.term = term;
        }

    }
}
