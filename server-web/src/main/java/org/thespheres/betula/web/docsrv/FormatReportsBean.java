/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import org.thespheres.betula.server.beans.AmbiguousResultException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
import org.thespheres.betula.niedersachsen.kgs.SGL;
import org.thespheres.betula.niedersachsen.zeugnis.NdsReportBuilderFactory;
import org.thespheres.betula.niedersachsen.xml.NdsZeugnisSchulvorlage;
import org.thespheres.betula.niedersachsen.zeugnis.ZeugnisArt;
import org.thespheres.betula.niedersachsen.zeugnis.ZeugnisBuilder;
import org.thespheres.betula.niedersachsen.zeugnis.ZeugnisData;
import org.thespheres.betula.server.beans.AmbiguousDateException;
import org.thespheres.betula.server.beans.FastTargetDocuments2;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;
import org.thespheres.betula.server.beans.StudentsListsLocalBean;
import org.thespheres.betula.server.beans.StudentsLocalBean;
import org.thespheres.betula.server.beans.annot.Current;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.LocalProperties;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.services.scheme.spi.TermNotFoundException;
import org.thespheres.betula.services.scheme.spi.TermSchedule;
import org.thespheres.betula.services.ws.CommonDocuments;
import org.thespheres.betula.util.CollectionUtil;
import org.thespheres.betula.util.StudentComparator;
import org.thespheres.ical.VCard;
import org.thespheres.ical.util.IComponentUtilities;

/**
 *
 * @author boris.heithecker
 */
@Stateless
@LocalBean
public class FormatReportsBean {

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
    private NdsReportBuilderFactory builderFactory;
    @Inject
    private LocalProperties properties;
    @Inject
    private BemerkungenBean bemBean;
//    @Inject
//    private WebUIConfiguration webConfig;
    @EJB(beanName = "StudentVCardsImpl")
    private StudentsLocalBean studentCardBean;
//    private DocumentId studentSGLMarkerDocId;
    private String sglConvention;
    private Comparator<Marker> reportsMarkerComparator;

    @PostConstruct
    public void initialize() {
//        studentSGLMarkerDocId = WebAppProperties.STUDENT_BILDUNGSGANG_DOCID;
        sglConvention = SGL.NAME;
        final Marker[] sort = Optional.ofNullable(properties.getProperty("berichte.convention"))
                .map(MarkerFactory::findConvention)
                .map(MarkerConvention::getAllMarkers)
                .orElse(null);
        reportsMarkerComparator = Comparator.comparingInt(m -> sort == null ? Integer.MAX_VALUE : Arrays.<Marker>binarySearch(sort, m, Comparator.comparing(Marker::getId)));
    }

