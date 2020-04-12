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
import java.time.format.DateTimeParseException;
import org.thespheres.betula.entities.service.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.dom.DOMResult;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.services.jms.AbstractDocumentEvent;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Template;
import org.thespheres.betula.document.ValueElement;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.document.util.MarkerAdapter;
import org.thespheres.betula.document.util.XmlDocumentEntry;
import org.thespheres.betula.entities.EmbeddableGrade;
import org.thespheres.betula.entities.EmbeddableStudentId;
import org.thespheres.betula.entities.EmbeddableTermId;
import org.thespheres.betula.entities.TermReportDocumentEntity;
import org.thespheres.betula.entities.UnitDocumentEntity;
import org.thespheres.betula.entities.config.IllegalServiceArgumentException;
import org.thespheres.betula.entities.facade.GradeTargetDocumentFacade;
import org.thespheres.betula.entities.facade.TextTargetDocumentFacade;
import org.thespheres.betula.entities.facade.UnitDocumentFacade;
import org.thespheres.betula.entities.jmsimpl.DocumentsNotificator;
import org.thespheres.betula.niedersachsen.xml.NdsZeugnisAngaben;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.Paths;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.UnauthorizedException;
import org.w3c.dom.Document;

/**
 *
 * @author boris.heithecker
 */
@Dependent
@Stateless
public class AdminReportsProcessor extends AbstractAdminContainerProcessor {

    private static JAXBContext jaxb;

    @PersistenceContext(unitName = "betula0")
    private EntityManager em;
    @EJB
    private UnitDocumentFacade units;
    @EJB
    protected GradeTargetDocumentFacade facade;
    @EJB
    protected TextTargetDocumentFacade textFacade;
    @Default
    @Inject
    private DocumentsModel docModel;
    @Inject
    protected DocumentsNotificator documentsNotificator;

