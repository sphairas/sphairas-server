/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.unitadmin;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TemporalType;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.services.jms.AbstractDocumentEvent;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Documents;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.Template;
import org.thespheres.betula.document.util.DateTimeUtil;
import org.thespheres.betula.document.util.MarkerAdapter;
import org.thespheres.betula.entities.BaseChangeLog;
import org.thespheres.betula.entities.BaseIdentityMarkerMapDocumentEntity;
import org.thespheres.betula.entities.EmbeddableStudentId;
import org.thespheres.betula.entities.EmbeddableTermId;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.StudentMarkerMapChangeLog;
import org.thespheres.betula.entities.StudentMarkerMapDocumentEntity;
import org.thespheres.betula.entities.VersionChangeLog;
import org.thespheres.betula.entities.jmsimpl.DocumentsNotificator;
import org.thespheres.betula.entities.service.ServiceUtils;
import org.thespheres.betula.server.beans.StudentsListsLocalBean;
import org.thespheres.betula.server.beans.annot.Current;
import org.thespheres.betula.services.scheme.spi.Term;
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
public class AdminStudentsProcessor extends AbstractAdminContainerProcessor {

    static final String VALID_STUDENTS_QUERY = "SELECT DISTINCT NEW org.thespheres.betula.StudentId(s.studentAuthority, s.studentId) FROM UnitDocumentEntity ude, IN(ude.studentIds) s, IN(ude.markerSet) m "
            + "WHERE m.convention='betula-db' AND m.markerId='primary-unit' AND m.subset=NULL "
            + "AND e.expirationTime > :dateTime";
    static final String ORPHANED_TERM_GRADE_ENTRIES = "SELECT DISTINCT ude FROM UnitDocumentEntity ude, IN(ude.studentIds) e "
            + "WHERE e.studentId IN :studentIds "
            + "AND e.studentAuthority=:authority";
    @EJB
    private StudentsListsLocalBean sllb;
    @PersistenceContext(unitName = "betula0")
    private EntityManager em;
    @Inject
    protected DocumentsNotificator documentsNotificator;
    @Current
    @Inject
    private Term currentTerm;

    public AdminStudentsProcessor() {
        super(new String[][]{Paths.STUDENTS_MARKERS_PATH,
            Paths.STUDENTS_SIGNEES_PATH,
            Paths.STUDENTS_PATH});
    }

