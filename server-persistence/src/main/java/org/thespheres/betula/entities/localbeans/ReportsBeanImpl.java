/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.localbeans;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceContext;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.services.jms.AbstractDocumentEvent;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.entities.EmbeddableGrade;
import org.thespheres.betula.entities.EmbeddableStudentId;
import org.thespheres.betula.entities.EmbeddableTermId;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.TermReportDocumentEntity;
import org.thespheres.betula.entities.UnitDocumentEntity;
import org.thespheres.betula.entities.facade.SigneeFacade;
import org.thespheres.betula.entities.facade.UnitDocumentFacade;
import org.thespheres.betula.entities.jmsimpl.DocumentsNotificator;
import org.thespheres.betula.niedersachsen.ASVAssessmentConvention;
import org.thespheres.betula.server.beans.ReportsBean;
import org.thespheres.betula.server.beans.TermReportDataException;
import org.thespheres.betula.server.beans.annot.Arbeitsgemeinschaft;
import org.thespheres.betula.server.beans.annot.Authority;
import org.thespheres.betula.services.ws.CommonDocuments;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class ReportsBeanImpl implements ReportsBean {

    @PersistenceContext(unitName = "betula0")
    private EntityManager em;
    @EJB
    private SigneeFacade login;
    @Arbeitsgemeinschaft
    @Inject
    private Marker agMarker;
    @Arbeitsgemeinschaft
    @Inject
    private Grade agTeilnahme;
    @EJB
    private UnitDocumentFacade unitFacade;
    @Inject
    protected DocumentsNotificator documentsNotificator;
    @Inject
    private CommonDocuments cd;
    @Authority
    @Inject
    private String authority;
    final static MessageFormat AG_QUERY_FORMAT = new MessageFormat("SELECT DISTINCT t FROM TermGradeTargetAssessmentEntity t, IN(t.entries) e, IN(t.markerSet) m "
            + "WHERE m.convention='{0}' AND m.markerId='{1}' AND m.subset={2} "
            + "AND e.grade.gradeConvention='{3}' AND e.grade.gradeId='{4}' "
            + "AND e.term=:term "
            + "AND e.student=:student");

    @Override
    public DocumentId[] findTermReports(StudentId student, TermId term, boolean create) {
        final List<TermReportDocumentEntity> l = em.createNamedQuery("findTermReport", TermReportDocumentEntity.class)
                //                .setParameter("studentAuthority", student.getAuthority())
                //                .setParameter("studentId", student.getId())
                //                .setParameter("termAuthority", term.getAuthority())
                //                .setParameter("termId", term.getId())
                .setParameter("term", new EmbeddableTermId(term))
                .setParameter("student", new EmbeddableStudentId(student))
                .setLockMode(LockModeType.OPTIMISTIC)
                .getResultList();
        DocumentId[] ret;
        if (l.isEmpty() && create) {
            String idBase = "kgs-zeugnis-" + Long.toString(student.getId()) + "-" + Integer.toString(term.getId());
            final SigneeEntity creator = login.getCurrent();
            DocumentId zeugnisId = new DocumentId(authority, idBase, DocumentId.Version.LATEST);
            int idc = 1;
            while (em.find(TermReportDocumentEntity.class, zeugnisId, LockModeType.OPTIMISTIC) != null) {
                String id = idBase + "-" + Integer.toString(idc++);
                zeugnisId = new DocumentId(authority, id, DocumentId.Version.LATEST);
            }
            TermReportDocumentEntity e = new TermReportDocumentEntity(zeugnisId, student, term, creator);
            em.persist(e);
            ret = new DocumentId[]{e.getDocumentId()};
        } else {
            ret = new DocumentId[l.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = l.get(i).getDocumentId();
            }
        }
        return ret;
    }

    private TermReportDocumentEntity findReportEntity(DocumentId id, LockModeType lmt) {
        return em.find(TermReportDocumentEntity.class, id, lmt);
    }

    @Override
    public StudentId getStudent(DocumentId zgnId) {
        TermReportDocumentEntity e = findReportEntity(zgnId, LockModeType.OPTIMISTIC);
        if (e != null) {
            return e.getStudent();
        }
        return null;
    }

    @Override
    public TermId getTerm(DocumentId zgnId) {
        TermReportDocumentEntity e = findReportEntity(zgnId, LockModeType.OPTIMISTIC);
        if (e != null) {
            return e.getTerm();
        }
        return null;
    }

    @Override
    public Grade getKopfnote(DocumentId zeugnis, String convention) {
        final TermReportDocumentEntity e = findReportEntity(zeugnis, LockModeType.OPTIMISTIC);
        if (e != null) {
            switch (convention) {
                case ASVAssessmentConvention.AV_NAME:
                    return e.getAvnote() != null ? e.getAvnote().findGrade() : null;
                case ASVAssessmentConvention.SV_NAME:
                    return e.getSvnote() != null ? e.getSvnote().findGrade() : null;
            }
        }
        return null;
    }

    @Override
    public boolean setKopfnote(DocumentId zeugnis, String convention, Grade grade) throws TermReportDataException {
        TermReportDocumentEntity e = findReportEntity(zeugnis, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (e != null && convention != null) {
            switch (convention) {
                case ASVAssessmentConvention.AV_NAME:
                    e.setAvnote(grade != null ? new EmbeddableGrade(grade) : null);
                    break;
                case ASVAssessmentConvention.SV_NAME:
                    e.setSvnote(grade != null ? new EmbeddableGrade(grade) : null);
                    break;
                default:
                    return false;
            }
            em.merge(e);
            final SigneeEntity se = login.getCurrent();
            final AbstractDocumentEvent evt = new AbstractDocumentEvent(e.getDocumentId(), AbstractDocumentEvent.DocumentEventType.CHANGE, se == null ? null : se.getSignee());
            documentsNotificator.notityConsumers(evt);
            return true;
        }
        return false;
    }

    @Override
    public Integer getIntegerValue(DocumentId zeugnis, String type) {
        final TermReportDocumentEntity e = findReportEntity(zeugnis, LockModeType.OPTIMISTIC);
        if (e != null) {
            switch (type) {
                case ReportsBean.TYPE_FEHLTAGE:
                    return e.getFehltage();
                case ReportsBean.TYPE_UENTSCHULDIGT:
                    return e.getUnentschuldigt();
            }
        }
        return 0;
    }

    @Override
    public boolean setIntegerValue(DocumentId zeugnis, String type, Integer value) throws TermReportDataException {
        TermReportDocumentEntity e = findReportEntity(zeugnis, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (e != null && type != null) {
            try {
                switch (type) {
                    case ReportsBean.TYPE_FEHLTAGE:
                        e.setFehltage(value);
                        break;
                    case ReportsBean.TYPE_UENTSCHULDIGT:
                        e.setUnentschuldigt(value);
                        break;
                    default:
                        return false;
                }
                em.merge(e);
            } catch (OptimisticLockException ex) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Marker[] getMarkers(DocumentId zeugnis) {
        final TermReportDocumentEntity e = findReportEntity(zeugnis, LockModeType.OPTIMISTIC);
        return e != null ? e.markers() : null;
    }

    @Override
    public boolean addMarker(DocumentId zeugnis, Marker m) {
        final TermReportDocumentEntity e = findReportEntity(zeugnis, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (e != null && m != null) {
            e.addMarker(m);
            em.merge(e);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeMarker(DocumentId zeugnis, Marker marker) {
        TermReportDocumentEntity e = findReportEntity(zeugnis, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (e != null) {
            e.removeMarker(marker);
            em.merge(e);
            return true;
        }
        return false;
    }

    @Override
    public String[] getAGs(StudentId student, TermId term) {

        final String query = AG_QUERY_FORMAT.format(new Object[]{agMarker.getConvention(), agMarker.getId(), "NULL", agTeilnahme.getConvention(), agTeilnahme.getId()});
//        Logger.getLogger("ZeugnisTest").log(Level.INFO, "Enter: " + Long.toString(student.getId()));
        //TODO use configurable AG marker and teilnahme
        final List<TermGradeTargetAssessmentEntity> l;

        if (agMarker != null || agMarker.getSubset() == null) {
            //TODO: query api for alternative ODER: subset=null ->> "" empty string in db, default value.... for column
//            throw new IllegalArgumentException("This query can handle only cases where Marker.subset is null.");

            final List<TermGradeTargetAssessmentEntity> l2 = em.createNamedQuery("findTermGradeTargetAssessmentsForSubjectMarkerWithNullSubsetOnly", TermGradeTargetAssessmentEntity.class)
                    .setParameter("student", new EmbeddableStudentId(student))
                    .setParameter("term", new EmbeddableTermId(term))
                    .setParameter("markerConvention", agMarker.getConvention())
                    .setParameter("markerId", agMarker.getId())
                    //                .setParameter("markerSubset", fach.getSubset())
                    .setLockMode(LockModeType.OPTIMISTIC)
                    .getResultList();
            l = l2.stream()
                    .filter(e -> e.findAssessmentEntry(student, term).getEmbeddableGrade().getConvention().equals(agTeilnahme.getConvention())
                    && e.findAssessmentEntry(student, term).getEmbeddableGrade().getId().equals(agTeilnahme.getId()))
                    .collect(Collectors.toList());
        } else {
            l = em.createQuery(query, TermGradeTargetAssessmentEntity.class)
                    .setParameter("term", new EmbeddableTermId(term))
                    .setParameter("student", new EmbeddableStudentId(student))
                    .setLockMode(LockModeType.OPTIMISTIC)
                    .getResultList();

        }

//        final List<TermGradeTargetAssessmentEntity> l = em.createNamedQuery("findStudentAGs", TermGradeTargetAssessmentEntity.class)
//                //        nq.setParameter("termAuthority", term.getAuthority())
//                //                .setParameter("termId", term.getId())
//                //                .setParameter("studentAuthority", student.getAuthority())
//                //                .setParameter("studentId", student.getId());
//                .setParameter("term", new EmbeddableTermId(term))
//                .setParameter("student", new EmbeddableStudentId(student))
//                .setLockMode(LockModeType.OPTIMISTIC)
//                .getResultList();
//Alternative : user TermGradeTargetAssessmentEntity.findAllForSubjectMarkerWithNullSubsetOnly
//Filter manually
        final DocumentId cNames = cd.forName(CommonDocuments.COMMON_NAMES_DOCID);
        return l.stream().map(e -> {
            final UnitDocumentEntity ue = e.getUnitDocs().stream().findAny().get();
            return cNames != null ? unitFacade.getCommonName(cNames, ue.getUnitId()) : ue.getUnitId().getId();
        }).toArray(String[]::new);
    }

    @Override
    public CustomNote[] getCustomNotes(DocumentId zeugnis) {
        TermReportDocumentEntity e = findReportEntity(zeugnis, LockModeType.OPTIMISTIC);
        if (e != null) {
            Map<Integer, String> notes = e.getFreeNotes();
            return notes.entrySet().stream()
                    .map(me -> new CustomNote(me.getKey(), me.getValue()))
                    .toArray(CustomNote[]::new);
        }
        return null;
    }

    @Override
    public void setCustomNotes(DocumentId zeugnis, CustomNote[] cn) {
        TermReportDocumentEntity e = findReportEntity(zeugnis, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (e != null) {
            final Map<Integer, String> notes = e.getFreeNotes();
            Arrays.stream(cn)
                    .filter(n -> n.getValue() != null)
                    .forEach(n -> notes.put(n.getPosition(), n.getValue()));
            Arrays.stream(cn)
                    .filter(n -> n.getValue() == null)
                    .forEach(n -> notes.remove(n.getPosition()));
        }
    }

    @Override
    public String getNote(final DocumentId zgn, final String key) {
        final TermReportDocumentEntity e = findReportEntity(zgn, LockModeType.OPTIMISTIC);
        if (e != null) {
            return e.getNotes().get(key);
        }
        return null;
    }

    @Override
    public void setNote(DocumentId zgn, String key, String value) {
        final TermReportDocumentEntity e = findReportEntity(zgn, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (e != null && key != null) {
            if (value != null) {
                e.getNotes().put(key, value);
            } else {
                e.getNotes().remove(key);
            }
        }
    }

}
