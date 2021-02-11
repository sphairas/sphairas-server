/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.io.IOException;
import java.nio.file.Path;
import org.thespheres.betula.server.beans.AmbiguousResultException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import org.openide.util.NbBundle;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.AssessmentConvention;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeReference;
import org.thespheres.betula.server.beans.NoEntityFoundException;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.MarkerConvention;
import org.thespheres.betula.document.MarkerFactory;
import org.thespheres.betula.services.CommonTargetProperties;
import org.thespheres.betula.document.model.MultiSubject;
import org.thespheres.betula.niedersachsen.ASVAssessmentConvention;
import org.thespheres.betula.niedersachsen.Abschluesse;
import org.thespheres.betula.niedersachsen.NdsTerms;
import org.thespheres.betula.niedersachsen.Uebertrag;
import org.thespheres.betula.niedersachsen.zeugnis.ZeugnisArt;
import org.thespheres.betula.niedersachsen.zeugnis.NdsReportBuilder;
import org.thespheres.betula.niedersachsen.zeugnis.NdsReportBuilderFactory;
import org.thespheres.betula.niedersachsen.NdsZeugnisFormular;
import org.thespheres.betula.niedersachsen.gs.CrossmarkSettings;
import org.thespheres.betula.niedersachsen.zeugnis.NdsReportBuilder.GradeEntry;
import org.thespheres.betula.niedersachsen.zeugnis.ReportProvisionsUtil;
import org.thespheres.betula.server.beans.AmbiguousDateException;
import org.thespheres.betula.server.beans.FastTargetDocuments2;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;
import org.thespheres.betula.server.beans.StudentsListsLocalBean;
import org.thespheres.betula.server.beans.StudentsLocalBean;
import org.thespheres.betula.server.beans.Utilities;
import org.thespheres.betula.server.beans.annot.Current;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.LocalProperties;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.services.scheme.spi.TermNotFoundException;
import org.thespheres.betula.services.scheme.spi.TermSchedule;
import org.thespheres.betula.services.ws.CommonDocuments;
import org.thespheres.betula.util.CollectionUtil;
import org.thespheres.ical.VCard;
import org.thespheres.ical.util.IComponentUtilities;

/**
 *
 * @author boris.heithecker
 */
@Stateless
@LocalBean
public class NdsFormatReportsBean {

    @EJB(beanName = "ReportsBeanImpl")
    private org.thespheres.betula.server.beans.ReportsBean zeugnisBean;
    @EJB
    private StudentsListsLocalBean sllb;
    @EJB
    private DocumentMapper documentMapper;
    @Inject
    private CommonTargetProperties targetProps;
    @EJB
    private ZeugnisArguments zeugnisArguments;
    @Default
    @Inject
    private TermSchedule termSchedule;
    @Current
    @Inject
    private Term currentTerm;
    @Default
    @Inject
    private NamingResolver namingResolver;
    @Inject
    private LocalProperties properties;
    @Inject
    private BemerkungenBean bemBean;
//    @Inject
//    private WebUIConfiguration webConfig;
    @Inject
    private CrossmarkSettings crossmarks;
    @EJB(beanName = "StudentVCardsImpl")
    private StudentsLocalBean studentCardBean;
//    private String sglConvention;
    private Comparator<Marker> reportsMarkerComparator;
    private final static boolean ADD_IDENTITIES = true;
    private final static boolean ADD_SIGNEES = false;

    @PostConstruct
    public void initialize() {
//        sglConvention = webConfig.getProperty(WebUIConfiguration.SGL_NAME, String.class);
        final Marker[] sort = Optional.ofNullable(properties.getProperty("berichte.convention"))
                .map(MarkerFactory::findConvention)
                .map(MarkerConvention::getAllMarkers)
                .orElse(null);
        reportsMarkerComparator = Comparator.comparingInt(m -> sort == null ? Integer.MAX_VALUE : Arrays.<Marker>binarySearch(sort, m, Comparator.comparing(Marker::getId)));
    }