    public ZeugnisData createZgn(FastTargetDocuments2 tgtae, DocumentId zgnId, UnitId pu, Map<MultiSubject, Set<DocumentId>> docMap, Map<MultiSubject, Set<DocumentId>> reports, Map<DocumentId, FastTermTargetDocument> map, Map<DocumentId, FastTextTermTargetDocument> rMap, Date asOf) throws NoEntityFoundException {
        final StudentId student = zeugnisBean.getStudent(zgnId);
        final String careers = properties.getProperty("student.career.conventions");
        final VCard card = studentCardBean.get(student);
        final Marker[] markers = zeugnisBean.getMarkers(zgnId);

        Marker sgl = null;
        if (careers != null) {//TODO configure choice
            Arrays.stream(markers)
                    .filter(m -> m.getConvention().equals(careers))
                    .collect(CollectionUtil.singleOrNull());
        }
        if (sgl == null) {
            sgl = getStudentSGL(student, asOf);
        }
//        if (sgl == null) {
//            return null;
//        }
        final ZeugnisBuilder builder;
        final Marker[] typ = Arrays.stream(markers)
                .filter(m -> m.getConvention().equals(ZeugnisArt.CONVENTION_NAME))
                .toArray(Marker[]::new);
        final String abschluss = Arrays.stream(markers)
                .filter(m -> m.getConvention().equals(Abschluesse.CONVENTION_NAME))
                .findAny()
                .map(Marker::getLongLabel)
                .orElse(null);
        Marker zeugnisTyp = null;
        if (typ.length > 1) {
            throw new IllegalArgumentException("More than one ZeugnisType.");
        } else if (typ.length == 1) {
            zeugnisTyp = typ[0];
            builder = new ZeugnisBuilder(sgl, zeugnisTyp.getLongLabel());
        } else {
            builder = new ZeugnisBuilder(sgl);
        }
        final Comparator<Marker> comp = ZeugnisBuilder.forSGL(sgl);

        Date dateOfBirth;
        try {
            dateOfBirth = IComponentUtilities.DATE.parse(card.getAnyPropertyValue(VCard.BDAY).orElse(""));
        } catch (ParseException ex) {
            dateOfBirth = new Date(0l);
        }
        final String placeOfBirth = card.getAnyPropertyValue(VCard.BIRTHPLACE).orElse("UNBEKANNT");
        final String n = card.getAnyPropertyValue(VCard.N).get();
        final String vorname = n.split(";")[1].replace(",", " ");
        final String fN = vorname + " " + n.split(";")[0];
        //use NdsZeugnisAngaben
        final Integer fehltage = zeugnisBean.getIntegerValue(zgnId, org.thespheres.betula.server.beans.ReportsBean.TYPE_FEHLTAGE);
        final Integer unentschuldigt = zeugnisBean.getIntegerValue(zgnId, org.thespheres.betula.server.beans.ReportsBean.TYPE_UENTSCHULDIGT);
        final Grade av = zeugnisBean.getKopfnote(zgnId, ASVAssessmentConvention.AV_NAME);
        final Grade sv = zeugnisBean.getKopfnote(zgnId, ASVAssessmentConvention.SV_NAME);

        String headerPflichtunterricht = sgl != null ? ZeugnisData.PFLICHTUNTERRICHT : ZeugnisData.LEHRGAENGE;

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

        final ArrayList<ZeugnisBuilder.ZeugnisField> fl = new ArrayList<>();
        final ArrayList<ZeugnisBuilder.ZeugnisField> flwpk = new ArrayList<>();
        final ArrayList<ZeugnisBuilder.ZeugnisField> flprofil = new ArrayList<>();
        final ArrayList<ZeugnisBuilder.ZeugnisBericht> ber = new ArrayList<>();
        for (final Map.Entry<MultiSubject, Set<DocumentId>> e : docMap.entrySet()) {
            Grade g = null;
            String flk = null;
            GradeReference reference = null;
            try {
                final Set<FastTermTargetDocument> fdocs = e.getValue().stream()
                        .map(map::get)
                        .collect(Collectors.toSet());
                final DocumentId d = documentMapper.find(fdocs, student, term);
                if (d == null) {
                    continue;
                }
                FastTermTargetDocument.Entry entry = map.get(d).selectEntry(student, term);
                g = entry != null ? entry.grade : null;
                if (g != null && Uebertrag.NAME.equals(g.getConvention())) {
                    reference = (GradeReference) g;
                    entry = map.get(d).selectEntry(student, before);
                    g = entry != null ? entry.grade : null;
                }
                if (g == null) {
                    continue;
                }
                Marker kurssgl = getDocumentSGL(tgtae, d);
                if (sgl != null) {
                    flk = NdsFormatter.checkAndGetFLK(sgl, kurssgl);
                }
            } catch (AmbiguousResultException ex) {
            }
            final Set<Marker> subjects = e.getKey().getSubjectMarkerSet().stream()
                    //                    .map(Subject::getSubjectMarker)
                    .collect(Collectors.toSet());
            final ZeugnisBuilder.ZeugnisField zf = builder.createField(subjects, g, flk); //HIER: resolve "Weitere Fremdsprache" nach Namen
            if (reference != null) {
                zf.setFootnote(fnindex, fnText);
            }
            final int tier = builderFactory.tier(e.getKey());
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
        Collections.sort(fl);
        Collections.sort(flwpk);
        Collections.sort(flprofil);

        if (sgl == null && reports != null && !reports.isEmpty()) {
            reports.entrySet().forEach(e -> {
                try {
                    final Set<FastTextTermTargetDocument> fdocs = e.getValue().stream()
                            .map(rMap::get)
                            .collect(Collectors.toSet());
                    final DocumentId d = documentMapper.findTexts(fdocs, student, term);
                    if (d != null) {
                        final List<FastTextTermTargetDocument.Entry> entries = rMap.get(d).select(student, term);
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
                                        final ZeugnisBuilder.ZeugnisBericht zb;
                                        if (!fach.isEmpty()) {
                                            zb = new ZeugnisBuilder.ZeugnisBericht(fach, text, comp);
                                        } else {
//                            zb = new ZeugnisBuilder.ZeugnisBericht(e.getKey().getSubjectMarker().getLongLabel(), text, comp);
                                            zb = new ZeugnisBuilder.ZeugnisBericht("?", text, comp);
                                        }
                                        ber.add(zb);
                                    }
                                });
                    }
                } catch (AmbiguousResultException ex) {
                }
            });
            Collections.sort(ber);
        }

        final String bemerkungen = bemBean.createBemerkungen(zgnId, pu);

        final String[] ags = zeugnisBean.getAGs(student, term);
        final String agText = Arrays.stream(ags)
                .collect(Collectors.joining(", "));

        Date aDatum;
        try {
            aDatum = zeugnisArguments.getZeugnisAusgabe(term, pu, zgnId, abschluss != null);
        } catch (AmbiguousDateException ex) {
            aDatum = null;
            Logger.getLogger(FormatReportsBean.class.getCanonicalName()).log(Level.CONFIG, ex.getLocalizedMessage(), ex);
        }

        final NdsZeugnisSchulvorlage vorlage = builderFactory.getSchulvorlage();
        builder.setNamenLogos(vorlage.getSchoolName(), vorlage.getSchoolName2(), vorlage.getImageLeftUrl(), vorlage.getImageRightUrl())
                .setVornameNachname(fN)
                .setGeburtsdatum(dateOfBirth)
                .setGeburtsort(placeOfBirth)
                .setAbschluss(abschluss)
                .setHalbjahresdatenUndKlasse(term, pu, namingResolver) //DEFAULTNAMINGRESOLVERPROVIDER)
                .setFehltageUnentschuldigt(term, fehltage, unentschuldigt)
                .setZeungisdatum(vorlage.getSchoolLocation(), aDatum)
                .createArea(headerPflichtunterricht, fl, ber.isEmpty())
                .createArea(ZeugnisData.BERICHTE, ber)
                .createArea(ZeugnisData.WAHLPFLICHTUNTERRICHT, flwpk, sgl != null)
                .createArea(ZeugnisData.PROFILUNTERRICHT, flprofil, false)
                .setAGs(agText)
                .createKopfnote(av, vorname, "niedersachsen.arbeitsverhalten")
                .createKopfnote(sv, vorname, "niedersachsen.sozialverhalten")
                .setBemerkungen(bemerkungen)
                .getZeugnisData();
        if (!ber.isEmpty()) {
            builder.setVornameNachnameZweiteSeite(fN);
        }
        if (zeugnisTyp != null && (zeugnisTyp.getId().equals("abgang") || zeugnisTyp.getId().equals("abschluss"))) {
//            zeugnisArguments.getZeugnisAusgabe(zgnId);
            String stufe;
            try {
                NamingResolver.Result r = namingResolver.resolveDisplayNameResult(pu);
                r.addResolverHint("naming.only.level");
                r.addResolverHint("klasse.ohne.schuljahresangabe");
                stufe = r.getResolvedName(termSchedule.resolve(term));
            } catch (IllegalAuthorityException | TermNotFoundException ex) {
                Logger.getLogger(NdsFormatter.class.getName()).log(Level.WARNING, ex.getLocalizedMessage(), ex);
                stufe = pu.getId();
            }
            builder.setAbgangsdaten(aDatum, stufe + ".");
        }
        ZeugnisData data = builder.getZeugnisData();
        data.setSortString(StudentComparator.sortStringFromDirectoryName(n));
        return data;
    }

    private Marker getDocumentSGL(FastTargetDocuments2 tgtae, DocumentId d) {
        for (final Marker m : tgtae.getDocumentMarkers(d)) {
            if (m.getConvention().equals(sglConvention)) {
                return m;
            }
        }
        return null;
    }

    private Marker getStudentSGL(StudentId student, Date asOf) {
        final DocumentId d = builderFactory.forName(CommonDocuments.STUDENT_CAREERS_DOCID);
        return d != null ? sllb.getMarkerEntry(student, d, asOf) : null;
    }
}
