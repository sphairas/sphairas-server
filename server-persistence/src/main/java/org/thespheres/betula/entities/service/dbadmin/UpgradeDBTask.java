/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.dbadmin;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.thespheres.betula.database.DBAdminTask;
import org.thespheres.betula.database.DBAdminTaskResult;
import org.thespheres.betula.entities.TermAssessmentEntry;
import org.thespheres.betula.entities.TermAssessmentEntry2;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.TermTextAssessmentEntry;
import org.thespheres.betula.entities.TermTextAssessmentEntry2;
import org.thespheres.betula.entities.TermTextTargetAssessmentEntity;

/**
 *
 * @author boris.heithecker
 */
@Deprecated
@Stateless
public class UpgradeDBTask {

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public static final String NAME = "upgrade-database";
    public static final String VERSION = "1.0";
    @PersistenceContext(unitName = "betula0")
    private EntityManager em;
    private final static Logger LOGGER = Logger.getLogger("dbadmin-upgrade-database-task");

    public String getName() {
        return NAME;
    }

    public String getVersion() {
        return VERSION;
    }

    @Messages({"UpgradeDBTask.success.message={1} entries have been copied from {0} documents.",
        "UpgradeDBTask.log.removed=Removed {1} entries from document {0}.",
        "UpgradeDBTask.log.removed.entry=Removed from document {0} entry for student {1} and term {2} with value {3}."})
//    @RolesAllowed({"superadmin"})
    public DBAdminTaskResult process(final DBAdminTask task) {
        if (!task.getVersion().equals(VERSION)) {
            //Ex
        }

//        final String msg = termGrades();
//        final String msg2 = termTexts();
//        return new DBAdminTaskResult(msg + "\n" + msg2);
        return new DBAdminTaskResult("Don't use it!");
    }

    private String termGrades() {
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<TermGradeTargetAssessmentEntity> cq = cb.createQuery(TermGradeTargetAssessmentEntity.class);
        final Root<TermGradeTargetAssessmentEntity> pet = cq.from(TermGradeTargetAssessmentEntity.class);
        cq.select(pet);
        final List<TermGradeTargetAssessmentEntity> l = em.createQuery(cq).setLockMode(LockModeType.OPTIMISTIC).getResultList();
        int docs = 0;
        int entries = 0;
        docs:
        for (final TermGradeTargetAssessmentEntity tgtae : l) {
            if (!tgtae.getEntries().isEmpty()) {
                for (final Object eo : tgtae.getEntries()) {
                    if (eo instanceof TermAssessmentEntry) {
                        final TermAssessmentEntry e = (TermAssessmentEntry) eo;
                        final TermAssessmentEntry2 ne = copy(tgtae, e);
                        em.persist(ne);
                        ++entries;
                    }
                }
                ++docs;
            }
        }
        final String msg = NbBundle.getMessage(UpgradeDBTask.class, "UpgradeDBTask.success.message", entries, docs);
        LOGGER.log(Level.INFO, msg);
        return msg;
    }

    private String termTexts() {
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<TermTextTargetAssessmentEntity> cq = cb.createQuery(TermTextTargetAssessmentEntity.class);
        final Root<TermTextTargetAssessmentEntity> pet = cq.from(TermTextTargetAssessmentEntity.class);
        cq.select(pet);
        final List<TermTextTargetAssessmentEntity> l = em.createQuery(cq).setLockMode(LockModeType.OPTIMISTIC).getResultList();
        int docs = 0;
        int entries = 0;
        docs:
        for (final TermTextTargetAssessmentEntity tgtae : l) {
            if (!tgtae.getEntries().isEmpty()) {
                for (final Object eo : tgtae.getEntries()) {
                    if (eo instanceof TermTextAssessmentEntry) {
                        final TermTextAssessmentEntry e = (TermTextAssessmentEntry) eo;
                        final TermTextAssessmentEntry2 ne = copyText(tgtae, e);
                        em.persist(ne);
                        ++entries;
                    }
                }
                ++docs;
            }
        }
        final String msg = NbBundle.getMessage(UpgradeDBTask.class, "UpgradeDBTask.success.message", entries, docs);
        LOGGER.log(Level.INFO, msg);
        return msg;
    }

    private TermAssessmentEntry2 copy(TermGradeTargetAssessmentEntity tgtae, TermAssessmentEntry e) {
        final TermAssessmentEntry2 ret = new TermAssessmentEntry2(tgtae, e.getStudentId());
        ret.setGradeId(e.getGradeId());
        ret.setGrade(e.getGrade(), e.getTimestamp());
        return ret;
    }

    private TermTextAssessmentEntry2 copyText(TermTextTargetAssessmentEntity tgtae, TermTextAssessmentEntry e) {
        final TermTextAssessmentEntry2 ret = new TermTextAssessmentEntry2(tgtae, e.getStudentId(), e.getTermId(), e.getSection());
        ret.setText(e.getText(), e.getTimestamp());
        return ret;
    }
}
