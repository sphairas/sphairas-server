/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.unitadmin;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import org.thespheres.betula.entities.service.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.document.DocumentEntry;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Documents;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.MarkerFactory;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.Template;
import org.thespheres.betula.document.util.DateTimeUtil;
import org.thespheres.betula.document.util.DocumentUtilities;
import org.thespheres.betula.document.util.GenericXmlDocument;
import org.thespheres.betula.document.util.MarkerAdapter;
import org.thespheres.betula.document.util.UnitEntry;
import org.thespheres.betula.document.util.XmlMarkerSet;
import org.thespheres.betula.entities.BaseChangeLog;
import org.thespheres.betula.entities.BaseTargetAssessmentEntity;
import org.thespheres.betula.entities.StudentIdCollectionChangeLog;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.UnitDocumentEntity;
import org.thespheres.betula.entities.VersionChangeLog;
import org.thespheres.betula.entities.config.IllegalServiceArgumentException;
import org.thespheres.betula.entities.facade.GradeTargetDocumentFacade;
import org.thespheres.betula.entities.facade.UnitDocumentFacade;
import org.thespheres.betula.services.ws.CommonDocuments;
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
public class AdminUnitsProcessor extends AbstractAdminContainerProcessor {

    private final String[] PU_PARTICIPANTS_PATH = new String[]{"primary-units", "participants"};
    @EJB
    private UnitDocumentFacade udef;
    @EJB
    private GradeTargetDocumentFacade targets;
    @Inject
    private CommonDocuments cd;
//    @Inject
//    private DocumentsNotificator documentsNotificator;

