/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import org.apache.commons.lang3.StringUtils;
import org.openide.util.NbBundle;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;
import org.thespheres.betula.assess.GradeFactory;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.assess.AssessmentConvention;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeParsingException;
import org.thespheres.betula.assess.GradeReference;
import org.thespheres.betula.assess.TargetDocument;
import org.thespheres.betula.services.jms.MultiTargetAssessmentEvent;
import org.thespheres.betula.services.jms.MultiTargetAssessmentEvent.Update;
import org.thespheres.betula.services.jms.TicketEvent;
import org.thespheres.betula.services.jms.TicketEvent.TicketEventType;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.model.AssessmentDecoration;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.niedersachsen.NdsTerms;
import org.thespheres.betula.niedersachsen.vorschlag.VorschlagDecoration;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.FastTermTargetDocument.Entry;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.util.CollectionUtil;
import org.thespheres.betula.web.AvailableTarget.AvailableTermColumn;
import org.thespheres.betula.web.config.ExtraAnnotation;

/**
 *
 * @author boris.heithecker
 */
public class AvailableTarget extends AbstractData<AvailableTermColumn> {

    public String value;
    private final Set<DocumentId> docs = new HashSet<>();
    private final Map<StudentId, Map<AvailableTermColumn, GradeValue>> gradeValues = new HashMap<>();
    private final Map<StudentId, Map<CrossMarkSubject, GradeValue>> crossMarkGradeValues = new HashMap<>();
    private ArrayList<AvailableTermColumn> gradeColumns;
    private HashMap<String, AvailableTermColumn> editableGradeColumns;
//    private final DocumentId base;
//    private boolean dirty;
    private DocumentId[] target;
    private boolean joinTargets = true;
    private String csvEncoding;
    private List<CrossMarkSubject> crossMarkSubjects = new ArrayList<>();
    private String entitlementTitle = null;
    private String signeeTypeTitle;
    private DocumentId commentsDoc;

    AvailableTarget(final String displayName, final BetulaWebApplication app) {
        super(app, displayName);
    }

    Set<DocumentId> getDocuments() {
        return docs;
    }

    protected String getEntitlementTitle() {
        return entitlementTitle;
    }

    void setEntitlementTitle(final String entitlementTitle) {
        this.entitlementTitle = entitlementTitle;
    }

    @Override
    public String getDisplayTitle() {
        return super.getDisplayTitle() + getSigneeTypeTitle();
    }

    protected String getSigneeTypeTitle() {
        if (signeeTypeTitle == null) {
            final String defaultTargetType = application.getWebUIConfiguration().getDefaultCommitTargetType();
            final String[] st = Stream.concat(docs.stream(), crossMarkSubjects.stream().map(CrossMarkSubject::getDocument))
                    .filter(d -> application.getDocumentsModel().getSuffix(d) != null)//May be null if custom suffix not listed in model
                    .filter(d -> application.getDocumentsModel().getSuffix(d).equals(defaultTargetType))
                    .flatMap(d -> application.getFastDocument(d).getSignees().keySet().stream())
                    .toArray(String[]::new);
            final StringJoiner sj = new StringJoiner(", ", " (", ")");
            sj.setEmptyValue("");
            Arrays.stream(st)
                    .filter(t -> !t.equals("entitled.signee"))
                    .map(type -> {
                        try {
                            return NbBundle.getMessage(AvailableTarget.class, "availableTarget.otherSigneeType.displayInfo." + type);
                        } catch (MissingResourceException mrex) {
                            return type;
                        }
                    })
                    .forEach(sj::add);
            signeeTypeTitle = sj.toString();
        }
        return signeeTypeTitle;
    }

    public Term getEditTerm() {
        return application.getCurrentTerm();
    }

    public TermId getEditTermId() {
        return application.getCurrentTerm().getScheduledItemId();
    }

