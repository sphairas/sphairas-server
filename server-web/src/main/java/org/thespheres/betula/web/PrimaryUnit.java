/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.commons.lang3.StringUtils;
import org.openide.util.NbBundle;
import org.primefaces.event.ToggleEvent;
import org.primefaces.model.Visibility;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;
import org.thespheres.betula.assess.GradeFactory;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.AssessmentConvention;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeReference;
import org.thespheres.betula.services.jms.MultiTargetAssessmentEvent;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.model.MultiSubject;
import org.thespheres.betula.document.model.Subject;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.niedersachsen.ASVAssessmentConvention;
import org.thespheres.betula.niedersachsen.NdsTerms;
import org.thespheres.betula.niedersachsen.zeugnis.ReportProvisionsUtil;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.FastTermTargetDocument.Entry;
import org.thespheres.betula.server.beans.ReportsBean;
import org.thespheres.betula.server.beans.TermReportDataException;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.web.config.WebAppProperties;
import org.thespheres.betula.web.docsrv.AmbiguousDocumentCollectionException;
import org.thespheres.betula.web.docsrv.NdsFormatter;

/**
 *
 * @author boris.heithecker
 */
public class PrimaryUnit extends AbstractData<Subject> {

    private final ReportsBean zeugnisBean = lookupReportsBeanImplRemote();
    private Map<String, Map<MultiSubject, Set<DocumentId>>> docTypes;
    private List<MultiSubject> subjects;
    private final HashMap<String, Map<StudentId, Map<MultiSubject, GradeValue>>> gradeMap = new HashMap<>();
    private final UnitId unit;
    private Collection<DocumentId> documents;
    private final Term term;
    private String[] editingDocTypes;
    private String editingDocType;
    private boolean isResolvedPrimaryUnitName = false;
    private String resolvedPrimaryUnitName = null;
    private HashSet<StudentId> studIds;
    private StudentId selectedStudent;
    public static final int WEB_UI_EDITABLE_REPORT_NOTE_POSITION = 1000;
    private Integer level;
    private final Term termBefore;

    PrimaryUnit(final UnitId unit, final Term term, final Term before, final BetulaWebApplication app) {
        super(app, unit.getId());
        this.unit = unit;
        this.term = term;
        this.termBefore = before;
    }

    public UnitId getUnitId() {
        return unit;
    }

    public boolean isEnableDetails() {
        return Boolean.getBoolean(WebAppProperties.BETULA_WEB_UI_ENABLE_DETAILSLIST_PROPERTY);
    }

    @Override
    public String getDisplayTitle() {
        final String val = getResolvedPrimaryUnitDisplayName();
        if (val != null) {
            return NbBundle.getMessage(PrimaryUnit.class, "primaryUnit.displayTitle", val);
        } else {
            return super.getDisplayTitle();
        }
    }

    protected String getResolvedPrimaryUnitDisplayName() {
        if (!isResolvedPrimaryUnitName) {
            try {
                resolvedPrimaryUnitName = application.getNamingResolver().resolveDisplayName(unit, term);
            } catch (IllegalAuthorityException ex) {
            }
            isResolvedPrimaryUnitName = true;
        }
        return resolvedPrimaryUnitName;
    }

    protected int getPrimaryUnitLevel() {
        if (level == null) {
            try {
                final NamingResolver.Result r = application.getNamingResolver().resolveDisplayNameResult(unit);
                r.addResolverHint("naming.only.level");
                r.addResolverHint("klasse.ohne.schuljahresangabe");
                final String ln = r.getResolvedName(application.getCurrentTerm());
                level = Integer.parseInt(ln);
            } catch (IllegalAuthorityException | NumberFormatException ex) {
                final String msg = "An exception has occurred resolving primary unit level for " + unit.toString();
                Logger.getLogger(PrimaryUnit.class.getName()).log(Level.WARNING, msg, ex);
                level = -1;
            }
        }
        return level;
    }

    @Override
    protected Grade resolveReference(AvailableStudent stud, Subject column, GradeReference proxy) {
//        GradeValue gv = gradeValueForDocType(stud.getId(), column, "vorzensuren");
//        if (gv != null) {
//            return gv.getValue();
//        }
        return proxy;
    }

