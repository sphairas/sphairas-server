/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import org.thespheres.betula.server.beans.AmbiguousResultException;
import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.openide.util.NbBundle;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.AbstractGrade;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeFactory;
import org.thespheres.betula.assess.GradeReference;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.document.model.MultiSubject;
import org.thespheres.betula.document.model.Subject;
import org.thespheres.betula.document.model.UnitsModel;
import org.thespheres.betula.niedersachsen.ASVAssessmentConvention;
import org.thespheres.betula.niedersachsen.Uebertrag;
import org.thespheres.betula.niedersachsen.vorschlag.AVSVVorschlag;
import org.thespheres.betula.niedersachsen.vorschlag.VorschlagDecoration;
import org.thespheres.betula.niedersachsen.xml.NdsZeugnisSchulvorlage;
import org.thespheres.betula.niedersachsen.xml.NdsZeugnisSchulvorlage.FontSizeValues;
import org.thespheres.betula.niedersachsen.xml.NdsZeugnisSchulvorlage.ListDefinition;
import org.thespheres.betula.niedersachsen.xml.NdsZeugnisSchulvorlage.Property;
import org.thespheres.betula.niedersachsen.zeugnis.NdsReportBuilder;
import org.thespheres.betula.niedersachsen.zeugnis.NdsReportBuilderFactory;
import org.thespheres.betula.niedersachsen.zeugnis.ReportProvisionsUtil;
import org.thespheres.betula.niedersachsen.zeugnis.listen.StudentDetailsXml;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.FastTermTargetDocument.Entry;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;
import org.thespheres.betula.server.beans.ReportsBean;
import org.thespheres.betula.server.beans.SigneeLocal;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.services.scheme.spi.TermNotFoundException;
import org.thespheres.betula.services.util.SigneeEntitlement;
import org.thespheres.betula.services.vcard.VCardStudent;
import org.thespheres.betula.util.CollectionUtil;
import org.thespheres.betula.web.PrimaryUnit;
import org.thespheres.betula.web.config.ExtraAnnotation;

/**
 *
 * @author boris.heithecker
 */
@Stateless
@LocalBean
public class NdsFormatDetailsBean {