    public DocumentId getTargetDocument() {
        if (target == null) {
            final DocumentId[] arr = docs.stream()
                    .filter(d -> d.getId().endsWith(application.getWebUIConfiguration().getDefaultCommitTargetType()))
                    .toArray(DocumentId[]::new);
            if (arr.length != 1) {
                StringJoiner sj = new StringJoiner(", ");
                Arrays.stream(arr).map(DocumentId::getId).forEach(sj::add);
                application.getLogger().log(Level.INFO, "Editing target type must be one single document in AvailableTarget. Found: {0} ({1})", new Object[]{arr.length, sj.toString()});
//                throw new IllegalStateException("Editing target type must be one single document in AvailableTarget. Found: " + arr.length + " (" + sj.toString() + ")");
            }
            target = arr;
        }
        return target.length != 0 ? target[0] : null;
    }

    public String getPdfUrl() {
        final DocumentId tid = getTargetDocument();
        if (tid == null) {
            return null;
        }
        String jahr = Integer.toString((Integer) getEditTerm().getParameter(NdsTerms.JAHR));
        int hj = (Integer) getEditTerm().getParameter(NdsTerms.HALBJAHR);
        String file = NbBundle.getMessage(PrimaryUnit.class, "availableTarget.download.target.filename.pdf", getDisplayTitle(), jahr, hj, new Date());
        String ret = "/zgnsrv/" + file + "?"
                + "document=betula.target&term.id=" + getTermIdEncoded() + "&term.authority=" + getTermAuthorityEncoded()
                + "&document.id=" + getDocumentIdEncoded(tid) + "&document.authority=" + getDocumentAuthorityEncoded(tid) + "&document.version=" + getDocumentVersionEncoded(tid)
                //                + "&unit.id=" + getUnitIdEncoded() + "&unit.authority=" + getUnitAuthorityEncoded()
                + "&mime=application/pdf";
        if (hasJoinedTargets()) {
            ret += "&joinTargets=" + Boolean.toString(joinTargets);
        }
        return ret;
    }

    public String getCsvUrl() {
        final DocumentId tid = getTargetDocument();
        if (tid == null) {
            return null;
        }
        String jahr = Integer.toString((Integer) getEditTerm().getParameter(NdsTerms.JAHR));
        int hj = (Integer) getEditTerm().getParameter(NdsTerms.HALBJAHR);
        String file = NbBundle.getMessage(PrimaryUnit.class, "availableTarget.download.target.filename.csv", getDisplayTitle(), jahr, hj, new Date());
        String ret = "zgnsrv/" + file + "?"
                + "document=betula.target&term.id=" + getTermIdEncoded() + "&term.authority=" + getTermAuthorityEncoded()
                + "&document.id=" + getDocumentIdEncoded(tid) + "&document.authority=" + getDocumentAuthorityEncoded(tid) + "&document.version=" + getDocumentVersionEncoded(tid);
//                + "&unit.id=" + getUnitIdEncoded() + "&unit.authority=" + getUnitAuthorityEncoded();
        final String enc;
        if ((enc = getCsvEncoding()) != null) {
            ret += "&encoding=" + enc;
        }
        ret += "&mime=text/csv";
        if (hasJoinedTargets()) {
            ret += "&joinTargets=" + Boolean.toString(joinTargets);
        }
        return ret;
    }

    public String getCsvEncoding() {
        return csvEncoding;
    }

    public void setCsvEncoding(String value) {
        csvEncoding = value;
    }

    public boolean hasJoinedTargets() {
        final DocumentId t = getTargetDocument();
        return t != null && false; //application.getJoinedUnits(t) != null;
    }

    public boolean isJoinTargets() {
        return joinTargets;
    }

    public void setJoinTargets(boolean joinTargets) {
        this.joinTargets = joinTargets;
    }

    public void joinTargetsChecked() {//Do not remove, without ajax event joinTargets is not updated properly
    }