    @TransactionAttribute(value = TransactionAttributeType.MANDATORY)
    @Override
    public void process(final String[] path, final Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException {
        if (Arrays.equals(path, Paths.STUDENTS_MARKERS_PATH)) {
            final Entry<DocumentId, ?> entry = ServiceUtils.toEntry(template, DocumentId.class);
            processStudentsMarkers(entry);
        } else if (Arrays.equals(path, Paths.STUDENTS_SIGNEES_PATH)) {
            final Entry<StudentId, ?> entry = ServiceUtils.toEntry(template, StudentId.class);
            processStudentsSignees(entry);
        } else if (Arrays.equals(path, Paths.STUDENTS_PATH)) {
            processStudents(template);
        }
    }

    protected void processStudentsSignees(Entry<StudentId, ?> entry) {
        final StudentId sid = entry.getIdentity();
        if (Objects.equals(entry.getAction(), Action.REQUEST_COMPLETION) && entry.getChildren().isEmpty()) {
            entry.setAction(Action.RETURN_COMPLETION);
            final Signee[] sig = getSignees(sid, null);
            Arrays.stream(sig)
                    .map(s -> new Entry<>(null, s))
                    .forEach(entry.getChildren()::add);
        } else if (entry.getAction() == null) {
            entry.getChildren().stream()
                    .filter(t -> t.getValue() instanceof String)
                    .filter(t -> Objects.equals(t.getAction(), Action.REQUEST_COMPLETION))
                    .forEachOrdered(t -> {
                        String ent = (String) t.getValue();
                        t.setAction(Action.RETURN_COMPLETION);
                        t.getChildren().clear();
                        final Signee[] sig = getSignees(sid, ent);
                        Arrays.stream(sig)
                                .map(s -> new Entry<>(null, s))
                                .forEach(t.getChildren()::add);
                    });
        }
    }

    private Signee[] getSignees(final StudentId studentId, final String entitlement) {
        final TermId term = currentTerm.getScheduledItemId();
        final EmbeddableTermId et = new EmbeddableTermId(term);
        if (entitlement != null) {
            return em.createNamedQuery("findTargetSigneesForSelectedStudents", SigneeEntity.class)
                    //                .setLockMode(LockModeType.OPTIMISTIC)   //signee has no version column
                    .setParameter("entitlement", entitlement)
                    .setParameter("student", new EmbeddableStudentId(studentId))
                    .setParameter("term", et)
                    .getResultList().stream()
                    .map(SigneeEntity::getSignee)
                    .distinct()
                    .toArray(Signee[]::new);
        } else {
            return em.createNamedQuery("findAllTargetSigneesForSelectedStudents", SigneeEntity.class)
                    //                .setLockMode(LockModeType.OPTIMISTIC)   //signee has no version column
                    .setParameter("student", new EmbeddableStudentId(studentId))
                    .setParameter("term", et)
                    .getResultList().stream()
                    .map(SigneeEntity::getSignee)
                    .distinct()
                    .toArray(Signee[]::new);
        }

    }

    protected void processStudentsMarkers(final Entry<DocumentId, ?> entry) throws SyntaxException {
        if (!entry.getChildren().isEmpty()) {
            StudentMarkerMapDocumentEntity mapEntity = em.find(StudentMarkerMapDocumentEntity.class, entry.getIdentity());
//            Template ret = builder.createTemplate(null, docId, null, STUDENTS_MARKERS_PATH, null, null);
            StudentMarkerMapChangeLog log = null;
            for (Template<?> i : entry.getChildren()) {
                final Entry<StudentId, MarkerAdapter> e = ServiceUtils.toEntry(i, StudentId.class, MarkerAdapter.class);
                final StudentId student = e.getIdentity();
                if (i.getAction().equals(Action.FILE)) {
                    final DocumentId.Version ov = mapEntity.getCurrentVersion();
                    final MarkerAdapter ma = e.getValue();
                    final Marker m = ma != null ? ma.getMarker() : null;
//                    final Timestamp time = e.getTimestamp();
                    final Date ts = e.getTimestamp() != null ? e.getTimestamp().getDate() : null;
                    if ((log = changeLog(mapEntity, student, m, ts)) != null) {
                        if (ov != null) {
                            DocumentId.Version nv = Documents.inc(ov, 2);
                            mapEntity.setCurrentVersion(nv);
                            //TODO: check chronologically!, user ChangeLogs!!!!
                            mapEntity.addChangeLog(new VersionChangeLog(mapEntity, ov, ts));
                            mapEntity.addChangeLog(log);
                            documentsNotificator.notityConsumers(new AbstractDocumentEvent(mapEntity.getDocumentId(), AbstractDocumentEvent.DocumentEventType.CHANGE, login.getSigneePrincipal(false)));//ZGN
                        }
                    }
                } else if (i.getAction().equals(Action.REQUEST_COMPLETION)) {
                    e.setAction(Action.RETURN_COMPLETION);
                    final Marker v = mapEntity.get(student);
                    final MarkerAdapter ma = v == null ? null : new MarkerAdapter(v);
                    e.setValue(ma);
                }
            }
            if (log != null) {
                em.merge(mapEntity);
            }
        }
    }

    private StudentMarkerMapChangeLog changeLog(final StudentMarkerMapDocumentEntity map, final StudentId student, final Marker marker, final Date asOf) {
        //TODO if time != null: Find correct previous entry at time, 
        final Marker prevAtDate = sllb.getMarkerEntry(student, map.getDocumentId(), asOf);
        final Marker prev = map.put(student, marker);
        if (Objects.equals(marker, prev)) {
            return null;
        }
        if (asOf != null) {
            map.applyRestoreVersion(asOf, bcl -> applyRemoveLog(bcl, student));
        }
        BaseChangeLog.Action a;
        if (prev == null) {
            a = BaseChangeLog.Action.ADD;
        } else if (Marker.isNull(marker)) {
            a = BaseChangeLog.Action.REMOVE;
        } else {
            a = BaseChangeLog.Action.UPDATE;
        }
        return new StudentMarkerMapChangeLog(map, student, prev, a); // prevAtDate
    }

    private void applyRemoveLog(final BaseChangeLog bcl, final StudentId student) {
        if (bcl instanceof StudentMarkerMapChangeLog
                && bcl.getProperty().equals(BaseIdentityMarkerMapDocumentEntity.BASE_IDENTITY_MARKER_MAP_DOCUMENT_VALUES)) {
            final StudentMarkerMapChangeLog log = (StudentMarkerMapChangeLog) bcl;
            if (student.equals(log.getStudent())) {
                bcl.setIgnore(true);
//                switch (log.getAction()) {
//                    case REMOVE:
//                        restored = log.getValue();
//                        break;
//                    case ADD:
//                        restored = null;
//                        break;
//                    case UPDATE:
//                        restored = log.getValue();
//                        break;
//                }
            }
        }
    }

    private void processStudents(final Envelope node) throws SyntaxException {
        final Action a = node.getAction();
        if (a != null && a.equals(Action.REQUEST_COMPLETION)) {
            final String get = node.getHints().get("if-expired-before");
            java.sql.Timestamp expringBefore = null;
            if (get != null) {
                try {
                    final ZonedDateTime zdt = ZonedDateTime.parse(get, DateTimeUtil.ZONED_DATE_TIME_FORMAT);
                    expringBefore = java.sql.Timestamp.from(zdt.toInstant());
                } catch (DateTimeParseException e) {
                    Logger.getLogger(AdminStudentsProcessor.class.getName()).log(Level.SEVERE, "An exception was thrown in processDocuments", e);
                    throw ServiceUtils.createSyntaxException(e);
                }
            }
//            findValidStudents();
            node.getChildren().clear();
            node.setAction(Action.RETURN_COMPLETION);
        }
    }

    protected Set<StudentId> findValidStudents(final Instant time) {
        return em.createQuery(VALID_STUDENTS_QUERY, StudentId.class)
                .setLockMode(LockModeType.OPTIMISTIC)
                .setParameter("dateTime", new java.sql.Timestamp(time.toEpochMilli()), TemporalType.TIMESTAMP)
                .getResultList().stream()
                .collect(Collectors.toSet());
    }

    private void processOrphanedStudentsDocuments(final Envelope node) throws SyntaxException {
        final Action a = node.getAction();
        if (a != null && a.equals(Action.REQUEST_COMPLETION)) {
            final String get = node.getHints().get("if-expired-before");
            final Instant time;
            if (get != null) {
                try {
                    final ZonedDateTime zdt = ZonedDateTime.parse(get, DateTimeUtil.ZONED_DATE_TIME_FORMAT);
                    time = zdt.toInstant();
                } catch (DateTimeParseException e) {
                    Logger.getLogger(AdminStudentsProcessor.class.getName()).log(Level.SEVERE, "An exception was thrown in processDocuments", e);
                    throw ServiceUtils.createSyntaxException(e);
                }
            } else {
                time = Instant.now();
            }

//            final CriteriaBuilder cb = em.getCriteriaBuilder();
//            final CriteriaQuery<TermGradeTargetAssessmentEntity> cq = cb.createQuery(TermGradeTargetAssessmentEntity.class);
//            final Root<TermGradeTargetAssessmentEntity> root = cq.from(TermGradeTargetAssessmentEntity.class);
//
//            final List<Predicate> list = new ArrayList<>();
//            CriteriaBuilder.In<Object> in = cb.in(root.get("entries"));
//            in.value(a)
//            list.add(cb.equal(root.join("markerSet").get("convention"), "betula-db"));
//            list.add(cb.equal(root.join("markerSet").get("markerId"), "primary-unit"));
//            list.add(cb.isNull(root.join("markerSet").get("subset")));
//
//            if (expringBefore != null) {
////            ParameterExpression<Date> param = cb.parameter(Date.class, "dateLimit");
//                list.add(cb.lessThanOrEqualTo(root.<java.sql.Timestamp>get("expirationTime"), expringBefore));
//            }
//
//            cq.select(root).distinct(true).where(list.stream().toArray(Predicate[]::new));
//            return em.createQuery(cq).setLockMode(LockModeType.OPTIMISTIC).getResultList();

//            findValidStudents();
            node.getChildren().clear();
            node.setAction(Action.RETURN_COMPLETION);
        }
    }
}