    @EJB
    private ZensurensprungValidationBean zensurensprung;
    @EJB
    private VersetzungsValidationBean versetzung;
    @EJB
    private ZensurenschnittValidationBean schnitt;
    @EJB(beanName = "ReportsBeanImpl")
    private ReportsBean zeugnisBean;
    @EJB
    private DocumentMapper documentMapper;
    @Default
    @Inject
    private NamingResolver namingResolver;
    @EJB
    private NdsFormatter fOPFormatter;
    @Default
    @Inject
    private DocumentsModel docModel;
    @Any
    @Inject
    private Instance<VorschlagDecoration> extraAssessment;
    @Inject
    private BemerkungenBean bemBean;
    @Inject
    private NdsReportBuilderFactory factory;
    @Inject
    private SigneeLocal signees;

//    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
//    @RolesAllowed({"signee", "unitadmin"})
    public void oneStudent(final StudentDetailsXml details,
            final MappedStudent ms,
            UnitId pu,
            Term current,
            int preTermsCount,
            final Map<TermId, Map<String, Map<MultiSubject, Set<DocumentId>>>> docMap,
            final Map<DocumentId, FastTermTargetDocument> targetData,
            final Map<DocumentId, FastTermTargetDocument> agTargetData,
            final Map<TermId, Map<String, Map<MultiSubject, Set<DocumentId>>>> textDocMap,
            final Map<DocumentId, FastTextTermTargetDocument> textData,
            final String beforeTermLabel) {
    public void oneStudent(final StudentDetailsXml details, final MappedStudent ms, UnitId pu, Term current, int preTermsCountRequest, final Map<TermId, Map<String, Map<MultiSubject, Set<DocumentId>>>> docMap, final Map<DocumentId, FastTermTargetDocument> targetData, final Map<DocumentId, FastTermTargetDocument> agTargetData, final Map<TermId, Map<String, Map<MultiSubject, Set<DocumentId>>>> textDocMap, final Map<DocumentId, FastTextTermTargetDocument> textData, NdsZeugnisSchulvorlage.ListDefinition listDef) {
        final StudentId student = ms.getStudentId();
        final String sName = ms.getDisplayName();
        final Marker sgl = ms.getCareer();
//        final VCardStudent card = new VCardStudent(student);
//        card.setVCard(studentCardBean.get(student));
//        String sName = card.getFullName();
//        final Marker sgl = fOPFormatter.getStudentSGL(student, fOPFormatter.termEnd(current.getScheduledItemId()));
//        if (sgl != null) {
//            sName += " (" + sgl.getShortLabel().replace("KGS ", "") + ")";
//        }
//        final String lname = NbBundle.getMessage(PrimaryUnit.class, "primaryUnits.download.details.title", sName, kla, jahr, hj);
//        final StudentDetailsXml details = new StudentDetailsXml();
//        details.setListDate(ldate);
//        details.setListName(lname); 
//        details.setSortString(StudentComparator.sortStringFromDirectoryName(card.getDirectoryName()));
        //Subject-grade map for validation, keine fächerübergreifende Evaluierung ????
        final Map<TermId, Map<Subject, Grade>> zeugnisnoten = new HashMap<>();
//            final Map<String, <Grade, Integer>> avsvMap = new TreeMap((Comparator<Grade>) (g1, g2) -> collator.compare(g1.getShortLabel(), g2.getShortLabel()));
        int row = 0;
        int preTermsCount = Optional.ofNullable(listDef)
                .map(ListDefinition::getPreTermsCount)
                .orElse(preTermsCountRequest);
        for (int tc = -preTermsCount; tc <= -1; tc++) {
            final Term t;
            try {
                t = current.getSchedule().resolve(new TermId(current.getScheduledItemId().getAuthority(), current.getScheduledItemId().getId() + tc));
            } catch (TermNotFoundException | IllegalAuthorityException ex) {
                Logger.getLogger(NdsFormatter.class.getName()).log(Level.WARNING, ex.getLocalizedMessage(), ex);
                continue;
            }
            final Map<MultiSubject, Set<DocumentId>> query = docMap.get(t.getScheduledItemId()).get("zeugnisnoten");
            if (query != null && !query.isEmpty()) {
                StudentDetailsXml.TermDataLine l = details.addLine(row++, t.getDisplayName());
                Optional.ofNullable(listDef)
                        .map(ListDefinition::getFontSize)
                        .map(FontSizeValues::getTableCells)
                        .ifPresent(l::setLabelFontSize);
                oneAssessLine("zeugnisnoten", query, targetData, student, t, sName, sgl, current, zeugnisnoten, details, l, listDef);
            }
        }
        final List<String> targetTypes = Optional.ofNullable(listDef)
                .map(ListDefinition::getTargetTypes)
                .map(Arrays::asList)
                .orElse(Arrays.asList(new String[]{"quartalsnoten", "zeugnisnoten", "arbeitsverhalten", "sozialverhalten"}));
        final Map<MultiSubject, Set<DocumentId>> q = docMap.get(current.getScheduledItemId()).get("quartalsnoten");
        if (q != null && !q.isEmpty() && targetTypes.contains("quartalsnoten")) {
            String lbl = NbBundle.getMessage(PrimaryUnit.class, "primaryUnits.docTypes.quartalsnoten");
            StudentDetailsXml.TermDataLine lq = details.addLine(row++, lbl);
            Optional.ofNullable(listDef)
                    .map(ListDefinition::getFontSize)
                    .map(FontSizeValues::getTableCells)
                    .ifPresent(lq::setLabelFontSize);
            oneAssessLine("quartalsnoten", q, targetData, student, current, sName, sgl, current, null, details, lq, listDef);
        }
        final Map<MultiSubject, Set<DocumentId>> query = docMap.get(current.getScheduledItemId()).get("zeugnisnoten");
        if (query != null && targetTypes.contains("zeugnisnoten")) {
            final String rowName = preTermsCount == 0 ? "Zeugnisnoten" : current.getDisplayName();
            StudentDetailsXml.TermDataLine l = details.addLine(row++, rowName);
            Optional.ofNullable(listDef)
                    .map(ListDefinition::getFontSize)
                    .map(FontSizeValues::getTableCells)
                    .ifPresent(l::setLabelFontSize);
            oneAssessLine("zeugnisnoten", query, targetData, student, current, sName, sgl, current, zeugnisnoten, details, l, listDef);
        }
        final Map<MultiSubject, Set<DocumentId>> av = docMap.get(current.getScheduledItemId()).get("arbeitsverhalten");
        Map<Grade, Integer> avcount = null;
        if (av != null && !av.isEmpty() && targetTypes.contains("arbeitsverhalten")) {
            String lblav = NbBundle.getMessage(PrimaryUnit.class, "primaryUnits.docTypes.arbeitsverhalten");
            StudentDetailsXml.TermDataLine lav = details.addLine(row++, lblav);
            Optional.ofNullable(listDef)
                    .map(ListDefinition::getFontSize)
                    .map(FontSizeValues::getTableCells)
                    .ifPresent(lav::setLabelFontSize);
            avcount = oneAssessLine("arbeitsverhalten", av, targetData, student, current, sName, sgl, current, null, details, lav, listDef);
        }
        final Map<MultiSubject, Set<DocumentId>> sv = docMap.get(current.getScheduledItemId()).get("sozialverhalten");
        Map<Grade, Integer> svcount = null;
        if (sv != null && !sv.isEmpty() && targetTypes.contains("sozialverhalten")) {
            String lblsv = NbBundle.getMessage(PrimaryUnit.class, "primaryUnits.docTypes.sozialverhalten");
            StudentDetailsXml.TermDataLine lsv = details.addLine(row++, lblsv);
            Optional.ofNullable(listDef)
                    .map(ListDefinition::getFontSize)
                    .map(FontSizeValues::getTableCells)
                    .ifPresent(lsv::setLabelFontSize);
            svcount = oneAssessLine("sozialverhalten", sv, targetData, student, current, sName, sgl, current, null, details, lsv, listDef);
        }
        final DocumentId[] reports = zeugnisBean.findTermReports(student, current.getScheduledItemId(), true);
        if (reports.length == 1) {
            final DocumentId report = reports[0];
            //AV-SV
            final String[] validConv = new String[]{ASVAssessmentConvention.AV_NAME, ASVAssessmentConvention.SV_NAME, AVSVVorschlag.NAME};
            final Grade avg = zeugnisBean.getKopfnote(report, ASVAssessmentConvention.AV_NAME);
            final Grade svg = zeugnisBean.getKopfnote(report, ASVAssessmentConvention.SV_NAME);
            if (avg != null || svg != null || avcount != null || svcount != null) {
                final String lbl = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.avsv.label");
                final String pt = NbBundle.getMessage(NdsReportBuilder.class, "niedersachsen.arbeitsverhalten.position");
                int pos = Integer.parseInt(pt);
                final StudentDetailsXml.Text tb = details.addText(lbl, pos);
                Optional.ofNullable(listDef)
                        .map(ListDefinition::getFontSize)
                        .map(FontSizeValues::getText)
                        .ifPresent(tb::setFontSize);
                final StringJoiner sj = new StringJoiner(" - ");
                String avlbl = null;
                if (avg != null) {
                    avlbl = avg.getLongLabel();
//                    if (factory.requireAVSVReason(avg)) {
                    final String avReason = zeugnisBean.getNote(report, ASVAssessmentConvention.AV_NAME);
                    if (avReason != null) {
                        avlbl = String.join(" ", avlbl, avReason);
                    }
//                    }
                }
                String svlbl = null;
                if (svg != null) {
                    svlbl = svg.getLongLabel();
//                    if (factory.requireAVSVReason(svg)) {
                    final String svReason = zeugnisBean.getNote(report, ASVAssessmentConvention.SV_NAME);
                    if (svReason != null) {
                        svlbl = String.join(" ", svlbl, svReason);
                    }
//                    }
                }
                final StringJoiner avsvlb = new StringJoiner(" \n ");
                if (avg != null) {
                    final String m = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.avsv.message",
                            GradeFactory.findConvention(ASVAssessmentConvention.AV_NAME).getDisplayName(),
                            avlbl,
                            avg.getShortLabel());
                    avsvlb.add(m);
                } else {
                    final String m = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.no.avsv.message",
                            GradeFactory.findConvention(ASVAssessmentConvention.AV_NAME).getDisplayName());
                    avsvlb.add(m);
                }
                if (svg != null) {
                    final String m = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.avsv.message",
                            GradeFactory.findConvention(ASVAssessmentConvention.SV_NAME).getDisplayName(),
                            svlbl,
                            svg.getShortLabel());
                    avsvlb.add(m);
                } else {
                    final String m = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.no.avsv.message",
                            GradeFactory.findConvention(ASVAssessmentConvention.SV_NAME).getDisplayName());
                    avsvlb.add(m);
                }
                sj.add(avsvlb.toString());
                //Addvalue
                if (avcount != null) {
                    final StringJoiner m = new StringJoiner(", ", "(", ")");
                    m.setEmptyValue("");
                    avcount.entrySet().stream()
                            .filter(e -> Arrays.stream(validConv).anyMatch(e.getKey().getConvention()::equals))
                            .sorted(Comparator.comparing(e -> e.getKey().getShortLabel(), Collator.getInstance(Locale.getDefault())))
                            .map(e -> NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.avsv.entries.format", e.getKey().getShortLabel(), e.getValue()))
                            .sorted(Collator.getInstance(Locale.getDefault()))
                            .forEach(m::add);
                    long sum = avcount.entrySet().stream()
                            .filter(e -> Arrays.stream(validConv).anyMatch(e.getKey().getConvention()::equals))
                            .map(e -> e.getValue())
                            .collect(Collectors.summarizingInt(i -> (int) i))
                            .getSum();
                    final String msg = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.av.count.message", sum, m.toString());
                    sj.add(msg);
                }
                if (svcount != null) {
                    final StringJoiner m = new StringJoiner(", ", "(", ")");
                    m.setEmptyValue("");
                    svcount.entrySet().stream()
                            .filter(e -> Arrays.stream(validConv).anyMatch(e.getKey().getConvention()::equals))
                            .sorted(Comparator.comparing(e -> e.getKey().getShortLabel(), Collator.getInstance(Locale.getDefault())))
                            .map(e -> NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.avsv.entries.format", e.getKey().getShortLabel(), e.getValue()))
                            .forEach(m::add);
                    long sum = svcount.entrySet().stream()
                            .filter(e -> Arrays.stream(validConv).anyMatch(e.getKey().getConvention()::equals))
                            .map(e -> e.getValue())
                            .collect(Collectors.summarizingInt(i -> (int) i))
                            .getSum();
                    final String msg = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.sv.count.message", sum, m.toString());
                    sj.add(msg);
                }
                tb.setValue(sj.toString());
            }

            final Grade tg = new AbstractGrade("niedersachsen.teilnahme", "tg");
            final String agText = agTargetData.entrySet().stream()
                    .filter(e -> {
                        final Entry entry = e.getValue().selectEntry(student, current.getScheduledItemId());
                        return entry != null && Objects.equals(entry.grade, tg);
                    })
                    .map(e -> docModel.convertToUnitId(e.getKey()))
                    .map(u -> {
                        try {
                            return namingResolver.resolveDisplayName(u, current);
                        } catch (IllegalAuthorityException ex) {
                            return u.getId();
                        }
                    })
                    .collect(Collectors.joining(", "));
            if (!agText.isEmpty()) {
                final String lbl = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.ag.label");
                final String pt = NbBundle.getMessage(NdsReportBuilder.class, "zeugnis.text.title.ag.position");
                int pos = Integer.parseInt(pt);
                final StudentDetailsXml.Text tb = details.addText(lbl, pos);
                Optional.ofNullable(listDef)
                        .map(ListDefinition::getFontSize)
                        .map(FontSizeValues::getText)
                        .ifPresent(tb::setFontSize);
                tb.setValue(agText);
            }
//Bemerkungen
            final String bemerkungen = bemBean.createBemerkungen(report, pu);
            if (!bemerkungen.isEmpty()) {
                final String lbl = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.notes.label");
                final String pt = NbBundle.getMessage(NdsReportBuilder.class, "zeugnis.text.title.bemerkungen.position");
                int pos = Integer.parseInt(pt);
                final StudentDetailsXml.Text tb = details.addText(lbl, pos);
                Optional.ofNullable(listDef)
                        .map(ListDefinition::getFontSize)
                        .map(FontSizeValues::getText)
                        .ifPresent(tb::setFontSize);
                tb.setValue(bemerkungen);
            }
//Extra-Text
            String stufe;
            try {
                NamingResolver.Result r = namingResolver.resolveDisplayNameResult(pu);
                r.addResolverHint("naming.only.level");
                r.addResolverHint("klasse.ohne.schuljahresangabe");
                stufe = r.getResolvedName(current);
            } catch (IllegalAuthorityException ex) {
                final String msg = "An exception has occurred resolving primary unit level for " + pu.toString();
                Logger.getLogger(NdsFormatDetailsBean.class.getName()).log(Level.WARNING, msg, ex);
                stufe = pu.getId();
            }

            boolean excludeAGs = false;
            final Map<String, String> extraTexts = new HashMap<>();
            try {
                final int level = Integer.parseInt(stufe);
                final String[] extraTextIds = ReportProvisionsUtil.textFields(level, sgl);
                for (final String tid : extraTextIds) {
                    final String note = zeugnisBean.getNote(report, tid);
                    if (note != null) {
                        extraTexts.put(tid, note);
                    }
                }
                excludeAGs = ReportProvisionsUtil.excludeAGField(level, sgl);
            } catch (NumberFormatException nfex) {
                final String msg = "An exception has occurred parsing primary unit level " + stufe + " for " + pu.toString();
                Logger.getLogger(NdsFormatDetailsBean.class.getName()).log(Level.WARNING, msg, nfex);
            }

            for (final Map.Entry<String, String> e : extraTexts.entrySet()) {
                final String lbl = ReportProvisionsUtil.getTextFieldLabel(e.getKey());
                final int pos = ReportProvisionsUtil.getTextFieldPosition(e.getKey());
                final StudentDetailsXml.Text tb = details.addText(lbl, pos);
                Optional.ofNullable(listDef)
                        .map(ListDefinition::getFontSize)
                        .map(FontSizeValues::getText)
                        .ifPresent(tb::setFontSize);
                tb.setValue(e.getValue());
            }

            //Comments
            final Map<MultiSubject, Set<DocumentId>> cm = textDocMap.get(current.getScheduledItemId()).get("kommentare");
            if (cm != null && !cm.isEmpty()) {
                final StringJoiner sj = new StringJoiner("\n");
                for (final Map.Entry<MultiSubject, Set<DocumentId>> e : cm.entrySet()) {
                    for (final DocumentId cd : e.getValue()) {
                        final FastTextTermTargetDocument texts = textData.get(cd);
                        if (texts != null) {
                            final List<FastTextTermTargetDocument.Entry> sel = texts.select(student, current.getScheduledItemId());
                            if (sel != null && !sel.isEmpty()) {
                                final DocumentId cdb = docModel.convert(cd);
                                String commentDocName = null;
                                try {
                                    commentDocName = namingResolver.resolveDisplayNameResult(cdb).getResolvedName(current);
                                } catch (IllegalAuthorityException ex) {
                                    commentDocName = cd.toString();
                                }
                                final String t = sel.stream()
                                        .filter(le -> le.getSection() == null)
                                        .map(FastTextTermTargetDocument.Entry::getText)
                                        .collect(Collectors.joining("\n"));
                                if (StringUtils.isNotBlank(t)) {
                                    sj.add(commentDocName + ":");
                                    final StringJoiner sigs = new StringJoiner(", ", " (", ")");
                                    sigs.setEmptyValue("");
                                    texts.getSignees().entrySet().stream()
                                            .map(se -> formatSignee(se.getValue(), se.getKey()))
                                            .forEach(sigs::add);
                                    sj.add(t.trim() + sigs.toString());
                                }
                            }
                        }
                    }
                }
                final String comment = StringUtils.trimToNull(sj.toString());
                if (comment != null) {
                    final String lbl = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.comments.label");
                    final StudentDetailsXml.Text cv = details.addText(lbl, Integer.MAX_VALUE - 1000);
                    Optional.ofNullable(listDef)
                            .map(ListDefinition::getFontSize)
                            .map(FontSizeValues::getText)
                            .ifPresent(cv::setFontSize);
                    cv.setValue(comment);
                }
            }

            //            AGs
//            final String[] ags = zeugnisBean.getAGs(student, current.getScheduledItemId());
//            final String agText = Arrays.stream(ags)
//                    .collect(Collectors.joining(", "));
//Validation
            final StringJoiner validations = new StringJoiner(" - ");
            final Marker[] reportMarker = zeugnisBean.getMarkers(report);
            final ReportDoc rd = new ReportDoc(report, current.getScheduledItemId(), zeugnisnoten, reportMarker, null);
            if (sgl != null) {
                rd.getMarkerSet().add(sgl);
            }
            final OneHistory h = new OneHistory(ms, rd);
//Zensurenschnitt
            final Set<OneZensurenschnittResult> schnitte = schnitt.validate(h);
            schnitte.stream()
                    .map(ozs -> NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.ZensurenschnittValidation.message", ozs.toString()))
                    .forEach(validations::add);
//Versetzungen
            final Set<OneVersetzungsResult> res = versetzung.validate(h);
            if (res.isEmpty()) {
                final String emptyValue = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.VersetzungsValidation.emptyResultValue");
                final String msg = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.VersetzungsValidation.message", emptyValue);
                validations.add(msg);
            } else {
                res.stream()
                        .map(OneVersetzungsResult::message)
                        .filter(Objects::nonNull)
                        .map(text -> NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.VersetzungsValidation.message", text))
                        .forEach(validations::add);
            }
//Zensurensprünge
            class OneUnitsModel implements UnitsModel<VCardStudent, FastTermTargetDocument> {

                @Override
                public List<VCardStudent> getStudents() {
                    return Collections.singletonList(ms);
                }

                @Override
                public FastTermTargetDocument getTarget(DocumentId did) {
                    return targetData.get(did);
                }

                @Override
                public Set<FastTermTargetDocument> getTargets() {
                    return targetData.entrySet().stream()
                            .map(e -> e.getValue())
                            .collect(Collectors.toSet());
                }

                @Override
                public Set<TermId> getTerms() {
                    return targetData.values().stream()
                            .flatMap(f -> f.getTerms().stream())
                            .collect(Collectors.toSet());
                }

            }
            final Set<OneZensurensprungResult> sprung = zensurensprung.validate(new OneUnitsModel(), current.getScheduledItemId());
            sprung.stream()
                    .filter(r -> r.getTerm().equals(current.getScheduledItemId())) //Zur Sicherheit
                    .map(OneZensurensprungResult::getMessage)
                    .forEach(validations::add);

            final String validationsText = validations.toString();
            final boolean printValidations = Optional.ofNullable(listDef)
                    .flatMap(ld -> ld.getProperty("Validierungen"))
                    .map(Property::getValue)
                    .map(p -> !"Nein".equalsIgnoreCase(p))
                    .orElse(Boolean.TRUE);
            if (!validationsText.isEmpty() && printValidations) {
                final String lbl = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatDetails.validations.label");
                StudentDetailsXml.Text tv = details.addText(lbl, Integer.MAX_VALUE);
                Optional.ofNullable(listDef)
                        .map(ListDefinition::getFontSize)
                        .map(FontSizeValues::getText)
                        .ifPresent(tv::setFontSize);
                tv.setValue(validationsText);
            }

        }
    }

    private Map<Grade, Integer> oneAssessLine(final String ltype,
            final Map<MultiSubject, Set<DocumentId>> byQuery,
            final Map<DocumentId, FastTermTargetDocument> fttd, StudentId student,
            Term current,
            String sName,
            final Marker sgl,
            final Map<TermId, Map<Subject, Grade>> sm,
            StudentDetailsXml details,
            StudentDetailsXml.TermDataLine l,
            final String beforeTermLabel) {
        final Map<Grade, Integer> ret = new HashMap<>();
        Grade avsvVorschlag = null;
        for (Map.Entry<MultiSubject, Set<DocumentId>> e : byQuery.entrySet()) {
            final Set<FastTermTargetDocument> fdocs = e.getValue().stream()
                    //                    .map(d -> fttd.computeIfAbsent(d, tgtae::getFastTermTargetDocument))
                    .map(fttd::get)
                    .collect(Collectors.toSet());
            final MultiSubject subject = e.getKey();
//            final Marker fach = subject.getSubjectMarker(); //.getFach();
            final Set<Marker> fach = e.getKey().getSubjectMarkerSet().stream()
                    //                    .map(Subject::getSubjectMarker)
                    .collect(Collectors.toSet());
            Grade g = null;
            DocumentId d = null;
            String flk = null;
            String msg = null;
//                    GradeReference reference = null;
            String subjectAltName = null;
            GradeReference reference = null;
            try {
                d = documentMapper.find(fdocs, student, current.getScheduledItemId());
                if (d != null) {
                    subjectAltName = fttd.get(d).getAltSubjectName();
                    FastTermTargetDocument.Entry entry = fttd.get(d).selectEntry(student, current.getScheduledItemId());
                    g = entry != null ? entry.grade : null;

                    if (g != null && Uebertrag.NAME.equals(g.getConvention())) {
                        reference = (GradeReference) g;
                        final TermId before = new TermId(current.getScheduledItemId().getAuthority(), current.getScheduledItemId().getId() - 1);
                        entry = fttd.get(d).selectEntry(student, before);
                        g = entry != null ? entry.grade : null;
                    }
                }
            } catch (AmbiguousResultException oex) {
                final AmbiguousDocumentCollectionException ex = (AmbiguousDocumentCollectionException) oex;
                ex.setStudentResolvedName(sName);
                Logger.getLogger(NdsFormatter.class.getCanonicalName()).log(Level.INFO, ex.getLocalizedMessage());
                msg = ex.getDocuments().length + " Werte!";
            }
            boolean vorschlag = false;
            if (g != null && AVSVVorschlag.NAME.equals(g.getConvention()) && "vorschlag".equals(g.getId())) {
                if (avsvVorschlag == null) {
                    final Instance<VorschlagDecoration> select = extraAssessment.select(new ExtraAnnotation(ltype));
                    if (!select.isUnsatisfied() && !select.isAmbiguous()) {
                        final GradeReference vr = (GradeReference) g;
                        final Grade v = select.get().resolveReference(vr, student, current.getScheduledItemId(), null);
                        if (v != null) {
                            avsvVorschlag = v;
                            vorschlag = true;
                        }
                    } else {
                        final String mesg = "No VorschlagDecoration for " + ltype;
                        Logger.getLogger(NdsFormatDetailsBean.class.getCanonicalName()).log(Level.WARNING, mesg);
                    }
                }
                if (avsvVorschlag != null) {
                    g = avsvVorschlag;
                    vorschlag = true;
                }
            }

            if (g != null) {
                if (d != null) {
                    final Marker kurssgl = getDocumentSGL(fttd.get(d));
                    flk = NdsFormatter.checkAndGetFLK(sgl, kurssgl);
                }
                ret.compute(g, (v, i) -> i == null ? 1 : i + 1);
            }

            if (sm != null && g != null) {
                //Das muss geklärt werden, wie wird fächerübergreifender Unterricht im Zusammenhang mit den Versetzung, Notendurchschitt etc. gewertet?
//                sm.put(subject, g);
                final Grade fg = g;
                final Marker realm = subject.getRealmMarker();
                subject.getSubjectMarkerSet().stream().forEach(s -> sm.computeIfAbsent(current.getScheduledItemId(), tid -> new HashMap<>()).put(new Subject(s, realm), fg));
            }

//TODO WebUIConfiguration
            final int tier = factory.tier(subject);

            final StudentDetailsXml.ColumnValue val;
            if (subjectAltName != null) {
                val = details.setValue(l, tier, subjectAltName, g, msg);
            } else {
                val = details.setValue(l, tier, fach, g, msg);
            }

            if (val != null) {
                if (flk != null) {
                    val.setLabelLeft(flk);
                }
                StringJoiner sj = new StringJoiner(", ");
                if (vorschlag) {
                    StudentDetailsXml.Footnote vorschlagfn = details.getFootnotes().stream()
                            .filter(fn -> Objects.equals(fn.getHint(), "vorschlag"))
                            .collect(CollectionUtil.singleOrNull());
                    if (vorschlagfn == null) {
                        final String lbl = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatListe.vorschlag.label");
                        vorschlagfn = details.addFootnote(lbl);
                        vorschlagfn.setHint("vorschlag");
                    }
                    sj.add(vorschlagfn.getIndex());
                }
                if (reference != null) {
                    StudentDetailsXml.Footnote uebertragfn = details.getFootnotes().stream()
                            .filter(fn -> Objects.equals(fn.getHint(), "uebertrag"))
                            .collect(CollectionUtil.singleOrNull());
                    if (uebertragfn == null) {
                        uebertragfn = details.addFootnote(beforeTermLabel);
                        uebertragfn.setHint("uebertrag");
                    }
                    sj.add(uebertragfn.getIndex());
                }
                if (sj.length() != 0) {
                    val.setLabelRight(sj.toString());
                }

                final String color = factory.getSchulvorlage().getColoring(g);
                val.setColor(color);

                Optional.ofNullable(listDef)
                        .map(ListDefinition::getFontSize)
                        .map(FontSizeValues::getTableCells)
                        .ifPresent(val::setFontSize);
            }
        }
        return ret;
    }

    private Marker getDocumentSGL(FastTermTargetDocument ftd) {
        if (ftd != null) {
            final String sglconv = fOPFormatter.getSglConvention();
            return Arrays.stream(ftd.markers())
                    .filter(m -> m.getConvention().equals(sglconv))
                    .findAny().orElse(null);
        }
        return null;
    }

    private String formatSignee(final Signee signee, final String entitlement) {
        final StringJoiner ret = new StringJoiner(": ");
        if (!"entitled.signee".equals(entitlement)) {
            final String e = SigneeEntitlement.find(entitlement)
                    .map(SigneeEntitlement::getDisplayName)
                    .orElse(entitlement);
            ret.add(e);
        }
        final String cn = signees.getSigneeCommonName(signee);
        ret.add(cn);
        return ret.toString();
    }
}