    public String getTermAuthorityEncoded() {
        try {
            return URLEncoder.encode(getEditTerm().getScheduledItemId().getAuthority(), "utf-8");
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    public String getTermIdEncoded() {
        try {
            String v = Integer.toString(getEditTerm().getScheduledItemId().getId());
            return URLEncoder.encode(v, "utf-8");
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    public static String getDocumentAuthorityEncoded(DocumentId id) {
        try {
            return URLEncoder.encode(id.getAuthority(), "utf-8");
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    public static String getDocumentIdEncoded(DocumentId id) {
        try {
            return URLEncoder.encode(id.getId(), "utf-8");
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    public static String getDocumentVersionEncoded(DocumentId id) {
        try {
            return URLEncoder.encode(id.getVersion().getVersion(), "utf-8");
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

//    public String getUnitIdEncoded() {
//        try {
//            return URLEncoder.encode(base.getId(), "utf-8");
//        } catch (UnsupportedEncodingException ex) {
//            return null;
//        }
//    }
//
//    public String getUnitAuthorityEncoded() {
//        try {
//            return URLEncoder.encode(base.getAuthority(), "utf-8");
//        } catch (UnsupportedEncodingException ex) {
//            return null;
//        }
//    }
    @Override
    protected HashSet<StudentId> createStudents() {
        final HashSet<StudentId> studs = new HashSet<>();
        getEditableTermColumns().values().stream()
                .map(t -> t.docId)
                .forEach(id -> studs.addAll(application.getFastDocument(id).getStudents(getEditTerm().getScheduledItemId()))); //fehlt: aus editableTarget, sonst gibt es bei leeren Vornoten keinen Zeilen!
        getCrossMarkSubjects().stream()
                .map(CrossMarkSubject::getDocument)
                .forEach(id -> studs.addAll(application.getFastDocument(id).getStudents(getEditTerm().getScheduledItemId())));
        return studs;
    }

    @Override
    protected AvailableStudent createAvailableStudent(StudentId sid, String dirName, Marker sgl) {
        return new TargetStudent(sid, dirName, sgl);
    }

    public synchronized List<AvailableTermColumn> getTermColumns() {
        if (gradeColumns == null) {
            gradeColumns = new ArrayList<>();
            docs.stream().forEach(d -> {
                final FastTermTargetDocument fd = application.getFastDocument(d);
                if (fd != null) {
                    fd.getTerms().stream()
                            .filter(t -> addToRenderedTerms(t, d))
                            .forEach(t -> gradeColumns.add(new AvailableTermColumn(d, t, null, false, application.getWebUIConfiguration().getDefaultCommitTargetType())));
                }
            });
            Collections.sort(gradeColumns);
            if (gradeColumns.size() > 3) {//nicht mehr als 3 !!!, siehe terms.xhtml
                int remove = gradeColumns.size() - 3;
                for (int i = 0; i++ < remove;) {
                    gradeColumns.remove(0);
                }
            }
        }
        return gradeColumns;
    }

    public synchronized Map<String, AvailableTermColumn> getEditableTermColumns() {
        if (editableGradeColumns == null) {
            application.getWebUIConfiguration().getCommitTargetTypes();
            editableGradeColumns = new HashMap<>();
            for (DocumentId d : docs) {
                final FastTermTargetDocument fd = application.getFastDocument(d);
                if (fd != null) {
                    for (TermId t : fd.getTerms()) {
                        if (addToEditableTerms(t, d)) {
                            final String type = application.getDocumentsModel().getSuffix(d);
                            if (type != null) {
                                editableGradeColumns.put(type, new AvailableTermColumn(d, t, getEditTerm(), true, type));
                            }
                        }
                    }
                }
            }
        }
        return editableGradeColumns;
    }

    boolean addToRenderedTerms(final TermId t, final DocumentId d) {
        return !t.equals(getEditTerm().getScheduledItemId())
                && d.getId().endsWith(application.getWebUIConfiguration().getDefaultCommitTargetType())
                && getEditTerm().getScheduledItemId().getId() - t.getId() > 0; //only preceding terms
    }

    boolean addToEditableTerms(TermId t, DocumentId d) {
        return t.equals(getEditTerm().getScheduledItemId());
    }

    @Override
    protected Grade resolveReference(AvailableStudent stud, AvailableTermColumn term, GradeReference proxy) {
        VorschlagDecoration deco = term.getTargetDocoration();
        if (deco != null) {
            return deco.resolveReference(proxy, stud.getId(), term.getTermId(), null);
        }
        return proxy;
    }

    public GradeValue getGradeValueAt(AvailableStudent stud, AvailableTermColumn term) {
        if (term == null) {
            return null;
        }
        final StudentId sid = stud.getId();
        Map<AvailableTermColumn, GradeValue> gr = gradeValues.get(sid);
        if (gr == null) {
            gr = new HashMap<>();
            gradeValues.put(sid, gr);
        }
        GradeValue ret = gr.get(term);
        if (ret == null) {
            final Entry initial = application.getFastDocument(term.docId).selectEntry(stud.getId(), term.termId);
            ret = new GradeValue(application, term.docId, term.termId, stud.getId(), term.getPreferredConventions(), initial, null, term.mayEdit, term.targetType, null);
            gr.put(term, ret);
        }
        return ret;
    }

    public GradeValue getCrossMarkGradeValueAt(final AvailableStudent stud, final CrossMarkSubject cms) {
        if (cms == null) {
            return null;
        }
        final Term term = application.getCurrentTerm();
        final boolean mayEdit = true;
        final StudentId sid = stud.getId();
        final GradeValue ret = crossMarkGradeValues
                .computeIfAbsent(sid, s -> new HashMap<>())
                .computeIfAbsent(cms, v -> {
                    final Entry initial = application.getFastDocument(v.getDocument()).selectEntry(stud.getId(), term.getScheduledItemId());
                    return new GradeValue(application, v.getDocument(), term.getScheduledItemId(), stud.getId(), new AssessmentConvention[]{application.getCrossMarkAssessmentConvention()}, initial, null, mayEdit, application.getWebUIConfiguration().getDefaultCommitTargetType(), null);
                });
        return ret;
    }

    public String resolveCrossMarkGradeLabel(final Grade g) {
//        application.getCrossMarkGrades()
        return g.getLongLabel();
    }

    @Override
    protected void onTicketEvent(TicketEvent evt) {
        Ticket ticket = evt.getSource();
        boolean dirty = false;
        if (evt.getType().equals(TicketEventType.REMOVE)) {
            gradeValues.values().stream().forEach(m -> {
                m.values().stream().filter(g -> g.usesTicket(ticket)).forEach(g -> {
                    g.invalidateTickets();
                });
            });
            dirty = true;
        } else if (evt.getType().equals(TicketEventType.ADD)) {
            gradeValues.values().stream().forEach(m -> {
                m.values().stream().forEach(g -> g.invalidateTickets());
            });
            dirty = true;
        }
        if (dirty && shouldUpdate()) {
            EventBus eventBus = EventBusFactory.getDefault().eventBus();
            BetulaPushMessage message = new BetulaPushMessage();
            message.setSource(dataTableClientId);
            message.setUpdate(dataTableClientId);
            eventBus.publish(NotifyGradeUpdateResource.CHANNEL_BASE + application.getUser().getSignee().getId(), message);
        }
    }

    @Override
    protected void onDocumentEvent(MultiTargetAssessmentEvent<TermId> evt) {
        if (evt.getUpdates() == null) {
            //The whole document has changed, been removed --> we have to update the whole view
            //TODO: consider a newly created document---?
            if (docs.contains(evt.getSource()) && shouldUpdate()) {
                EventBus eventBus = EventBusFactory.getDefault().eventBus();
                BetulaPushMessage message = new BetulaPushMessage();
                message.setSource(dataTableClientId);
                message.setUpdate(dataTableClientId);
                eventBus.publish(NotifyGradeUpdateResource.CHANNEL_BASE + application.getUser().getSignee().getId(), message);
            } else {
                return;
            }
        }
        for (Update<?> u : evt.getUpdates()) {
            Map<AvailableTermColumn, GradeValue> m = gradeValues.get(u.getStudent());
            GradeValue gv = null;
            String termDN = null;
            if (m != null) {
                for (Map.Entry<AvailableTermColumn, GradeValue> e : m.entrySet()) {
                    DocumentId di = e.getKey() != null ? e.getKey().docId : null;
                    TermId ti = e.getKey() != null ? e.getKey().termId : null;
                    if (di == null || ti == null || u.getGradeId() == null) {
                        continue;
                    }
                    if (di.equals(evt.getSource()) && ti.equals(u.getGradeId())) {
                        gv = e.getValue();
                        termDN = e.getKey().getDisplayName();
                        break;
                    }
                }
            }
            if (gv != null) {
                Grade evtGrade = u.getValue();
//            dirty = gv.checkIsOwnGradeUpdate(evtGrade);
                boolean dirty = gv.invalidateGrade(evtGrade, evt.getTimestamp().getValue());
                if (dirty && shouldUpdate()) {
                    String g = evtGrade.getShortLabel();
                    AvailableStudent as = findStudent(gv.getStudent());
                    String n = as != null ? as.getFullname() : null;
                    String msg = NbBundle.getMessage(AvailableTarget.class, "target.update.message");
                    String det = NbBundle.getMessage(AvailableTarget.class, "target.update.message.detail", g, n, termDN);
                    EventBus eventBus = EventBusFactory.getDefault().eventBus();
                    BetulaPushMessage message = new BetulaPushMessage(FacesMessage.SEVERITY_INFO, msg, det);
                    message.setSource(dataTableClientId);
                    message.setUpdate(dataTableClientId);
                    eventBus.publish(NotifyGradeUpdateResource.CHANNEL_BASE + application.getUser().getSignee().getId(), message);
                }
            }
        }
    }

    protected boolean shouldUpdate() {
//        return dirty && application.getCurrentPage().equals("terms") && isActiveTab() && dataTableClientId != null;
        return application.getCurrentPage().equals("terms") && isActiveTab() && dataTableClientId != null;

    }

    public boolean hasComments() {
        if (this.commentsDoc == null) {
            final String sfx = this.application.getWebUIConfiguration().getProperty("targets.comments.suffix");
            this.commentsDoc = this.docs.stream()
                    .filter(d -> sfx != null && sfx.equals(this.application.getDocumentsModel().getSuffix(d)))
                    .collect(CollectionUtil.singleton())
                    .orElse(DocumentId.NULL);
            application.getLogger().log(Level.FINE, "AvailableTarget.hasComments Comment doc: {0}", commentsDoc.toString());
        }
        return this.commentsDoc != null;
    }

    public boolean isCrossMarksEnabled() {
        return !crossMarkSubjects.isEmpty();
    }

    public List<CrossMarkSubject> getCrossMarkSubjects() {
        return crossMarkSubjects;
    }

    void addCrossMarksDocument(final DocumentId doc, final Marker sub) {
        crossMarkSubjects.add(new CrossMarkSubject(doc, sub));
    }

    public class TargetStudent extends AvailableStudent {

        private String comment;
        private boolean commentsEnabled = true;

        TargetStudent(final StudentId sid, final String dirName, final Marker sgl) {
            super(sid, dirName, sgl);
        }

        public String getComment() {
            if (comment == null) {
                final FastTextTermTargetDocument fttd = application.getFastTextDocument(commentsDoc);
                comment = fttd.select(getId(), application.getCurrentTerm().getScheduledItemId()).stream()
                        .filter(e -> e.getSection() == null)
                        .collect(CollectionUtil.singleton())
                        .map(FastTextTermTargetDocument.Entry::getText)
                        .orElse("");
                application.getLogger().log(Level.FINE, "TargetStudent.getComment Found comment: {0}", comment);
            }
            return comment;
        }

        public void setComment(final String value) {
            final String cmnt = StringUtils.stripToNull(value);
            if (!(cmnt == null && StringUtils.isBlank(comment))) {
                final boolean res = application.submitText(commentsDoc, application.getCurrentTerm().getScheduledItemId(), null, getId(), cmnt);
                if (res) {
                    this.comment = cmnt;
                }
            }
        }

        public boolean isCommentsEnabled() {
            return commentsEnabled;
        }

        public void setCommentsEnabled(boolean commentsEnabled) {
            this.commentsEnabled = commentsEnabled;
        }

    }

    public class AvailableTermColumn implements Comparable<AvailableTermColumn>, Converter {

        private final DocumentId docId;
        private final TermId termId;
        private String displayName;
        private Term term;
        private List<Grade> grades;
        private AssessmentConvention[] currentConvention;
        private final boolean mayEdit;
        private final String targetType;

        public AvailableTermColumn(DocumentId docId, TermId termId, Term term, boolean mayEdit, String targetType) {
            this.docId = docId;
            this.termId = termId;
            this.term = term;
            this.mayEdit = mayEdit;
            this.targetType = targetType;
        }

        public String getDisplayName() {
            if (displayName == null) {
                String jahr = Integer.toString((Integer) getTerm().getParameter(NdsTerms.JAHR));
                int j = Integer.parseInt(jahr);
                if (j >= 2000) {
                    j = j - 2000;
                } else {
                    j = j - 1900;
                }
                int hj = (Integer) getTerm().getParameter(NdsTerms.HALBJAHR);
                displayName = NbBundle.getMessage(PrimaryUnit.class, "target.term.displayName", j, ++j, hj);
            }
            return displayName;
        }

        public TermId getTermId() {
            return termId;
        }

        public synchronized AssessmentConvention[] getPreferredConventions() {
            if (currentConvention == null) {
                final List<AssessmentConvention> l = new ArrayList<>();
                final String con = application.getFastDocument(docId).getPreferredConvention();
                final AssessmentConvention pref = GradeFactory.findConvention(con);
                l.add(pref);
                final AssessmentDecoration deco = getTargetDocoration();
                if (deco != null) {
                    deco.getDecoration(docId, (TargetDocument) application.getFastDocument(docId)).stream()
                            .forEach(l::add);
                }
                currentConvention = l.stream()
                        .toArray(AssessmentConvention[]::new);
            }
            return currentConvention;
        }

        private VorschlagDecoration getTargetDocoration() {
            final ExtraAnnotation annot = new ExtraAnnotation(targetType);
            return application.getAssessmentDecoration(annot);
        }

        public synchronized List<Grade> getGrades() {
            if (grades == null) {
                final List<Grade> l = Arrays.stream(getPreferredConventions())
                        .flatMap(c -> Arrays.stream(c.getAllGradesReverseOrder()))
                        .collect(Collectors.toList());
                application.getExtraGrades().forEach(l::add);
                grades = l;
            }
            return grades;
        }

        public synchronized String[] getSigneeTypes() {
            return application.getFastDocument(docId).getSignees().keySet()
                    .stream()
                    .toArray(String[]::new);
        }

        @Override
        public Object getAsObject(FacesContext context, UIComponent component, String value) {
            try {
                Grade g = getPreferredConventions()[0].parseGrade(value);
                return g != null ? g.getId() : "";
            } catch (GradeParsingException ex) {
            }
            return "";
        }

        @Override
        public String getAsString(FacesContext context, UIComponent component, Object value) {
            if (value instanceof String) {
                final AssessmentConvention[] c = getPreferredConventions();
                Grade g = c[0].find((String) value);
                if (g != null) {
                    return g.getShortLabel();
                }
                if (c.length > 1) {
                    for (int i = 1; i < c.length; i++) {
                        g = c[i].find((String) value);
                        if (g != null) {
                            return g.getShortLabel();
                        }
                    }
                }
            }
            return "";
        }

        @Override
        public int compareTo(AvailableTermColumn o) {
            return this.termId.getId() - o.termId.getId();
        }

        private synchronized Term getTerm() {
            if (term == null) {
                try {
                    term = NdsTerms.fromId(termId);
                } catch (IllegalAuthorityException ex) {
                }
            }
            return term;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 89 * hash + Objects.hashCode(this.docId);
            hash = 89 * hash + Objects.hashCode(this.termId);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final AvailableTermColumn other = (AvailableTermColumn) obj;
            if (!Objects.equals(this.docId, other.docId)) {
                return false;
            }
            return Objects.equals(this.termId, other.termId);
        }

    }
}