    static synchronized JAXBContext getZeungnisAngabenJAXB() {
        if (jaxb == null) {
            try {
                jaxb = JAXBContext.newInstance(NdsZeugnisAngaben.class);
            } catch (JAXBException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return jaxb;
    }

    public AdminReportsProcessor() {
        super(new String[][]{Paths.UNITS_REPORTS_PATH,
            Paths.REPORTS_PATH,
            Paths.UNITS_REPORTTERMGRADES_PATH});
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    @Override
    public void process(String[] path, Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException {
        if (Arrays.equals(path, Paths.UNITS_REPORTS_PATH)) {
            processUnitsReports(template);
        } else if (Arrays.equals(path, Paths.REPORTS_PATH)) {
            processReports(template);
        } else if (Arrays.equals(path, Paths.UNITS_REPORTTERMGRADES_PATH)) {
            processUnitsReporttermgrades(template);
        }
    }

    private void processUnitsReports(final Envelope node) throws SyntaxException, NotFoundException {
        final Entry<UnitId, ?> ue = ServiceUtils.toEntry(node, UnitId.class);
        final Action unitEntryAction = ue.getAction();
        if (node.getChildren().isEmpty() && unitEntryAction.equals(Action.REQUEST_COMPLETION)) {
//            processReports(ue, ue.getIdentity(), null);
        } else if (!node.getChildren().isEmpty()) {
            for (final Template<?> tt : node.getChildren()) {
                final Action termEntryAction = tt.getAction();
                if (termEntryAction != null && termEntryAction.equals(Action.REQUEST_COMPLETION)) {
                    final Entry<TermId, ?> te = ServiceUtils.toEntry(tt, TermId.class);
                    processReports(te, ue.getIdentity(), te.getIdentity(), ue.getHints());
                }
            }
        }
    }

    private void processUnitsReporttermgrades(final Envelope node) throws SyntaxException, NotFoundException {
        final Entry<UnitId, ?> ue = ServiceUtils.toEntry(node, UnitId.class);
        final Action unitEntryAction = ue.getAction();
        if (node.getChildren().isEmpty() && unitEntryAction.equals(Action.REQUEST_COMPLETION)) {
//            processReports(ue, ue.getIdentity(), null);
        } else if (!node.getChildren().isEmpty()) {
            for (final Template<?> tt : node.getChildren()) {
                final Action termEntryAction = tt.getAction();
                if (termEntryAction != null && termEntryAction.equals(Action.REQUEST_COMPLETION)) {
                    final Entry<TermId, ?> te = ServiceUtils.toEntry(tt, TermId.class);
                    processReportTermgrades(te, ue.getIdentity(), te.getIdentity(), ue.getHints());
                }
            }
        }
    }

    private void processReports(final Envelope node, final UnitId pu, final TermId term, final Map<String, String> hints) throws SyntaxException, NotFoundException {
        node.getChildren().clear();
        node.setAction(Action.RETURN_COMPLETION);
        final DocumentId udoc = docModel.convertToUnitDocumentId(pu);
        final UnitDocumentEntity ude = units.find(udoc, LockModeType.OPTIMISTIC);
        final boolean noReportDocumentValue = node.getHints().getOrDefault("term.reports.no.document", "false").equals("true");
        LocalDate asOf = null;
        final String hint = hints.get("asOf");
        if (hint != null) {
            try {
                asOf = LocalDate.parse(hint);
            } catch (DateTimeParseException e) {
                Logger.getLogger(AdminReportsProcessor.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        }
        final List<TermReportDocumentEntity> l;
        if (ude != null && asOf == null) {
            l = em.createNamedQuery("findTermReportForUnit", TermReportDocumentEntity.class)
                    .setParameter("term", term == null ? null : new EmbeddableTermId(term))
                    .setParameter("unitEntity", ude)
                    .setLockMode(LockModeType.OPTIMISTIC)
                    .getResultList();
        } else if (ude != null && term != null && asOf != null) {
            final Date date;
            try {
                date = Date.from(Instant.from(asOf.atStartOfDay().atZone(ZoneId.systemDefault())));
            } catch (DateTimeException ex) {
                throw new IllegalServiceArgumentException("Illegal hint.");
            }
            final Set<StudentId> studs = new HashSet<>(ude.getStudentIds());
            units.adoptStudentsToVersionAsOf(ude, date, studs);
            l = studs.stream()
                    .map(s -> new EmbeddableStudentId(s))
                    .flatMap(s -> {
                        return em.createNamedQuery("findTermReport", TermReportDocumentEntity.class)
                                .setParameter("term", new EmbeddableTermId(term))
                                .setParameter("student", s)
                                .setLockMode(LockModeType.OPTIMISTIC)
                                .getResultList().stream();
                    })
                    .collect(Collectors.toList());
        } else {
            l = Collections.EMPTY_LIST;
        }
        final Map<StudentId, List<TermReportDocumentEntity>> m = l.stream()
                .collect(Collectors.groupingBy(TermReportDocumentEntity::getStudent));
        m.entrySet().stream()
                .map(e -> toEntry(e, noReportDocumentValue))
                .forEach(node.getChildren()::add);
    }

    private Entry<StudentId, ?> toEntry(Map.Entry<StudentId, List<TermReportDocumentEntity>> entry, boolean omitDocuments) {
        final Entry<StudentId, ?> ret = new Entry<>(null, entry.getKey());
        entry.getValue().stream()
                .map(tre -> {
                    if (!omitDocuments) {
                        final Grade avnote = tre.getAvnote() != null ? tre.getAvnote().findGrade() : null;
                        final Grade svnote = tre.getSvnote() != null ? tre.getSvnote().findGrade() : null;
                        final Marker[] markers = tre.markers();
                        final NdsZeugnisAngaben.FreieBemerkung[] custom = tre.getFreeNotes().entrySet().stream()
                                .map(me -> new NdsZeugnisAngaben.FreieBemerkung(me.getValue(), me.getKey(), null))
                                .toArray(NdsZeugnisAngaben.FreieBemerkung[]::new);
//                        NdsZeugnisAngaben.Text
                        final NdsZeugnisAngaben angaben = new NdsZeugnisAngaben(tre.getFehltage(), tre.getUnentschuldigt(), avnote, svnote, markers, custom);
                        final DOMResult result = new DOMResult();
                        try {
                            getZeungnisAngabenJAXB().createMarshaller().marshal(angaben, result);
                        } catch (JAXBException ex) {
                            throw new IllegalStateException(ex);
                        }
                        final Document d = (Document) result.getNode();
                        final XmlDocumentEntry re = new XmlDocumentEntry(tre.getDocumentId(), null, false);
                        re.setReportDataElement(d.getDocumentElement());
                        return re;
                    } else {
                        return new Entry(null, tre.getDocumentId());
                    }
                })
                .forEach(ret.getChildren()::add);
        return ret;
    }

    private void processReports(Envelope node) throws NotFoundException, SyntaxException, IllegalStateException {
//                final Entry<StudentId, ?> se = ServiceUtils.toEntry(c, StudentId.class);
//                final StudentId student = se.getIdentity();
//                for (Template<?> rc : c.getChildren()) {
        final XmlDocumentEntry re = ServiceUtils.toEntryType(node, XmlDocumentEntry.class);
        final DocumentId doc = re.getIdentity();
        if (doc == null) {
            throw ServiceUtils.createSyntaxException("Identity cannot be null.");
        }
        final TermReportDocumentEntity entity = em.find(TermReportDocumentEntity.class, doc, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (entity == null) {
            throw ServiceUtils.createNotFoundException(doc);
        }
        final NdsZeugnisAngaben angaben;
        try {
            angaben = (NdsZeugnisAngaben) getZeungnisAngabenJAXB().createUnmarshaller().unmarshal(re.getReportDataElement());
        } catch (JAXBException ex) {
            throw new IllegalStateException(ex);
        }
        update(entity, angaben);
        em.merge(entity);
    }

    protected void update(final TermReportDocumentEntity tre, final NdsZeugnisAngaben angaben) {
        boolean changed = false;
        final ValueElement<Grade> av = angaben.getArbeitsverhalten();
        final Action avAction;
        if (av != null && (avAction = av.getAction()) != null) {
            final Grade g = av.getValue();
            switch (avAction) {
                case FILE:
                    tre.setAvnote(g == null ? null : new EmbeddableGrade(g));
                    av.setAction(Action.CONFIRM);
                    break;
                case ANNUL:
                    final Grade before = tre.getAvnote() == null ? null : tre.getAvnote().findGrade();
                    if (g == null || Objects.equals(g, before)) {
                        tre.setAvnote(null);
                        av.setAction(Action.CONFIRM);
                    }
                    break;
            }
            changed = true;
        }
        //
        final ValueElement<Grade> sv = angaben.getSozialverhalten();
        final Action svAction;
        if (sv != null && (svAction = sv.getAction()) != null) {
            final Grade g = sv.getValue();
            switch (svAction) {
                case FILE:
                    tre.setSvnote(g == null ? null : new EmbeddableGrade(g));
                    sv.setAction(Action.CONFIRM);
                    break;
                case ANNUL:
                    final Grade before = tre.getSvnote() == null ? null : tre.getSvnote().findGrade();
                    if (g == null || Objects.equals(g, before)) {
                        tre.setAvnote(null);
                        sv.setAction(Action.CONFIRM);
                    }
                    break;
            }
            changed = true;
        }
        //
        final ValueElement<Integer> ft = angaben.getFehltage();
        final Action ftAction;
        if (ft != null && (ftAction = ft.getAction()) != null) {
            final Integer v = ft.getValue();
            switch (ftAction) {
                case FILE:
                    tre.setFehltage(v);
                    ft.setAction(Action.CONFIRM);
                    break;
                case ANNUL:
                    if (v == null || v.equals(tre.getFehltage())) {
                        tre.setFehltage(null);
                        ft.setAction(Action.CONFIRM);
                    }
                    break;
            }
            changed = true;
        }
        //
        final ValueElement<Integer> ue = angaben.getUnentschuldigt();
        final Action ueAction;
        if (ue != null && (ueAction = ue.getAction()) != null) {
            final Integer v = ue.getValue();
            switch (ueAction) {
                case FILE:
                    tre.setUnentschuldigt(v);
                    ue.setAction(Action.CONFIRM);
                    break;
                case ANNUL:
                    if (v == null || v.equals(tre.getUnentschuldigt())) {
                        tre.setUnentschuldigt(null);
                        ue.setAction(Action.CONFIRM);
                    }
                    break;
            }
            changed = true;
        }
        //
        final ValueElement<Marker>[] mee = angaben.getMarkerElements();
        if (mee != null) {
            for (final ValueElement<Marker> me : mee) {
                final Action meAction;
                if (me != null && (meAction = me.getAction()) != null) {
                    final Object v = me.rawValue();
                    switch (meAction) {
                        case FILE:
                            if (v != null) {
                                if (v instanceof MarkerAdapter) {
                                    final MarkerAdapter ma = (MarkerAdapter) v;
                                    tre.addMarker(ma.getConvention(), ma.getId(), ma.getSubset());
                                } else {
                                    final Marker m = me.getValue();
                                    tre.addMarker(m);
                                }
                                me.setAction(Action.CONFIRM);
                            }
                            break;
                        case ANNUL:
                            if (v != null) {
                                if (v instanceof MarkerAdapter) {
                                    final MarkerAdapter ma = (MarkerAdapter) v;
                                    tre.removeMarker(ma.getConvention(), ma.getId(), ma.getSubset());
                                } else {
                                    final Marker m = me.getValue();
                                    tre.removeMarker(m);
                                }
                                me.setAction(Action.CONFIRM);
                            }
                            break;
                    }
                    changed = true;
                }
            }
        }
        //
//        NdsZeugnisAngaben.Text
        //
        final NdsZeugnisAngaben.FreieBemerkung[] fbb = angaben.getCustom();
        if (fbb != null) {
            for (final NdsZeugnisAngaben.FreieBemerkung fb : fbb) {
                final Action fbAction;
                if (fb != null && (fbAction = fb.getAction()) != null) {
                    final String text = fb.getValue();
                    final int pos = fb.getPosition();
                    switch (fbAction) {
                        case FILE:
                            if (text != null) {
                                tre.getFreeNotes().put(pos, text);
                                fb.setAction(Action.CONFIRM);
                            }
                            break;
                        case ANNUL:
                            if (text == null || text.equals(tre.getFreeNotes().get(pos))) {
                                tre.getFreeNotes().remove(pos);
                                fb.setAction(Action.CONFIRM);
                            }
                            break;
                    }
                    changed = true;
                }
            }
        }
        if (changed) {
            final AbstractDocumentEvent evt = new AbstractDocumentEvent(tre.getDocumentId(), AbstractDocumentEvent.DocumentEventType.CHANGE, login.getSigneePrincipal(false));
            documentsNotificator.notityConsumers(evt);
        }
    }

    private void processReportTermgrades(final Envelope node, final UnitId pu, final TermId term, final Map<String, String> hints) throws SyntaxException, NotFoundException {
        node.getChildren().clear();
        node.setAction(Action.RETURN_COMPLETION);
//        final DocumentId udoc = docModel.convertToUnitDocumentId(pu);
//        final UnitDocumentEntity ude = units.find(udoc, LockModeType.OPTIMISTIC);
//        final boolean noReportDocumentValue = node.getHints().getOrDefault("term.reports.no.document", "false").equals("true");
//        LocalDate asOf = null;
//        final String hint = hints.get("asOf");
//        if (hint != null) {
//            try {
//                asOf = LocalDate.parse(hint);
//            } catch (DateTimeParseException e) {
//                Logger.getLogger(AdminReportsProcessor.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
//            }
//        }
//        final List<TermReportDocumentEntity> l;
//        if (ude != null && asOf == null) {
//            l = em.createNamedQuery("findTermReportForUnit", TermReportDocumentEntity.class)
//                    .setParameter("term", term == null ? null : new EmbeddableTermId(term))
//                    .setParameter("unitEntity", ude)
//                    .setLockMode(LockModeType.OPTIMISTIC)
//                    .getResultList();
//        } else if (ude != null && term != null && asOf != null) {
//            final Date date;
//            try {
//                date = Date.from(Instant.from(asOf.atStartOfDay().atZone(ZoneId.systemDefault())));
//            } catch (DateTimeException ex) {
//                throw new IllegalBeanArgumentException("Illegal hint.");
//            }
//            final Set<StudentId> studs = new HashSet<>(ude.getStudentIds());
//            units.adoptStudentsToVersionAsOf(ude, date, studs);
//            l = studs.stream()
//                    .map(s -> new EmbeddableStudentId(s))
//                    .flatMap(s -> {
//                        return em.createNamedQuery("findTermReport", TermReportDocumentEntity.class)
//                                .setParameter("term", new EmbeddableTermId(term))
//                                .setParameter("student", s)
//                                .setLockMode(LockModeType.OPTIMISTIC)
//                                .getResultList().stream();
//                    })
//                    .collect(Collectors.toList());
//        } else {
//            l = Collections.EMPTY_LIST;
//        }
//        final Map<StudentId, List<TermReportDocumentEntity>> m = l.stream()
//                .collect(Collectors.groupingBy(TermReportDocumentEntity::getStudent));
//        m.entrySet().stream()
//                .map(e -> toEntry(e, noReportDocumentValue))
//                .forEach(node.getChildren()::add);

//facade.findForStudents(related, term)
//        final Set<DocumentId> ret;
//        if (!useLinked) {
//            //findTermGradeTargetAssessmentsForUnitEntityStudents
//            ret = facade.findForUnitDocument(ude, null).stream().map(BaseTargetAssessmentEntity::getDocumentId).collect(Collectors.toSet());
//        } else {
//            ret = ude.getTargetAssessments().stream().map(BaseTargetAssessmentEntity::getDocumentId).collect(Collectors.toSet());
//        }
//        if (true) {
//            textFacade.findForPrimaryUnit(unit, LockModeType.OPTIMISTIC).stream().map(BaseTargetAssessmentEntity::getDocumentId).forEach(ret::add);
//        }
    }
}
