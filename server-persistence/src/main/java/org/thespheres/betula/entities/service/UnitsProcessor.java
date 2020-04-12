/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.Template;
import org.thespheres.betula.document.util.GenericXmlDocument;
import org.thespheres.betula.document.util.UnitEntry;
import org.thespheres.betula.document.util.XmlMarkerSet;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.UnitDocumentEntity;
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
public class UnitsProcessor extends AbstractContainerProcessor {

    private final String[] PU_PARTICIPANTS_PATH = new String[]{"primary-units", "participants"};
    @EJB
    private UnitDocumentFacade udef;
    @EJB
    private GradeTargetDocumentFacade targets;
//    @Inject
//    private DocumentsNotificator documentsNotificator;
    @Inject
    private CommonDocuments cd;

    public UnitsProcessor() {
        super(new String[][]{Paths.UNITS_PATH, Paths.UNITS_PARTICIPANTS_PATH, Paths.TARGETS_PATH});
    }

    @Override
    protected void process(Container container, String[] path, Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException {
        if (Arrays.equals(path, Paths.UNITS_PATH)) {
            processUnits(template);
        } else if (Arrays.equals(path, Paths.UNITS_PARTICIPANTS_PATH)) {
            final Entry<UnitId, ?> entryNode = toEntry(template, UnitId.class);
            processUnitsParticipants(container, entryNode);
        } else if (Arrays.equals(path, Paths.TARGETS_PATH)) {
            processTargets(template);
        }
    }

    private void processTargets(Envelope node) {
        final Action a = node.getAction();
        if (a != null && a.equals(Action.REQUEST_COMPLETION)) {
            final List<TermGradeTargetAssessmentEntity> l = targets.findAll(LockModeType.READ, TermGradeTargetAssessmentEntity.class);
            node.getChildren().clear();
            node.setAction(Action.RETURN_COMPLETION);
            l.stream()
                    .forEach(tgtae -> {
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
    private void processUnits(Envelope node) {
        Action a = node.getAction();
        if (a != null && a.equals(Action.REQUEST_COMPLETION)) {
//            boolean doAsAdmin = false;
//            String pRole = node.getHints().get("preferred-security-role");
//            if (context.isUserInRole(pRole) && "unitadmins".equals(pRole)) {
//                doAsAdmin = true;
//            }
//            CriteriaQuery<UnitDocumentEntity> cq = em.getCriteriaBuilder().createQuery(UnitDocumentEntity.class);
//            Root<UnitDocumentEntity> pet = cq.from(UnitDocumentEntity.class);
//            List<Predicate> list = new ArrayList<>();
//            if (!doAsAdmin) {
//                Signee signee = login.getSigneePrincipal();
//                CriteriaBuilder cb = em.getCriteriaBuilder();
//                list.add(cb.equal(pet.join("targets").join("signeeInfoentries").get("type"), "entitled.signee"));
//                list.add(cb.equal(pet.join("targets").join("signeeInfoentries").get("signee").get("prefix"), signee.getPrefix()));
//                list.add(cb.equal(pet.join("targets").join("signeeInfoentries").get("signee").get("suffix"), signee.getSuffix()));
//                list.add(cb.equal(pet.join("targets").join("signeeInfoentries").get("signee").get("alias"), signee.isAlias()));
//            }
//            cq.select(pet).distinct(true).where(list.stream().toArray((s) -> new Predicate[s]));
//            List<UnitDocumentEntity> l = em.createQuery(cq).getResultList();
            List<UnitDocumentEntity> l = udef.findAll(LockModeType.OPTIMISTIC);
            node.getChildren().clear();
            node.setAction(Action.RETURN_COMPLETION);
//            l.stream().map(ude -> new UnitEntry(ude.getDocumentId(), null)).forEach(ue -> node.getChildren().add(ue));
            l.stream()
                    .map(ude -> new Entry(null, ude.getUnitId()))
                    .forEach(node.getChildren()::add);
        }
    }

    private void processUnitsParticipants(Container container, Entry<UnitId, ?> entryNode) throws NotFoundException, SyntaxException, UnauthorizedException {
        UnitId unit = entryNode.getIdentity();
        for (Template<?> t : entryNode.getChildren()) {
            UnitEntry uentry = toEntryType(t, UnitEntry.class);
            DocumentId docId = uentry.getIdentity();
            GenericXmlDocument xmlDoc = null;
            try {
                xmlDoc = GenericXmlDocument.class.cast(uentry.getValue());
            } catch (NullPointerException | ClassCastException cce) {
                throw ServiceUtils.createSyntaxException(cce);
            }
            UnitDocumentEntity u;
            Action action = uentry.getAction();
            if (unit != null && docId != null) {
                if (action != null && action.equals(Action.REQUEST_COMPLETION)) {
                    u = udef.find(docId, LockModeType.OPTIMISTIC);
                    if (u == null || !u.getUnitId().equals(unit)) {
                        throw ServiceUtils.createNotFoundException(unit);
                    }
                    uentry.getChildren().clear();
                    uentry.setAction(Action.RETURN_COMPLETION);
                    u.getStudentIds().stream()
                            .forEach(sid -> uentry.getChildren().add(new Entry<>(null, sid)));
                    final GenericXmlDocument udoc = xmlDoc;
                    //Avoid calling MarkerFactory.find
                    u.getEmbeddableMarkers().stream()
                            .forEach(em -> ((XmlMarkerSet) udoc.getMarkerSet()).add(em.getConvention(), em.getId(), em.getSubset(), null));
                    final DocumentId cNames = cd.forName(CommonDocuments.COMMON_NAMES_DOCID);
                    if (cNames != null) {
                        String cn = udef.getCommonName(cNames, unit);
                        uentry.setCommonUnitName(cn);
                    }
                    String ts = u.getTermScheduleProvider();
                    uentry.setPreferredTermSchedule(ts);
                    ZonedDateTime exp = u.getExpirationDate();
                    uentry.setDocumentValidity(exp);
                    addPrimaryUnitsToContainer(container, u.getStudentIds());
                } else {
//                    boolean bulk = ServiceUtils.getBulkProcessValue(uentry, context);
//
//                    boolean file = action != null && action.equals(Action.FILE);
//                    u = udef.find(docId, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
//                    DocumentId.Version ov = null;
//                    if (u == null && file) {
//                        u = udef.create(unit, "students", null);
//                    } else if (u == null) {
//                        throw new NotFoundFault();
//                    } else {
//                        ov = u.getCurrentVersion();
//                    }
//                    boolean notify = false;
//                    if (file) {
//                        if (!xmlDoc.isFragment() && !u.markers().isEmpty()) {
//                            ServiceUtils.checkUnitAdmin(context);
//                            u.clearMarkerSet();
//                            notify = true;
//                        }
//                        for (MarkerAdapter m : ((XmlMarkerSet) xmlDoc.markers()).getAdapterSet()) {
//                            u.addMarker(m.getConvention(), m.getId(), m.getSubset());
//                            notify = true;
//                        }
//                        String pTermSched = uentry.getPreferredTermSchedule();
//                        if (!Objects.equals(u.getTermScheduleProvider(), pTermSched)) {
//                            ServiceUtils.checkUnitAdmin(context);
//                            notify = true;
//                            if (pTermSched != null) {
//                                u.setTermScheduleProvider(pTermSched);
//                            } else if (!xmlDoc.isFragment()) {
//                                u.setTermScheduleProvider(null);
//                            }
//                        }
//                        String cname = uentry.getCommonUnitName();
//                        if (!Objects.equals(udef.getStringValue(KGS.AG_NAMEN_DOCID, unit), cname)) {
//                            ServiceUtils.checkUnitAdmin(context);
//                            notify = true;
//                            if (cname != null) {
//                                udef.setStringValue(KGS.AG_NAMEN_DOCID, unit, cname);
//                            } else if (!xmlDoc.isFragment()) {
//                                udef.setStringValue(KGS.AG_NAMEN_DOCID, unit, null);
//                            }
//                        }
//                        Date expiry = uentry.getValue().getDocumentValidity().getExpirationDate();
//                        if (!Objects.equals(u.getExpirationDate(), expiry)) {
//                            ServiceUtils.checkUnitAdmin(context);
//                            notify = true;
//                            if (expiry != null) {
//                                u.setExpirationDate(expiry);
//                            } else if (!xmlDoc.isFragment()) {
//                                u.setExpirationDate(null);
//                            }
//                        }
//                    }
//                    Set<StudentId> addStuds = new HashSet<>();
//                    for (Template<?> st : uentry.getChildren()) {
//                        Entry<StudentId, ?> sentry = toEntry(st, StudentId.class);
//                        if (file || (st.getAction() != null && st.getAction().equals(Action.FILE))) {
//                            StudentId sid = sentry.getIdentity();
//                            addStuds.add(sid);
//                        }
//                    }
//                    final Set<StudentId> students = u.getStudentIds();
//                    Set<StudentId> removed = Collections.EMPTY_SET;
//                    Set<StudentId> added = addStuds.stream().filter(s -> !students.contains(s)).collect(Collectors.toSet());
//                    if (file) {
//                        removed = students.stream().filter(s -> !addStuds.contains(s)).collect(Collectors.toSet());
//                        notify = notify | students.retainAll(addStuds);
//                    }
//                    notify = notify | students.addAll(addStuds);
//                    if (notify) {
//                        if (ov != null) {
//                            DocumentId.Version nv = Documents.inc(ov, 2);
//                            u.setCurrentVersion(nv);
//                            u.addChangeLog(new VersionChangeLog(u, ov.getVersion(), new Date(System.currentTimeMillis())));
//                            for (StudentId sid : removed) {
//                                u.addChangeLog(new StudentIdCollectionChangeLog(u, "UNIT_DOCUMENT_STUDENTS", sid, BaseChangeLog.Action.REMOVE));
//                            }
//                            for (StudentId sid : added) {
//                                u.addChangeLog(new StudentIdCollectionChangeLog(u, "UNIT_DOCUMENT_STUDENTS", sid, BaseChangeLog.Action.ADD));
//                            }
//                        }
//                        udef.edit(u, notify);
//                    }
                }
            }
        }
    }

    private void addPrimaryUnitsToContainer(Container container, Collection<StudentId> secoll) {
        final Map<UnitId, List<StudentId>> m = new HashMap<>();
        for (StudentId sid : secoll) {
            UnitId pu = udef.findPrimaryUnitForStudent(sid, null, LockModeType.OPTIMISTIC);
            m.computeIfAbsent(pu, key -> new ArrayList())
                    .add(sid);
//            for (UnitDocumentEntity ude : ul) {
//                UnitId pu = ude.getUnitId();
//                m.computeIfAbsent(pu, key -> new ArrayList());
//                m.compute(pu, (key, l) -> {
//                    l.add(sid);
//                    return l;
//                });
//            }
        }
        m.keySet().stream()
                .forEach(pu -> createStudentOrUnitImpl(container, pu, m.get(pu), PU_PARTICIPANTS_PATH, Action.RETURN_COMPLETION));
    }

    private Template createStudentOrUnitImpl(Container container, UnitId unit, List<StudentId> id, String[] pathIdentifiers, Action action) {
        Template file = new Template(action);
        id.stream().map(s -> new Entry(null, s)).forEach(e -> file.getChildren().add(e));
        Entry root = new Entry(null, unit);
        root.getChildren().add(file);
        addPath(container, pathIdentifiers, root);
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