    @Override
    protected void onDocumentEvent(final MultiTargetAssessmentEvent<TermId> evt) {
        final DocumentId source = evt.getSource();
        //Don't call getDocs() because we do not have a valid session context if onDocumentEvent is called from MessageDrivenBean!
        if (documents == null || !documents.stream().anyMatch(source::equals)) {
            return;
        }
        if (evt.getUpdates() == null) {
            return;
        }
        for (MultiTargetAssessmentEvent.Update<TermId> u : evt.getUpdates()) {
            final StudentId stud = u.getStudent();
            if (studIds == null || (stud != null && !studIds.contains(stud))) {
                continue;
            }
            final TermId termId = u.getGradeId();
            if (term == null || (termId != null && !term.getScheduledItemId().equals(termId))) {
                continue;
            }
            String docType = getEditingDocType();
            final Map<MultiSubject, Set<DocumentId>> m = getDocMap().get(docType);
            if (m == null) {
                return;
            }
            final MultiSubject sub = m.entrySet().stream().filter(e -> e.getValue().contains(source)).findAny().map(e -> e.getKey()).orElse(null);
            if (sub == null) {
                return;
            }
            final Map<StudentId, Map<MultiSubject, GradeValue>> dm = gradeMap.get(docType);
            if (stud != null) {
                AvailableStudent as = findStudent(stud);
                if (as == null) {
                    return;
                }
                final GradeValue gv = dm.get(as.getId()).get(sub);
                if (gv != null) {
//            Grade evtGrade = evt.getNewValue();
                    if (gv.invalidateGrade(u.getValue(), evt.getTimestamp().getValue())) {
//                gv.invalidateGrade();
                        if (application.getCurrentPage().equals("primaryUnits") && dataTableClientId != null) {
//                    String g = evtGrade.getShortLabel();
//                    String n = as.getFullname();
//                    String msg = NbBundle.getMessage(AvailableTarget.class, "target.update.message");
//                    String det = NbBundle.getMessage(AvailableTarget.class, "target.update.message.detail", g, n, termDN);
                            final EventBus eventBus = EventBusFactory.getDefault().eventBus();
                            final BetulaPushMessage message = new BetulaPushMessage();
                            message.setSource(dataTableClientId);
                            message.setUpdate(dataTableClientId);
                            eventBus.publish(NotifyGradeUpdateResource.CHANNEL_BASE + application.getUser().getSignee().getId(), message);
                        }
                    }

                }
            } else {
                //invalidate document
            }
        }

    }