    public NdsZeugnisFormular createReport(final FastTargetDocuments2 tgtae, final DocumentId zgnId, final UnitId pu, final Map<MultiSubject, Set<DocumentId>> docMap, final Map<MultiSubject, Set<DocumentId>> reports, final Map<DocumentId, FastTermTargetDocument> targets, final Map<DocumentId, FastTextTermTargetDocument> texts, final Date asOf, final NdsReportBuilderFactory fac, final boolean encodeLogos) throws NoEntityFoundException {
        final StudentId student = zeugnisBean.getStudent(zgnId);
        final String careers = properties.getProperty("student.career.conventions");
        final VCard card = studentCardBean.get(student);
        final Marker[] markers = zeugnisBean.getMarkers(zgnId);

        final String nProp = card.getAnyPropertyValue(VCard.N).get();
        final String vorname = Utilities.findGivenName(card); //nProp.split(";")[1].replace(",", " ");
        final String fN = Utilities.formatFullname(card); //vorname + " " + nProp.split(";")[0];

        Marker sgl = null;
        if (careers != null) {//TODO configure choice
            Arrays.stream(markers)
                    .filter(m -> m.getConvention().equals(careers))
                    .collect(CollectionUtil.singleOrNull());
        }
        if (sgl == null) {
            sgl = Optional.ofNullable(fac.forName(CommonDocuments.STUDENT_CAREERS_DOCID))
                    .map(d -> sllb.getMarkerEntry(student, d, asOf))
                    .orElse(null);
        }
//        if (sgl == null) {
//            return null;
//        }
        final NdsReportBuilder builder;

        final Marker[] typ = Arrays.stream(markers)
                .filter(m -> m.getConvention().equals(ZeugnisArt.CONVENTION_NAME))
                .toArray(Marker[]::new);
        final String abschluss = Arrays.stream(markers)
                .filter(m -> m.getConvention().equals(Abschluesse.CONVENTION_NAME))
                .findAny()
                .map(Marker::getLongLabel)
                .orElse(null);

        final boolean abgangsZeugnis;
        if (typ.length > 1) {
            throw new IllegalArgumentException("More than one ZeugnisType.");
        } else if (typ.length == 1) {
            final Marker zeugnisTyp = typ[0];
            abgangsZeugnis = zeugnisTyp != null && (zeugnisTyp.getId().equals("abgang") || zeugnisTyp.getId().equals("abschluss"));
            builder = fac.newBuilder(Utilities.findFamilyName(card), zeugnisTyp.getLongLabel(), sgl);
//            builder = new NdsReportBuilder(sgl, zeugnisTyp.getLongLabel());
        } else {
            builder = fac.newBuilder(Utilities.findFamilyName(card), null, sgl);
            abgangsZeugnis = false;
        }

        if (encodeLogos) {
            final Path appRes = NdsFormatter.findAppResourcesBase();
            try {
                builder.encodeLogos(appRes);
            } catch (IOException ex) {
                Logger.getLogger(NdsFormatReportsBean.class.getName()).log(Level.WARNING, ex.getMessage());
                builder.addException(ex);
            }
        }

        if (ADD_IDENTITIES) {
            builder.setUnit(pu)
                    .setStudent(student)
                    .setZeugnisId(zgnId);
        }

        final Comparator<Marker> comp = fac.forCareer(sgl);

        Date dateOfBirth;
        try {
            dateOfBirth = IComponentUtilities.DATE.parse(card.getAnyPropertyValue(VCard.BDAY).orElse(""));
        } catch (ParseException ex) {
            dateOfBirth = new Date(0l);
        }
        final String placeOfBirth = card.getAnyPropertyValue(VCard.BIRTHPLACE).orElse("UNBEKANNT");
        //use NdsZeugnisAngaben
        final Integer fehltage = zeugnisBean.getIntegerValue(zgnId, org.thespheres.betula.server.beans.ReportsBean.TYPE_FEHLTAGE);
        final Integer unentschuldigt = zeugnisBean.getIntegerValue(zgnId, org.thespheres.betula.server.beans.ReportsBean.TYPE_UNENTSCHULDIGT);
        final Grade av = zeugnisBean.getKopfnote(zgnId, ASVAssessmentConvention.AV_NAME);
        final String avBegruendung = fac.requireAVSVReason(av) ? zeugnisBean.getNote(zgnId, ASVAssessmentConvention.AV_NAME) : null;
        final Grade sv = zeugnisBean.getKopfnote(zgnId, ASVAssessmentConvention.SV_NAME);
        final String svBegruendung = fac.requireAVSVReason(sv) ? zeugnisBean.getNote(zgnId, ASVAssessmentConvention.SV_NAME) : null;

        final TermId term = zeugnisBean.getTerm(zgnId);
        final TermId before = new TermId(term.getAuthority(), term.getId() - 1);
        final String fnindex = "i)";
        String fnText;
        try {
            Term br = termSchedule.resolve(before);
            int bhj = (Integer) br.getParameter(NdsTerms.HALBJAHR);
            int j = (int) br.getParameter(NdsTerms.JAHR);
            String bsj = Integer.toString(j) + "/" + Integer.toString(j + 1).substring(2);
            fnText = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatReport.uebertrag.label", bhj, bsj);
        } catch (TermNotFoundException | IllegalAuthorityException ex) {
            fnText = ex.getLocalizedMessage();
        }

        final ArrayList<NdsReportBuilder.GradeEntry> fl = new ArrayList<>();
        final ArrayList<NdsReportBuilder.GradeEntry> flwpk = new ArrayList<>();
        final ArrayList<NdsReportBuilder.GradeEntry> flprofil = new ArrayList<>();
        final ArrayList<NdsReportBuilder.GradeEntry> flcrossmark = new ArrayList<>();
        final ArrayList<NdsReportBuilder.TextEntry> ber = new ArrayList<>();

        String stufe;
        try {
            NamingResolver.Result r = namingResolver.resolveDisplayNameResult(pu);
            r.addResolverHint("naming.only.level");
            r.addResolverHint("klasse.ohne.schuljahresangabe");
            stufe = r.getResolvedName(termSchedule.resolve(term));
        } catch (IllegalAuthorityException | TermNotFoundException ex) {
            final String msg = "An exception has occurred resolving primary unit level for " + pu.toString();
            Logger.getLogger(NdsFormatReportsBean.class.getName()).log(Level.WARNING, msg, ex);
            stufe = pu.getId();
        }

        int level = -1;
        try {
            level = Integer.parseInt(stufe);
        } catch (NumberFormatException nfex) {
            final String msg = "An exception has occurred parsing primary unit level " + stufe + " for " + pu.toString();
            Logger.getLogger(NdsFormatReportsBean.class.getName()).log(Level.WARNING, msg, nfex);
        }

//        final List<String> crossMarkSubjectConventions = Arrays.stream(properties.getProperty("crossmark.subject.conventions", "").split(","))
//                .collect(Collectors.toList());
//        final AssessmentConvention crossMarkGradeConvention = Optional.ofNullable(properties.getProperty("crossmark.assessment.convention"))
//                .map(GradeFactory::findConvention)
//                .orElse(null);
        final MarkerConvention[] crossMarkSubjectConventions = crossmarks.conventions(level);
        final AssessmentConvention crossMarkGradeConvention = crossmarks.getAssessmentConvention();
//        if (!crossmarks.isUnsatisfied()) {
//            crossMarkSubjectConventions = crossmarks.get().conventions(level);
//            crossMarkGradeConvention = crossmarks.get().getAssessmentConvention();
//        } else {
//            crossMarkSubjectConventions = null;
//            crossMarkGradeConvention = null;
//        }

        boolean foundCaptionsOnLayoutConvention = false;
        for (final Map.Entry<MultiSubject, Set<DocumentId>> e : docMap.entrySet()) {
            Grade g = null;
            String flk = null;
            GradeReference reference = null;
            FastTermTargetDocument td = null;
            try {
                final Set<FastTermTargetDocument> fdocs = e.getValue().stream()
                        .map(targets::get)
                        .collect(Collectors.toSet());
                final DocumentId d = documentMapper.find(fdocs, student, term);
                if (d == null) {
                    continue;
                }
                td = targets.get(d);
                FastTermTargetDocument.Entry entry = td.selectEntry(student, term);
                g = entry != null ? entry.grade : null;
                if (g != null && Uebertrag.NAME.equals(g.getConvention())) {
                    reference = (GradeReference) g;
                    entry = td.selectEntry(student, before);
                    g = entry != null ? entry.grade : null;
                }
                if (g == null) {
                    continue;
                }
                Marker kurssgl = tgtae.getDocumentMarkers(d).stream()
                        .filter(m -> m.getConvention().equals(fac.careerMarkerConvention()))
                        .collect(CollectionUtil.singleOrNull());

                if (sgl != null) {
                    flk = NdsFormatter.checkAndGetFLK(sgl, kurssgl);
                }
            } catch (AmbiguousResultException ex) {
                builder.addException(ex);
            }
            foundCaptionsOnLayoutConvention = foundCaptionsOnLayoutConvention
                    || (g != null && ReportProvisionsUtil.getGradeCaptionsOnLayoutConventionName().equals(g.getConvention()));
            final Set<Marker> subjects = e.getKey().getSubjectMarkerSet().stream()
                    //                    .map(Subject::getSubjectMarker)
                    .collect(Collectors.toSet());
            final NdsReportBuilder.GradeEntry zf = builder.newGradeEntry(subjects, g, flk); //HIER: resolve "Weitere Fremdsprache" nach Namen
            if (reference != null) {
                zf.setFootnote(fnindex, fnText);
            }
            if (abgangsZeugnis) {
                zf.setUseLongLabel(true);
            }
            final String altSubjectName;
            if (td != null && (altSubjectName = td.getAltSubjectName()) != null) {
                zf.setAltSubject(altSubjectName);
            }
            if (ADD_IDENTITIES && td != null) {
                zf.setTarget(td.getDocument());
            }
            if (ADD_SIGNEES && td != null) {
                zf.setSignee(td.getSignees().get("entitled.signee"));
            }
            //
            if (zf.getSubject().stream().allMatch(s -> Arrays.stream(crossMarkSubjectConventions).anyMatch(cv -> cv.getName().equals(s.getConvention())))) {
                flcrossmark.add(zf);
                continue;
            }
            //
            final int tier = fac.tier(e.getKey());
            switch (tier) {
                case 0:
                    fl.add(zf);
                    break;
                case 1:
                    flwpk.add(zf);
                    break;
                case 2:
                    flprofil.add(zf);
                    break;
                default:
                    break;
            }
        }
        final Comparator<NdsReportBuilder.GradeEntry> cmp = (e1, e2) -> {
            final Marker f1 = e1.getSubject().stream().min(comp).get();
            final Marker f2 = e2.getSubject().stream().min(comp).get();
            return comp.compare(f1, f2);
        };
        Collections.sort(fl, cmp);
        Collections.sort(flwpk, cmp);
        Collections.sort(flprofil, cmp);

        if (sgl == null && reports != null && !reports.isEmpty()) {
            reports.entrySet().forEach(e -> {
                try {
                    final Set<FastTextTermTargetDocument> fdocs = e.getValue().stream()
                            .map(texts::get)
                            .collect(Collectors.toSet());
                    final DocumentId d = documentMapper.findTexts(fdocs, student, term);
                    if (d != null) {
                        final List<FastTextTermTargetDocument.Entry> entries = texts.get(d).select(student, term);
                        entries.stream()
                                .sorted(Comparator.comparing(en -> en.section, Comparator.nullsFirst(reportsMarkerComparator)))
                                .forEach(entry -> { //TODO: sorted
                                    final String text = entry != null ? entry.text : null;
                                    if (text != null && !text.isEmpty()) {
                                        assert entry != null;
                                        final Set<Marker> fach;
                                        if (!Marker.isNull(entry.section)) {
                                            fach = Collections.singleton(entry.section);
                                        } else {
//                        Marker fach = e.getKey().getSubjectMarker();
                                            fach = e.getKey().getSubjectMarkerSet().stream()
                                                    //                                .map(Subject::getSubjectMarker)
                                                    .collect(Collectors.toSet());
                                        }
                                        final NdsReportBuilder.TextEntry zb = builder.newTextEntry(fach, text);
                                        ber.add(zb);
                                    }
                                });
                    }
                } catch (AmbiguousResultException ex) {
                    builder.addException(ex);
                }
            });
            final Comparator<NdsReportBuilder.TextEntry> cmp2 = (e1, e2) -> {
                final Marker f1 = e1.getSubject().stream().min(comp).orElse(Marker.NULL);
                final Marker f2 = e2.getSubject().stream().min(comp).orElse(Marker.NULL);
                return comp.compare(f1, f2);
            };
            Collections.sort(ber, cmp2);
        }

        final String headerPflichtunterricht = ReportProvisionsUtil.getHeaderPflichtUnterricht(sgl, stufe);
        if (!fl.isEmpty() || (ber.isEmpty() && flcrossmark.isEmpty())) {
            builder.createGradesArea(headerPflichtunterricht, fl);
        }
        builder.createTextArea(NdsZeugnisFormular.BERICHTE, ber);
        if (!flwpk.isEmpty() || sgl != null) {
            builder.createGradesArea(NdsZeugnisFormular.WAHLPFLICHTUNTERRICHT, flwpk);
        }
        if (!flprofil.isEmpty()) {
            builder.createGradesArea(NdsZeugnisFormular.PROFILUNTERRICHT, flprofil);
        }
        if (crossMarkGradeConvention != null && !flcrossmark.isEmpty()) {
            final NdsZeugnisFormular.CrossMarkArea cma = builder.createCrossMarkArea();

//            crossMarkSubjectConventions.stream()
//                    .map(MarkerFactory::findConvention)
//                    .filter(Objects::nonNull)
            Arrays.stream(crossMarkSubjectConventions)
                    //                    .peek(mc -> Logger.getLogger("ZEUGNISSE").log(Level.INFO, "Found " + mc.getClass().getCanonicalName()))
                    .forEach(cmc -> {
                        final NdsZeugnisFormular.CrossMarkSubject cms = new NdsZeugnisFormular.CrossMarkSubject(cmc.getDisplayName());
                        cma.getSubjects().add(cms);
                        String subset = null;
                        for (final Marker m : cmc.getAllMarkers()) {
                            if (!Objects.equals(m.getSubset(), subset)) {
                                final String cat = m.getLongLabel("category");
                                NdsZeugnisFormular.CrossMarkLine h = new NdsZeugnisFormular.CrossMarkLine(cat, 1);
                                cms.getLines().add(h);
                                subset = m.getSubset();
                            }
                            NdsZeugnisFormular.CrossMarkLine cml = new NdsZeugnisFormular.CrossMarkLine(m.getLongLabel());
                            cms.getLines().add(cml);
                            flcrossmark.stream()
                                    .filter(e -> e.getSubject().size() == 1)
                                    .filter(e -> e.getSubject().contains(m))
                                    .map(GradeEntry::getGrade)
                                    .collect(CollectionUtil.requireSingleton())
                                    .ifPresent(g -> {
                                        boolean set = false;
                                        if (g.getConvention().equals(crossMarkGradeConvention.getName())) {
                                            //
                                            final Grade[] ag = crossMarkGradeConvention.getAllGrades();
                                            for (int i = 0; i < ag.length; i++) {
                                                if (ag[i].equals(g)) {
                                                    cml.setPosition(i + 1);
                                                    set = true;
                                                    break;
                                                }
                                            }
                                            //
                                        }
                                        if (!set) {
                                            cml.setEntryText(g.getShortLabel());
                                        }
                                    });
                        }

                    });
        }

        final String bemerkungen = bemBean.createBemerkungen(zgnId, pu);

        final Map<String, String> extraTexts = new HashMap<>();
        final String[] extraTextIds = ReportProvisionsUtil.textFields(level, sgl);
        for (final String tid : extraTextIds) {
            final String note = zeugnisBean.getNote(zgnId, tid);
            if (note != null) {
                extraTexts.put(tid, note);
            }
        }
        final boolean excludeAGs = ReportProvisionsUtil.excludeAGField(level, sgl);

        if (!excludeAGs) {
            final String[] ags = zeugnisBean.getAGs(student, term);
            final String agText = Arrays.stream(ags)
                    .collect(Collectors.joining(", "));
            builder.setAGs(agText);
        }

        Date aDatum;
        try {
            aDatum = zeugnisArguments.getZeugnisAusgabe(term, pu, zgnId, abschluss != null);
        } catch (AmbiguousDateException ex) {
            aDatum = null;
            builder.addException(ex);
        }
        final LocalDate issuance = aDatum == null ? null : LocalDate.from(aDatum.toInstant().atZone(ZoneId.systemDefault()));

        try {
            final NamingResolver.Result result = namingResolver.resolveDisplayNameResult(pu);
            builder.setHalbjahresdatenUndKlasse(term, result);
        } catch (IllegalAuthorityException ex) {
            Logger.getLogger(NdsFormatReportsBean.class.getName()).log(Level.WARNING, ex.getMessage());
            builder.addException(ex);
        }
        builder.setVornameNachname(fN)
                .setGeburtsdatum(dateOfBirth)
                .setGeburtsort(placeOfBirth)
                .setAbschluss(abschluss)
                .setFehltageUnentschuldigt(term, fehltage, unentschuldigt)
                .setZeungisdatum(issuance)
                .createKopfnote(av, vorname, "niedersachsen.arbeitsverhalten", avBegruendung)
                .createKopfnote(sv, vorname, "niedersachsen.sozialverhalten", svBegruendung)
                .setBemerkungen(bemerkungen)
                .setGradeCaptionsOnLayout(foundCaptionsOnLayoutConvention)
                .getZeugnisData();

        for (final Map.Entry<String, String> e : extraTexts.entrySet()) {
            final String lbl = ReportProvisionsUtil.getTextFieldLabel(e.getKey());
            final int pos = ReportProvisionsUtil.getTextFieldPosition(e.getKey());
            builder.setExtraText(pos, lbl, e.getValue());
        }

        if (!ber.isEmpty()) {
            builder.setVornameNachnameZweiteSeite(fN);
        }

        if (abgangsZeugnis) {
//            zeugnisArguments.getZeugnisAusgabe(zgnId);
            builder.setAbgangsdaten(issuance, stufe + ".");
        }
        final NdsZeugnisFormular data = builder.getZeugnisData();
        return data;
    }

}