    public AdminUnitsProcessor() {
        super(new String[][]{Paths.UNITS_PATH,
            Paths.PRIMARYUNITS_PATH,
            Paths.STUDENTS_UNITS_PARTICIPANTS_PATH,
            Paths.UNITS_PARTICIPANTS_PATH,
            Paths.TARGETS_PATH,
            Paths.PRIMARY_UNITS_SIGNEES_PATH,
            Paths.UNITS_TARGET_DOCUMENTS_PATH});
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    @Override
    public void process(String[] path, Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void process(Container container, String[] path, Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException {
        if (Arrays.equals(path, Paths.UNITS_PATH)) {
            processUnits(template, false);
        } else if (Arrays.equals(path, Paths.UNITS_PATH)) {
            processUnits(template, true);
        } else if (Arrays.equals(path, Paths.STUDENTS_UNITS_PARTICIPANTS_PATH)) {
            processStudentsUnits(template);
        } else if (Arrays.equals(path, Paths.UNITS_PARTICIPANTS_PATH)) {
//            if (ServiceUtils.isEntryOf(template, UnitId.class)) {
            final Entry<UnitId, ?> entryNode = ServiceUtils.toEntry(template, UnitId.class);
            processUnitsParticipants(container, entryNode);
//            } else {
//                
//            }
        } else if (Arrays.equals(path, Paths.TARGETS_PATH)) {
            processTargets(template);
        } else if (Arrays.equals(path, Paths.PRIMARY_UNITS_SIGNEES_PATH)) {
            final DocumentEntry<?> entryNode = ServiceUtils.toEntryType(template, DocumentEntry.class);
            processPrimaryUnitsSignees(entryNode);
        } else if (Arrays.equals(path, Paths.UNITS_TARGET_DOCUMENTS_PATH)) {
            final UnitEntry ue = ServiceUtils.toEntryType(template, UnitEntry.class);
            processUnitTargets(ue);
        }
    }

    private void processUnitTargets(final UnitEntry ue) throws SyntaxException {
        final DocumentId unit = ue.getIdentity();
        if (DocumentId.isNull(unit)) {
            final String l = ue.getHints().getOrDefault("linked", "false");
            final boolean linked = Boolean.parseBoolean(l);
            final Set<StudentId> students = ue.getChildren().stream()
                    .filter(Entry.class::isInstance)
                    .map(Entry.class::cast)
                    .filter(e -> e.getIdentity() instanceof StudentId)
                    .map(e -> (StudentId) e.getIdentity())
                    .collect(Collectors.toSet());
            ue.getChildren().clear();
            ue.setAction(Action.RETURN_COMPLETION);
            students.stream()
                    .map(targets::findForStudent)
                    .flatMap(Collection::stream)
                    .map(BaseTargetAssessmentEntity::getDocumentId)
                    .map(did -> new Entry<>(null, did))
                    .forEach(ue.getChildren()::add);
            return;
        }
        final UnitDocumentEntity ude = udef.find(unit, LockModeType.OPTIMISTIC);
        final Action a = ue.getAction();
        if (ude != null && a != null && a.equals(Action.REQUEST_COMPLETION)) {
            ue.getChildren().clear();
            ue.setAction(Action.RETURN_COMPLETION);
            final String l = ue.getHints().getOrDefault("linked", "false");
            final boolean linked = Boolean.parseBoolean(l);
            if (linked) {
                ude.getTargetAssessments().stream()
                        .map(BaseTargetAssessmentEntity::getDocumentId)
                        .distinct()
                        .map(did -> new Entry<>(null, did))
                        .forEach(ue.getChildren()::add);
            } else {
                targets.findForUnitDocument(ude, null).stream()
                        .map(BaseTargetAssessmentEntity::getDocumentId)
                        .distinct()
                        .map(did -> new Entry<>(null, did))
                        .forEach(ue.getChildren()::add);
            }
        } else {
            for (final Template<?> ch : ue.getChildren()) {
                final TermId reference = ServiceUtils.toEntry(ch, TermId.class).getIdentity();
                final Action ac = ch.getAction();
                if (ude != null && ac != null && ac.equals(Action.REQUEST_COMPLETION)) {
                    final String dv = ch.getHints().get("version.asOf");
                    if (dv != null) {
                        ch.getChildren().clear();
                        ch.setAction(Action.RETURN_COMPLETION);
                        targets.findForStudents(adoptStudents(ude, dv), reference).stream()
                                .map(BaseTargetAssessmentEntity::getDocumentId)
                                .distinct()
                                .map(did -> new Entry<>(null, did))
                                .forEach(ch.getChildren()::add);
                    } else {
                        ch.getChildren().clear();
                        ch.setAction(Action.RETURN_COMPLETION);
                        targets.findForUnitDocument(ude, reference).stream()
                                .map(BaseTargetAssessmentEntity::getDocumentId)
                                .distinct()
                                .map(did -> new Entry<>(null, did))
                                .forEach(ch.getChildren()::add);
                    }
                }
            }
        }
    }

    private Set<StudentId> adoptStudents(final UnitDocumentEntity ude, final String dv) throws IllegalServiceArgumentException {
        final Set<StudentId> set = new HashSet<>(ude.getStudentIds());
        final Date date;
        try {
            final LocalDate ta = LocalDate.parse(dv, DateTimeUtil.DATEFORMAT);
            date = Date.from(Instant.from(ta.atStartOfDay().atZone(ZoneId.systemDefault())));
        } catch (DateTimeException ex) {
            throw new IllegalServiceArgumentException("Illegal hint.");
        }
        udef.adoptStudentsToVersionAsOf(ude, date, set);
        return set;
    }

    private void processTargets(final Envelope node) {
        final Action a = node.getAction();
        if (a != null && a.equals(Action.REQUEST_COMPLETION)) {
            final List<TermGradeTargetAssessmentEntity> l = targets.findAll(LockModeType.READ, TermGradeTargetAssessmentEntity.class);
            node.getChildren().clear();
            node.setAction(Action.RETURN_COMPLETION);
            l.stream().forEach(tgtae -> {
                final Entry<?, ?> te = new Entry(null, tgtae.getDocumentId());
                node.getChildren().add(te);
                tgtae.getUnitDocs().stream()
                        .map(ude -> new Entry(null, ude.getUnitId()))
                        .forEach(ue -> te.getChildren().add(ue));
            });
        }
    }

    //TODO: return unitdocuments?
    //TODO: decorator
    private void processUnits(final Envelope node, final boolean pus) throws SyntaxException {
        final Action a = node.getAction();
        if (a != null && a.equals(Action.REQUEST_COMPLETION)) {
            final boolean noValueEntry = Boolean.parseBoolean(node.getHints().get("request-completion.no-value"));
            final String get = node.getHints().get("if-expired-before");
            java.sql.Timestamp expringBefore = null;
            if (get != null) {
                try {
                    final ZonedDateTime zdt = ZonedDateTime.parse(get, DateTimeUtil.ZONED_DATE_TIME_FORMAT);
                    expringBefore = java.sql.Timestamp.from(zdt.toInstant());
                } catch (DateTimeParseException e) {
                    Logger.getLogger(AdminUnitsProcessor.class.getName()).log(Level.SEVERE, "An exception was thrown in processDocuments", e);
                    throw ServiceUtils.createSyntaxException(e);
                }
            }

            final List<UnitDocumentEntity> l;
            if (!pus) {
                l = udef.findAll(LockModeType.OPTIMISTIC);
            } else {
                l = udef.getAllPrimaryUnits(expringBefore);
            }
            node.getChildren().clear();
            node.setAction(Action.RETURN_COMPLETION);
            l.stream()
                    .map(ude -> noValueEntry ? new Entry(null, ude.getUnitId()) : toUnitEntry(ude))
                    .forEach(node.getChildren()::add);
        }
    }

    private Entry toUnitEntry(final UnitDocumentEntity ude) {
        final UnitEntry ue = new UnitEntry(ude.getDocumentId(), ude.getUnitId(), null, false);
        returnUnitDocument(ude, ue, ue.getValue(), ude.getUnitId());
        return ue;
    }

    private void processStudentsUnits(final Envelope node) throws SyntaxException, NotFoundException {
        final Action a = node.getAction();
        if (a != null && a.equals(Action.REQUEST_COMPLETION)) {

            final Set<StudentId> students = new HashSet<>();
            for (Template<?> t : node.getChildren()) {
                final StudentId s = ServiceUtils.toEntry(t, StudentId.class).getIdentity();
                students.add(s);
            }
            final Set<UnitDocumentEntity> res = udef.findForStudents(students);
            node.getChildren().clear();
            node.setAction(Action.RETURN_COMPLETION);

            for (final UnitDocumentEntity ude : res) {
                final UnitEntry ue = new UnitEntry(ude.getDocumentId(), ude.getUnitId(), null, false);
                node.getChildren().add(ue);
                returnUnitDocument(ude, ue, ue.getValue(), ude.getUnitId());
            }
        }
    }

    private void processUnitsParticipants(Container container, Entry<UnitId, ?> entryNode) throws NotFoundException, SyntaxException, UnauthorizedException {
        final UnitId unit = entryNode.getIdentity();
        for (Template<?> t : entryNode.getChildren()) {
            UnitEntry uentry = ServiceUtils.toEntryType(t, UnitEntry.class);
            DocumentId docId = uentry.getIdentity();
            GenericXmlDocument xmlDoc = null;
            try {
                xmlDoc = GenericXmlDocument.class.cast(uentry.getValue());
            } catch (NullPointerException | ClassCastException e) {
                throw ServiceUtils.createSyntaxException(e);
            }
            UnitDocumentEntity u;
            final Action action = uentry.getAction();
            if (unit != null && docId != null) {
                if (action != null && action.equals(Action.REQUEST_COMPLETION)) {
                    u = udef.find(docId, LockModeType.OPTIMISTIC);
                    if (u == null || !u.getUnitId().equals(unit)) {
                        throw ServiceUtils.createNotFoundException(unit);
                    }
                    uentry.getChildren().clear();
                    uentry.setAction(Action.RETURN_COMPLETION);
                    returnUnitDocument(u, uentry, xmlDoc, unit);
                    final boolean addPrimaryUnits = Boolean.parseBoolean(uentry.getHints().getOrDefault("units.add-students-primary-units", "false"));
                    if (addPrimaryUnits) {
                        addPrimaryUnitsToContainer(container, u.getStudentIds());
                    }
                } else if (action != null && action.equals(Action.ANNUL)) {
                    u = udef.find(docId, LockModeType.OPTIMISTIC);
                    if (u == null || !u.getUnitId().equals(unit)) {
                        throw ServiceUtils.createNotFoundException(unit);
                    }
                    udef.remove(u);
                    uentry.getChildren().clear();
                    uentry.setAction(Action.CONFIRM);
                } else {
//                    boolean bulk = ServiceUtils.getBulkProcessValue(uentry, context);

                    boolean file = action != null && action.equals(Action.FILE);
                    u = udef.find(docId, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                    DocumentId.Version ov = null;
                    if (u == null && file) {
                        u = udef.create(unit, "students", null);
                    } else if (u == null) {
                        throw ServiceUtils.createNotFoundException(docId);
                    } else {
                        ov = u.getCurrentVersion();
                    }
                    boolean notify = false;
                    if (file) {
                        if (!xmlDoc.isFragment() && u.markers().length != 0) {
                            ServiceUtils.checkUnitAdmin(context);
                            u.clearMarkerSet();
                            notify = true;
                        }
                        for (MarkerAdapter m : ((XmlMarkerSet) xmlDoc.getMarkerSet()).getAdapterSet()) {
//                            u.addMarker(m.getConvention(), m.getId(), m.getSubset());
                            final Action markerAction = m.getAction();
                            if (markerAction == null || markerAction.equals(Action.FILE)) {
                                u.addMarker(m.getConvention(), m.getId(), m.getSubset());
                            } else if (markerAction.equals(Action.ANNUL)) {
                                u.removeMarker(m.getConvention(), m.getId(), m.getSubset());
                            }
                            notify = true;
                        }
                        final String pTermSched = uentry.getPreferredTermSchedule();
                        if (!Objects.equals(u.getTermScheduleProvider(), pTermSched)) {
                            ServiceUtils.checkUnitAdmin(context);
                            notify = true;
                            if (pTermSched != null) {
                                u.setTermScheduleProvider(pTermSched);
                            } else if (!xmlDoc.isFragment()) {
                                u.setTermScheduleProvider(null);
                            }
                        }
                        final String cname = uentry.getCommonUnitName();
                        final DocumentId cNames = cd.forName(CommonDocuments.COMMON_NAMES_DOCID);
                        if (cNames != null && !Objects.equals(udef.getCommonName(cNames, unit), cname)) {
                            ServiceUtils.checkUnitAdmin(context);
                            notify = true;
                            if (cname != null) {
                                udef.setCommonName(cNames, unit, cname);
                            } else if (!xmlDoc.isFragment()) {
                                udef.setCommonName(cNames, unit, null);
                            }
                        }
                        final ZonedDateTime expiry = uentry.getValue().getDocumentValidity().getExpirationDate();
                        if (!Objects.equals(u.getExpirationDate(), expiry)) {
                            ServiceUtils.checkUnitAdmin(context);
                            notify = true;
                            if (expiry != null) {
                                u.setExpirationDate(expiry);
                            } else if (!xmlDoc.isFragment()) {
                                u.setExpirationDate(null);
                            }
                        }
                    }
                    Set<StudentId> addStuds = new HashSet<>();
                    for (Template<?> st : uentry.getChildren()) {
                        Entry<StudentId, ?> sentry = ServiceUtils.toEntry(st, StudentId.class);
                        if (file || (st.getAction() != null && st.getAction().equals(Action.FILE))) {
                            StudentId sid = sentry.getIdentity();
                            addStuds.add(sid);
                        }
                    }
                    final Set<StudentId> students = u.getStudentIds();
                    Set<StudentId> removed = Collections.EMPTY_SET;
                    Set<StudentId> added = addStuds.stream().filter(s -> !students.contains(s)).collect(Collectors.toSet());
                    if (file) {
                        removed = students.stream().filter(s -> !addStuds.contains(s)).collect(Collectors.toSet());
                        notify = notify | students.retainAll(addStuds);
                    }
                    notify = notify | students.addAll(addStuds);
                    if (notify) {
                        if (ov != null) {
                            DocumentId.Version nv = Documents.inc(ov, 2);
                            u.setCurrentVersion(nv);
                            u.addChangeLog(new VersionChangeLog(u, ov, new Date(System.currentTimeMillis())));
                            for (StudentId sid : removed) {
                                u.addChangeLog(new StudentIdCollectionChangeLog(u, "UNIT_DOCUMENT_STUDENTS", sid, BaseChangeLog.Action.REMOVE));
                            }
                            for (StudentId sid : added) {
                                u.addChangeLog(new StudentIdCollectionChangeLog(u, "UNIT_DOCUMENT_STUDENTS", sid, BaseChangeLog.Action.ADD));
                            }
                        }
                        udef.edit(u, notify);
                    }
                }
            }
        }
    }

    protected void returnUnitDocument(UnitDocumentEntity u, UnitEntry uentry, GenericXmlDocument xmlDoc, final UnitId unit) {
        final String dv = uentry.getHints().get("version.asOf");
        final boolean children = !Boolean.parseBoolean(uentry.getHints().getOrDefault("request-completion.no-children", "false"));
        uentry.getChildren().clear();
        if (children) {
            final Set<StudentId> s;
            if (dv == null) {
                s = u.getStudentIds();
            } else {
                s = adoptStudents(u, dv);
            }
            s.stream()
                    .forEach(sid -> uentry.getChildren().add(new Entry<>(null, sid)));
        }
        final GenericXmlDocument udoc = xmlDoc;
        //Avoid calling MarkerFactory.find
        u.getEmbeddableMarkers().stream()
                .forEach(em -> ((XmlMarkerSet) udoc.getMarkerSet()).add(em.getConvention(), em.getId(), em.getSubset(), null));
        final DocumentId cNames = cd.forName(CommonDocuments.COMMON_NAMES_DOCID);
        if (cNames != null) {
            final String cn = udef.getCommonName(cNames, unit);
            uentry.setCommonUnitName(cn);
        }
        final String ts = u.getTermScheduleProvider();
        uentry.setPreferredTermSchedule(ts);
        final ZonedDateTime exp = u.getExpirationDate();
        uentry.setDocumentValidity(exp);
    }

    private void processPrimaryUnitsSignees(final DocumentEntry<?> node) throws NotFoundException, SyntaxException {
        final DocumentId klassenLehrerDoc = node.getIdentity();
        final Action a = node.getAction();
        if (a != null && a.equals(Action.REQUEST_COMPLETION)) {
            node.getChildren().clear();
            node.setAction(Action.RETURN_COMPLETION);
            final List<UnitDocumentEntity> addAll;
            final Optional<Marker> selector;
            try {
                selector = Optional.ofNullable(node.getHints().get("add-all-units-selector"))
                        .map(MarkerFactory::resolveAbstract)
                        .filter(m -> !Marker.isNull(m));
            } catch (final IllegalArgumentException illex) {
                throw ServiceUtils.createSyntaxException(illex);
            }
            if (selector.isPresent()) {
                addAll = udef.findAll(selector.get(), LockModeType.OPTIMISTIC);
            } else {
                addAll = udef.getAllPrimaryUnits(null);
            }
            final Map<UnitId, List<Signee>> m = udef.getPrimaryUnitsSignees(klassenLehrerDoc);
            Stream.concat(addAll.stream().map(UnitDocumentEntity::getUnitId), m.keySet().stream())
                    .distinct()
                    .forEach(pu -> {
                        Entry<UnitId, ?> pue = new Entry<>(null, pu);
                        node.getChildren().add(pue);
                        final List<Signee> l = m.get(pu);
                        if (l != null && !l.isEmpty()) {
                            l.stream()
                                    .map(sig -> new Entry<>(null, sig))
                                    .forEach(sige -> pue.getChildren().add(sige));
                        }
                    });
        } else {
            try {
                final Map<UnitId, List<Entry<Signee, ?>>> filed = node.getChildren().stream()
                        .filter(n -> DocumentUtilities.isEntryIdentity(n, UnitId.class))
                        .map(n -> (Entry<UnitId, ?>) n)
                        .collect(Collectors.toMap(Entry::getIdentity, e -> DocumentUtilities.extractEntryListChildren(e, Signee.class, Action.FILE)));
                filed.forEach((pu, l) -> l.stream().forEach(e -> {
                    final String propagationId = e.getHints().get("event-propagation-id");
                    udef.setPrimaryUnit(klassenLehrerDoc, e.getIdentity(), pu, propagationId);
                    e.setAction(Action.CONFIRM);
                }));
                final List<Entry<Signee, ?>> annuled = node.getChildren().stream()
                        .filter(n -> DocumentUtilities.isEntryIdentity(n, Signee.class))
                        .map(n -> (Entry<Signee, ?>) n)
                        .filter(e -> Action.ANNUL.equals(e.getAction()))
                        .collect(Collectors.toList());
                annuled.forEach(e -> {
                    final String propagationId = e.getHints().get("event-propagation-id");
                    udef.setPrimaryUnit(klassenLehrerDoc, e.getIdentity(), null, propagationId);
                    e.setAction(Action.CONFIRM);
                });
            } catch (IllegalServiceArgumentException ex) {//If not found signeeEntity
                throw ServiceUtils.createNotFoundException(null);
            }
        }
    }

    private void addPrimaryUnitsToContainer(Container container, Set<StudentId> secoll) {
        final Map<UnitId, Set<StudentId>> pus = new HashMap<>();
        secoll.forEach(sid -> {
            final UnitId pu = udef.findPrimaryUnitForStudent(sid, null, LockModeType.OPTIMISTIC);
            if (pu != null) {
                pus.computeIfAbsent(pu, u -> new HashSet<>()).add(sid);
            }
        });

        pus.forEach((pu, l) -> createStudentOrUnitImpl(container, pu, l));
    }

    private Template createStudentOrUnitImpl(Container container, UnitId unit, Set<StudentId> id) {
        final Template file = new Template(Action.RETURN_COMPLETION);
        id.stream()
                .map(s -> new Entry(null, s))
                .forEach(file.getChildren()::add);
        final Entry root = new Entry(null, unit);
        root.getChildren().add(file);
        addPath(container, PU_PARTICIPANTS_PATH, root);
        container.getEntries().add(root);
        return root;
    }

    private void addPath(Container container, String[] pathIdentifiers, Template unitFile) {
        if (pathIdentifiers != null && pathIdentifiers.length > 0) {
            int l = pathIdentifiers.length;
            Container.PathDescriptorElement pathEl = new Container.PathDescriptorElement(unitFile, pathIdentifiers[--l]);
            while (l != 0) {
                pathEl = new Container.PathDescriptorElement(pathEl, pathIdentifiers[--l]);
            }
            container.getPathElements().add(pathEl);
        }
    }

}