    public String getUnitAuthorityEncoded() {
        try {
            return URLEncoder.encode(unit.getAuthority(), "utf-8");
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    public String getUnitIdEncoded() {
        try {
            return URLEncoder.encode(unit.getId(), "utf-8");
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    public String getTermAuthorityEncoded() {
        try {
            return URLEncoder.encode(term.getScheduledItemId().getAuthority(), "utf-8");
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    public String getTermIdEncoded() {
        try {
            String v = Integer.toString(term.getScheduledItemId().getId());
            return URLEncoder.encode(v, "utf-8");
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    public String[] getEditingDocTypes() {
        if (editingDocTypes == null) {
            editingDocTypes = application.getWebUIConfiguration().getPrimaryUnitListedTargetTypes();
        }
        return editingDocTypes;
    }

    public String getEditingDocType() {
        if (editingDocType == null) {
            editingDocType = application.getWebUIConfiguration().getDefaultCommitTargetType();
        }
        return editingDocType;
    }

    public void setEditingDocType(String type) {
        this.editingDocType = type;
    }

    public String getDocTypeDisplayName(String type) {
        try {
            final String val = getResolvedPrimaryUnitDisplayName();
            final Term t;
            if ("vorzensuren".equalsIgnoreCase(type)) {
                t = application.getTermBefore();
            } else {
                t = term;
            }
            return NbBundle.getMessage(PrimaryUnit.class, "primaryUnit.docDisplayName", val, t.getDisplayName(), StringUtils.capitalize(type));
        } catch (MissingResourceException ex) {
        }
        return getDisplayTitle();
    }

    public String getListenDownload() {
        String kla = getResolvedPrimaryUnitDisplayName();
        if (kla == null) {
            kla = unit.getId();
        }
        final String jahr = Integer.toString((Integer) term.getParameter(NdsTerms.JAHR));
        final int hj = (Integer) term.getParameter(NdsTerms.HALBJAHR);
        final String file = NbBundle.getMessage(PrimaryUnit.class, "primaryUnits.menu.download.allelisten.filename", kla, jahr, hj, new Date());
        return "zgnsrv/" + file + "?" + "document=betula.primaryUnit.allLists&unit.id=" + getUnitIdEncoded() + "&unit.authority=" + getUnitAuthorityEncoded();
    }

    public String exportCsv(final String enc) {
        String kla = getResolvedPrimaryUnitDisplayName();
        if (kla == null) {
            kla = unit.getId();
        }
        final String jahr = Integer.toString((Integer) term.getParameter(NdsTerms.JAHR));
        final int hj = (Integer) term.getParameter(NdsTerms.HALBJAHR);
        final String file = NbBundle.getMessage(PrimaryUnit.class, "primaryUnits.menu.export.csv.filename", kla, jahr, hj, new Date());
        String ret = "zgnsrv/" + file + "?" + "document=betula.primaryUnit.export.csv&unit.id=" + getUnitIdEncoded() + "&unit.authority=" + getUnitAuthorityEncoded();
        if (enc != null && !enc.isEmpty()) {
            ret += "&encoding=" + enc;
        }
        return ret;
    }

    public String getDetailsDownload() {
        String kla = getResolvedPrimaryUnitDisplayName();
        if (kla == null) {
            kla = unit.getId();
        }
        final String jahr = Integer.toString((Integer) term.getParameter(NdsTerms.JAHR));
        final int hj = (Integer) term.getParameter(NdsTerms.HALBJAHR);
        String file = NbBundle.getMessage(PrimaryUnit.class, "primaryUnits.menu.download.detailListen.filename", kla, jahr, hj, new Date());
        return "zgnsrv/" + file + "?" + "document=betula.primaryUnit.details&unit.id=" + getUnitIdEncoded() + "&unit.authority=" + getUnitAuthorityEncoded();
    }

    public String getZgnDownload() {
        String kla = getResolvedPrimaryUnitDisplayName();
        if (kla == null) {
            kla = unit.getId();
        }
        final String jahr = Integer.toString((Integer) term.getParameter(NdsTerms.JAHR));
        final int hj = (Integer) term.getParameter(NdsTerms.HALBJAHR);
        final String file = NbBundle.getMessage(PrimaryUnit.class, "primaryUnits.menu.download.allezgn.filename", kla, jahr, hj, new Date());
        return "zgnsrv/" + file + "?" + "unit.id=" + getUnitIdEncoded() + "&unit.authority=" + getUnitAuthorityEncoded();
    }

    public Map<String, Map<MultiSubject, Set<DocumentId>>> getDocMap() {
        if (docTypes == null) {
            docTypes = application.getDocumentMapper().getDocMap(getDocs(), false);
        }
        return docTypes;
    }

    public List<MultiSubject> getSubjects() {
        if (subjects == null) {
            subjects = new ArrayList<>();
            final Comparator<Subject> cmp = application.getSubjectComparator();
            final Comparator<MultiSubject> arrCmp = Comparator.comparing(ms -> ms.getSubjectMarkerSet().stream().map(m -> new Subject(m, ms.getRealmMarker())).min(cmp).get(), cmp);
            subjects = getDocMap().values().stream()
                    .map(Map::keySet)
                    .flatMap(Set::stream)
                    .filter(ms -> !ms.getSubjectMarkerSet().isEmpty())
                    .distinct()
                    .sorted(arrCmp)
                    .collect(Collectors.toList());
//            Collections.sort(subjects, arrCmp);
        }
        return subjects;
    }

    @Override
    protected HashSet<StudentId> createStudents() {
        studIds = new HashSet<>();
        studIds.addAll(application.getStudents()); //Arrays.asList(unitDocumentBeanRemote.getStudents(unit)));
        return studIds;
    }

    @Override
    protected AvailableStudent createAvailableStudent(StudentId sid, String dirName, Marker sgl) {
        return new AvailableStudentExt(sid, dirName, sgl);
    }

    private Collection<DocumentId> getDocs() {
        if (documents == null) {
            documents = application.getTargetAssessmentDocuments(unit); //unitDocumentBeanRemote.getTargetAssessmentDocuments(unit);
        }
        return documents;
    }

    public GradeValue getGradeValueAt(AvailableStudent stud, MultiSubject subject) {
        final String docType = getEditingDocType();
        return gradeValueForDocType(stud.getId(), subject, docType);
    }

    public GradeValue gradeValueForDocType(StudentId stud, MultiSubject subject, String docType) {
        final Map<MultiSubject, GradeValue> gr = gradeMap
                .computeIfAbsent(docType, k -> new HashMap<>())
                .computeIfAbsent(stud, k -> new HashMap<>());
        GradeValue ret = gr.get(subject);
        if (ret == null) {
            String msg = null;
            TermId tid = term.getScheduledItemId();
            if ("vorzensuren".equals(docType)) {
                docType = "zeugnisnoten";
                tid = new TermId(tid.getAuthority(), tid.getId() - 1);
            }
            final Set<DocumentId> docs = getDocMap().get(docType).get(subject);
            DocumentId id = null;
            if (docs != null && !docs.isEmpty()) {
                Set<FastTermTargetDocument> fdocs = docs.stream().map(d -> application.getFastDocument(d)).collect(Collectors.toSet());
                try {
                    id = application.getDocumentMapper().find(fdocs, stud, tid);
                } catch (AmbiguousDocumentCollectionException ex) {
                    msg = ex.getDocuments().length + " Werte!";
                }
            }
            final Entry initial = id != null ? application.getFastDocument(id).selectEntry(stud, tid) : null;
            ret = new GradeValue(application, id, tid, stud, null, initial, msg, false, docType, subject);
            gr.put(subject, ret);
        }
        return ret;
    }

    public StudentId getSelectedStudent() {
        return selectedStudent;
    }

    public void onRowToggle(final ToggleEvent event) {
        final AvailableStudent s = (AvailableStudent) event.getData();
        if (event.getVisibility() == Visibility.VISIBLE) {
            selectedStudent = s.getId();
        } else {
            selectedStudent = null;
        }
    }

    private ReportsBean lookupReportsBeanImplRemote() {
        try {
            final Context c = new InitialContext();
            return (ReportsBean) c.lookup("java:global/Betula_Server/Betula_Persistence/ReportsBeanImpl!org.thespheres.betula.server.beans.ReportsBean");
        } catch (NamingException ne) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
            throw new RuntimeException(ne);
        }
    }

//    private ZeugnisBean lookupZeugnisBeanImplRemote() {
//        try {
//            Context c = new InitialContext();
//            return (ZeugnisBean) c.lookup("java:global/Betula_Persistence/ZeugnisBeanImpl!org.thespheres.betula.niedersachsen.admin.zgn.ZeugnisBean");
//        } catch (NamingException ne) {
//            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
//            throw new RuntimeException(ne);
//        }
//    }
    private NdsFormatter lookupFOPFormatterBean() {
        try {
            final Context c = new InitialContext();
//            return (NdsFormatter) c.lookup("java:global/Betula_Web/FOPFormatter!org.thespheres.betula.web.docsrv.NdsFormatter");
            return (NdsFormatter) c.lookup("java:module/FOPFormatter");
        } catch (NamingException ne) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
            throw new RuntimeException(ne);
        }
    }

    public class AVSVWrapper extends AbstractGradeWrapper {

        private final DocumentId zgnDoc;
        private Grade grade;
        private boolean invalidated = true;
        private final Object INVALIDATED_LOCK = new Object();

        public AVSVWrapper(String convention, DocumentId zgnDoc) {
            super(GradeFactory.findConvention(convention));
            this.zgnDoc = zgnDoc;
        }

        @Override
        protected Grade getGrade() {
            if (invalidated) {
                synchronized (INVALIDATED_LOCK) {
                    grade = zeugnisBean.getKopfnote(zgnDoc, getConventions()[0].getName());
                    invalidated = false;
                }
            }
            return grade;
        }

        @Override
        protected void setGrade(Grade g) {
            if (!Objects.equals(g, this.grade)) {
                try {
                    boolean result = zeugnisBean.setKopfnote(zgnDoc, getConventions()[0].getName(), g);
                    if (result) {
                        this.grade = g;
                    }
                } catch (TermReportDataException ex) {
                    Logger.getLogger(PrimaryUnit.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
                }
            }
        }

        @Override
        protected Grade resolveReference(GradeReference proxy) {
            return proxy;
        }

        @Override
        protected boolean invalidateGrade(Grade newValue, Timestamp time) {
            synchronized (INVALIDATED_LOCK) {
                invalidated = true;
            }
            return true;
        }

        @Override
        protected boolean invalidateTickets() {
            return false;
        }

    }

    public class AvailableStudentExt extends AvailableStudent {

        private ArrayList<Grade> grades;
        private AVSVWrapper avGrade;
        private AVSVWrapper svGrade;
        private DocumentId[] zeugnisId;
        private boolean invalidatedFehltage = true;
        private String fehltage;
        private String unentschuldigt;
        private String fehltagePrecedingTerm;
        private Map<String, TextFieldHolder> textFieldValues;
        private Boolean displayPrecedingFehltage;

        public AvailableStudentExt(StudentId sid, String dirName, Marker sgl) {
            super(sid, dirName, sgl);
        }

        PrimaryUnit getPrimaryUnit() {
            return PrimaryUnit.this;
        }

        private void updateFehltage() {
            if (invalidatedFehltage) {
                final Integer vf = zeugnisBean.getIntegerValue(getZeugnisId(), ReportsBean.TYPE_FEHLTAGE);
                final Integer vu = zeugnisBean.getIntegerValue(getZeugnisId(), ReportsBean.TYPE_FEHLTAGE);
                fehltage = Optional.ofNullable(vf)
                        .map(i -> Integer.toString(i))
                        .orElse(null);
                unentschuldigt = Optional.ofNullable(vu)
                        .map(i -> Integer.toString(i))
                        .orElse(null);
                displayPrecedingFehltage = Optional.ofNullable(termBefore.getParameter("halbjahr"))
                        .filter(Integer.class::isInstance)
                        .map(hj -> ((int) hj) == 1)
                        .orElse(false);
                if (displayPrecedingFehltage) {
                    final DocumentId zeugnisIdPrecedingTerm = getZeugnisIdPrecedingTerm();
                    final Integer vfp = Optional.ofNullable(zeugnisIdPrecedingTerm)
                            .map(d -> zeugnisBean.getIntegerValue(d, ReportsBean.TYPE_FEHLTAGE))
                            .orElse(null);
                    final Integer vup = Optional.ofNullable(zeugnisIdPrecedingTerm)
                            .map(d -> zeugnisBean.getIntegerValue(d, ReportsBean.TYPE_UNENTSCHULDIGT))
                            .orElse(null);
                    final String termDn = Optional.ofNullable(termBefore.getParameter("schuljahr"))
                            .map(Object::toString)
                            .orElse(termBefore.getDisplayName());
                    if (vfp == null) {
                        fehltagePrecedingTerm = NbBundle.getMessage(BetulaWebApplication.class, "primunit.fehltagePrecedingTerm.null", termDn);
                    } else if (vup == null) {
                        fehltagePrecedingTerm = NbBundle.getMessage(BetulaWebApplication.class, "primunit.fehltagePrecedingTerm.noUnentschuldigt", termDn, vfp);
                    } else {
                        fehltagePrecedingTerm = NbBundle.getMessage(BetulaWebApplication.class, "primunit.fehltagePrecedingTerm", termDn, vfp, vup);
                    }
                } else {
                    fehltagePrecedingTerm = null;
                }
                invalidatedFehltage = false;
            }
        }

        public boolean isDisplayPrecedingFehltage() {
            return displayPrecedingFehltage;
        }

        public String getFehltagePrecedingTerm() {
            updateFehltage();
            return fehltagePrecedingTerm;
        }

        public String getFehltage() {
            updateFehltage();
            return fehltage;
        }

        public void setFehltage(String value) {
            if (!Objects.equals(value, this.fehltage)) {
                Integer val = null;
                try {
                    if (value != null && !value.trim().isEmpty()) {
                        try {
                            val = Integer.parseInt(value);
                        } catch (final NumberFormatException nfex) {
                            return;
                        }
                    }
                    boolean result = zeugnisBean.setIntegerValue(getZeugnisId(), ReportsBean.TYPE_FEHLTAGE, val);
                    if (result) {
                        this.fehltage = val == null ? "" : Integer.toString(val);
                        if (val == null) {
                            invalidatedFehltage = true;
                        }
                    }
                } catch (final TermReportDataException ex) {
                    Logger.getLogger(PrimaryUnit.class.getName()).log(Level.WARNING, ex.getLocalizedMessage(), ex);
                }
            }
        }

        public String getUnentschuldigt() {
            updateFehltage();
            return unentschuldigt;
        }

        public void setUnentschuldigt(String value) {
            if (!Objects.equals(value, this.unentschuldigt)) {
                Integer val = null;
                try {
                    if (value != null && !value.trim().isEmpty()) {
                        try {
                            val = Integer.parseInt(value);
                        } catch (final NumberFormatException nfex) {
                            return;
                        }
                    }
                    boolean result = zeugnisBean.setIntegerValue(getZeugnisId(), ReportsBean.TYPE_UNENTSCHULDIGT, val);
                    if (result) {
                        this.unentschuldigt = val == null ? "" : Integer.toString(val);
                        if (val == null) {
                            invalidatedFehltage = true;
                        }
                    }
                } catch (final TermReportDataException ex) {
                    Logger.getLogger(PrimaryUnit.class.getName()).log(Level.WARNING, ex.getLocalizedMessage(), ex);
                }
            }
        }

        public AVSVWrapper getArbeitsverhalten() {
            if (avGrade == null) {
                avGrade = new AVSVWrapper(ASVAssessmentConvention.AV_NAME, getZeugnisId());
            }
            return avGrade;
        }

        public String getAvReason() {
            return zeugnisBean.getNote(getZeugnisId(), ASVAssessmentConvention.AV_NAME);
        }

        public void setAvReason(String svReason) {
            final String text = Util.trimToNull(svReason);
            zeugnisBean.setNote(getZeugnisId(), ASVAssessmentConvention.AV_NAME, text);
        }

        public boolean isAvReasonEnabled() {
            final String p = application.getWebUIConfiguration().getProperty("avsv.reason.disabled");
            final boolean disabled = Boolean.parseBoolean(p);
            //TODO: use NdsReportBuilderFactory
            return !disabled && ReportProvisionsUtil.requireAvSvReason(getArbeitsverhalten().getGrade());
        }

        public AVSVWrapper getSozialverhalten() {
            if (svGrade == null) {
                svGrade = new AVSVWrapper(ASVAssessmentConvention.SV_NAME, getZeugnisId());
            }
            return svGrade;
        }

        public String getSvReason() {
            return zeugnisBean.getNote(getZeugnisId(), ASVAssessmentConvention.SV_NAME);
        }

        public void setSvReason(String svReason) {
            final String text = Util.trimToNull(svReason);
            zeugnisBean.setNote(getZeugnisId(), ASVAssessmentConvention.SV_NAME, text);
        }

        public boolean isSvReasonEnabled() {
            final String p = application.getWebUIConfiguration().getProperty("avsv.reason.disabled");
            final boolean disabled = Boolean.parseBoolean(p);
            //TODO: use NdsReportBuilderFactory
            return !disabled && ReportProvisionsUtil.requireAvSvReason(getSozialverhalten().getGrade());
        }

        public boolean isNotesEnabled() {
            final String p = application.getWebUIConfiguration().getProperty("report.notes.dialog.enabled");
            final boolean enabled = Boolean.parseBoolean(p);
            return enabled;
        }

        public String getNotes() {
            final ReportsBean.CustomNote[] cn = zeugnisBean.getCustomNotes(getZeugnisId());
            return Arrays.stream(cn)
                    .sorted(Comparator.comparingInt(ReportsBean.CustomNote::getPosition))
                    .map(ReportsBean.CustomNote::getValue)
                    .collect(Collectors.joining(". "));
        }

        public void setNotes(final String value) {
            final String text = Util.trimToNull(value);
            ReportsBean.CustomNote[] cn = new ReportsBean.CustomNote[]{new ReportsBean.CustomNote(WEB_UI_EDITABLE_REPORT_NOTE_POSITION, text)};
            zeugnisBean.setCustomNotes(getZeugnisId(), cn);
        }

        public String[] getReportTextFieldIds() {
            return ReportProvisionsUtil.textFields(getPrimaryUnitLevel(), null);
        }

        public String getPrimaryUnitTextFieldLabel(final String id) {
            return ReportProvisionsUtil.getTextFieldLabel(id);
        }

        public Map<String, TextFieldHolder> getTextFieldValues() {
            if (textFieldValues == null) {
                textFieldValues = Arrays.stream(getReportTextFieldIds())
                        .map(TextFieldHolder::new)
                        .collect(Collectors.toMap(h -> h.key, h -> h));
            }
            return this.textFieldValues;
        }

//        public void showReportNotesDialog() {
//            final Map<String, List<String>> params = new HashMap<>();
//            final List<String> zgnId = Arrays.asList(getZeugnisId().getAuthority(), getZeugnisId().getId(), getZeugnisId().getVersion().getVersion());
//            params.put(ConfigureReportNotes.PARAMETER_ZEUGNIS_ID, zgnId);
//            final List<String> unitId = Arrays.asList(unit.getAuthority(), unit.getId());
//            params.put(ConfigureReportNotes.PARAMETER_UNIT_ID, unitId);
//            final Map<String, Object> options = new HashMap<>();
//            options.put("modal", true);
//            options.put("contentHeight", 320);
//            options.put("contentWidth", 640); ////hint: available options are modal, draggable, resizable, width, height, contentWidth and contentHeight
//            RequestContext.getCurrentInstance().openDialog("content/reportNotesConfig", options, params);
//        }
        public List<Grade> getAVSVGrades() {
            if (grades == null) {
                final AssessmentConvention con = GradeFactory.findConvention(ASVAssessmentConvention.AV_NAME);
                final Grade[] all = con.getAllGrades();
                grades = new ArrayList<>(Arrays.asList(all));
            }
            return grades;
        }

        public String getZeugnisAuthorityEncoded() {
            try {
                return URLEncoder.encode(getZeugnisId().getAuthority(), "utf-8");
            } catch (UnsupportedEncodingException ex) {
                return null;
            }
        }

        public String getZeugnisIdEncoded() {
            try {
                return URLEncoder.encode(getZeugnisId().getId(), "utf-8");
            } catch (UnsupportedEncodingException ex) {
                return null;
            }
        }

        public boolean isReportConfigurationEnabled() {
            //TODO: check zeungisDocument if Ausgabe past....
            return !"vorzensuren".equals(getEditingDocType()) && true;
        }

        public DocumentId getZeugnisId() {
            if (zeugnisId == null) {
                zeugnisId = new DocumentId[2];
                final DocumentId[] result = zeugnisBean.findTermReports(getId(), term.getScheduledItemId(), true);
                zeugnisId[1] = result[0]; //fehlt: check if more than one!!!!!!!!1
                final DocumentId[] resultB = zeugnisBean.findTermReports(getId(), termBefore.getScheduledItemId(), false);
                zeugnisId[0] = resultB.length == 1 ? resultB[0] : null;
            }
            return zeugnisId[1];
        }

        public DocumentId getZeugnisIdPrecedingTerm() {
            getZeugnisId();
            return zeugnisId[0];
        }

        public class TextFieldHolder {

            private final String key;

            TextFieldHolder(String key) {
                this.key = key;
            }

            public String getValue() {
                return zeugnisBean.getNote(getZeugnisId(), key);
            }

            public void setValue(final String value) {
                final String text = Util.trimToNull(value);
                zeugnisBean.setNote(getZeugnisId(), key, text);
            }
        }
    }

}
