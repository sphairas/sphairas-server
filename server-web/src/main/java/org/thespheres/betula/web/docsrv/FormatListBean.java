/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import org.thespheres.betula.server.beans.AmbiguousResultException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.openide.util.NbBundle;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeReference;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.MarkerConvention;
import org.thespheres.betula.document.MarkerFactory;
import org.thespheres.betula.document.model.MultiSubject;
import org.thespheres.betula.niedersachsen.NdsTerms;
import org.thespheres.betula.niedersachsen.Uebertrag;
import org.thespheres.betula.niedersachsen.kgs.SGL;
import org.thespheres.betula.niedersachsen.vorschlag.AVSVVorschlag;
import org.thespheres.betula.niedersachsen.vorschlag.VorschlagDecoration;
import org.thespheres.betula.niedersachsen.zeugnis.NdsReportBuilderFactory;
import org.thespheres.betula.niedersachsen.zeugnis.listen.ZensurenListe;
import org.thespheres.betula.niedersachsen.zeugnis.listen.ZensurenListeCsv;
import org.thespheres.betula.niedersachsen.zeugnis.listen.ZensurenListeXml;
import org.thespheres.betula.niedersachsen.zeugnis.listen.ZensurenListenCollectionXml;
import org.thespheres.betula.server.beans.FastTargetDocuments2;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.StudentsListsLocalBean;
import org.thespheres.betula.server.beans.StudentsLocalBean;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.LocalProperties;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.services.ws.CommonDocuments;
import org.thespheres.betula.web.PrimaryUnit;
import org.thespheres.betula.web.config.ExtraAnnotation;

/**
 *
 * @author boris.heithecker
 */
@Stateless
@LocalBean
public class FormatListBean {

    @EJB(beanName = "StudentVCardsImpl")
    private StudentsLocalBean studentCardBean;
    @EJB
    private DocumentMapper documentMapper;
    @EJB
    private NdsFormatter fOPFormatter;
//    @Inject
//    private WebUIConfiguration webConfig;
    @Inject
    private LocalProperties properties;
//    private DocumentId studentSGLMarkerDocId;
    private String sglConvention;
    @EJB
    private StudentsListsLocalBean sllb;
    @Inject
    private NdsReportBuilderFactory builderFactory;
    @Any
    @Inject
    private Instance<VorschlagDecoration> extraAssessment;

    @PostConstruct
    public void initialize() {
//        studentSGLMarkerDocId = WebAppProperties.STUDENT_BILDUNGSGANG_DOCID;
        sglConvention = sglConvention = SGL.NAME;
        final Marker[] sort = Optional.ofNullable(properties.getProperty("berichte.convention"))
                .map(MarkerFactory::findConvention)
                .map(MarkerConvention::getAllMarkers)
                .orElse(null);
//        reportsMarkerComparator = Comparator.comparingInt(m -> sort == null ? Integer.MAX_VALUE : Arrays.<Marker>binarySearch(sort, m, Comparator.comparing(Marker::getId)));
    }

    public void oneListe(final boolean vorzensuren, final FastTargetDocuments2 tgtae, final UnitId pu, final Term term, String ltype, Map<String, Map<MultiSubject, Set<DocumentId>>> docMap, NamingResolver resolver, Map<DocumentId, FastTermTargetDocument> fttd, Term before, ZensurenListenCollectionXml collection) {
        final ZensurenListeXml list = new ZensurenListeXml();
        list.getTierLabels().put(1, "WPK");
//        list.getTierLabels().put(2, "Profil");
        if (!oneListeImpl(vorzensuren, tgtae, pu, term, ltype, docMap, resolver, list, fttd, before)) {
            return;
        }
        list.removeEmptyColumns();
        list.sort();
        collection.LISTS.add(list);
    }

    public void oneListeCsv(final boolean vorzensuren, final FastTargetDocuments2 tgtae, final UnitId pu, final Term term, String ltype, Map<String, Map<MultiSubject, Set<DocumentId>>> docMap, NamingResolver resolver, Map<DocumentId, FastTermTargetDocument> fttd, Term before, List<ZensurenListeCsv> collection) {
        final ZensurenListeCsv list = new ZensurenListeCsv(ltype);
        if (!oneListeImpl(vorzensuren, tgtae, pu, term, ltype, docMap, resolver, list, fttd, before)) {
            return;
        }
        collection.add(list);
    }

    boolean oneListeImpl(final boolean vorzensuren, final FastTargetDocuments2 tgtae, final UnitId pu, final Term term, String ltype, Map<String, Map<MultiSubject, Set<DocumentId>>> docMap, NamingResolver resolver, final ZensurenListe list, Map<DocumentId, FastTermTargetDocument> fttd, Term before) throws MissingResourceException {
        Map<MultiSubject, Set<DocumentId>> byQuery = null;
        if (vorzensuren) {
            final Collection<DocumentId> coll = tgtae.getTargetAssessmentDocumentsForTerm(pu, term.getScheduledItemId());
            final Map<String, Map<MultiSubject, Set<DocumentId>>> m = documentMapper.getDocMap(coll, false);
            byQuery = m.get(ltype);
        }
        final Map<MultiSubject, Set<DocumentId>> map = docMap.get(ltype);
        if (map == null) {
            return false;
        }
        final String jahr = Integer.toString((Integer) term.getParameter(NdsTerms.JAHR));
        final int hj = (Integer) term.getParameter(NdsTerms.HALBJAHR);
        final String docTypeDisplay = NbBundle.getMessage(PrimaryUnit.class, "primaryUnits.docTypes." + ltype);
        String kla;
        try {
            kla = resolver.resolveDisplayName(pu, term);
        } catch (IllegalAuthorityException ex) {
            kla = pu.getId();
        }
        final String lname = NbBundle.getMessage(PrimaryUnit.class, "primaryUnits.download.allelisten.title", kla, jahr, hj, docTypeDisplay);
        final String ldate = NbBundle.getMessage(PrimaryUnit.class, "primaryUnits.download.allelisten.date", new Date());
        list.setListName(lname);
        list.setListDate(ldate);
        ZensurenListe.Footnote reffn = null;
        ZensurenListe.Footnote vorschlagfn = null;
        for (final StudentId student : tgtae.getStudents(pu, null)) {
            Grade avsvVorschlag = null;
            final String sName = studentCardBean.get(student).getFN();
            final Marker sgl = getStudentSGL(student, fOPFormatter.termEnd(term.getScheduledItemId()));
            final ZensurenListe.DataLine l = list.addLine(sName);
            if (sgl != null) {
                l.setStudentHint(" (" + sgl.getShortLabel().replace("KGS ", "") + ")");
            }
//            double sum = 0d;
//            int count = 0;
//            final Map<Grade, Integer> gCount = new TreeMap((Comparator<Grade>) (g1, g2) -> collator.compare(g1.getShortLabel(), g2.getShortLabel()));

//
            for (Map.Entry<MultiSubject, Set<DocumentId>> e : map.entrySet()) {
                final Set<FastTermTargetDocument> fdocs = e.getValue().stream()
                        .map(fttd::get)
                        .collect(Collectors.toSet());
//                final Marker fach = e.getKey().getSubjectMarker();
                final Set<Marker> fach = e.getKey().getSubjectMarkerSet().stream()
                        //                        .map(Subject::getSubjectMarker)
                        .collect(Collectors.toSet());
                Grade g = null;
                DocumentId d = null;
                String flk = null;
                String msg = null;
                GradeReference gradeReference = null;
                boolean vorschlag = false;
                try {
                    d = documentMapper.find(fdocs, student, term.getScheduledItemId());
                    if (d != null) {
//                        altSubjectName = fttd.get(d).getAltSubjectName();
                        FastTermTargetDocument.Entry entry = fttd.get(d).selectEntry(student, term.getScheduledItemId());
                        g = entry != null ? entry.grade : null;
                        if (g != null && Uebertrag.NAME.equals(g.getConvention()) && !vorzensuren && before != null) {
                            gradeReference = (GradeReference) g;
                            entry = fttd.get(d).selectEntry(student, before.getScheduledItemId());
                            g = entry != null ? entry.grade : null;
                        } else if (g != null && AVSVVorschlag.NAME.equals(g.getConvention()) && "vorschlag".equals(g.getId())) {
                            if (avsvVorschlag == null) {
                                final Instance<VorschlagDecoration> select = extraAssessment.select(new ExtraAnnotation(ltype));
                                if (!select.isUnsatisfied() && !select.isAmbiguous()) {
                                    final GradeReference vr = (GradeReference) g;
                                    final Grade v = select.get().resolveReference(vr, student, term.getScheduledItemId(), null);
                                    if (v != null) {
                                        avsvVorschlag = v;
                                        vorschlag = true;
                                    }
                                } else {
                                    final String mesg = "No VorschlagDecoration for " + ltype;
                                    Logger.getLogger(NdsFormatter.class.getCanonicalName()).log(Level.WARNING, mesg);
                                }
                            }
                            if (avsvVorschlag != null) {
                                g = avsvVorschlag;
                                vorschlag = true;
                            }
                        }
                    } else if (vorzensuren && byQuery != null) {
                        Set<DocumentId> other = byQuery.get(e.getKey());
                        if (other != null) {
                            final Grade[] og = tgtae.findSingleChecked(pu, term.getScheduledItemId(), student, other);
                            if (og.length == 1) {
                                g = og[0];
                            } else if (og.length > 1) {
                                AmbiguousDocumentCollectionException amex = new AmbiguousDocumentCollectionException(student);
                                amex.setDocumentCollection(other.toArray(new DocumentId[other.size()]));
                                throw amex;
                            }
                        }
                    }
                } catch (AmbiguousResultException oex) {
                    final AmbiguousDocumentCollectionException ex = (AmbiguousDocumentCollectionException) oex;
                    ex.setStudentResolvedName(sName);
                    Logger.getLogger(NdsFormatter.class.getCanonicalName()).log(Level.INFO, ex.getLocalizedMessage());
                    msg = ex.getDocuments().length + " Werte!";
                }
                if (g != null) {
                    if (d != null) {
                        final Marker kurssgl = getDocumentSGL(tgtae, d);
                        flk = NdsFormatter.checkAndGetFLK(sgl, kurssgl);
                    }

                    final Grade lg;
                    if (g instanceof Grade.Biasable) {
                        lg = ((Grade.Biasable) g).getUnbiased();
                    } else {
                        lg = g;
                    }
//                    if (lg != null && lg instanceof NumberValueGrade) {
////                        double nv = ((NumberValueGrade) lg).getNumberValue().doubleValue();
////                        sum += nv;
////                        ++count;
//                    } 
//                    else if (lg != null && (lg.getConvention().equals(ASVAssessmentConvention.AV_NAME) || lg.getConvention().equals(ASVAssessmentConvention.SV_NAME))) {
//                        gCount.compute(lg, (gr, i) -> i != null ? ++i : 1);
//                    }
                }
//TODO WebUIConfiguration
                // final MultiSubject subject = e.getKey();
                final int tier = builderFactory.tier(e.getKey());
                final ZensurenListe.Column val;
                final String altSubjectName;
                if (e.getKey() instanceof MultiSubjectExt && (altSubjectName = ((MultiSubjectExt) e.getKey()).getAltSubject()) != null) {
                    val = list.setValue(l, tier, altSubjectName, g, msg);
                } else if (!fach.isEmpty()) {
                    val = list.setValue(l, tier, fach, g, msg);
                } else {//Value must be set in any case to this line
                    val = list.setValue(l, tier, "No subject", g, msg);
                }
                if (val != null) {
                    if (flk != null) {
                        val.setLevel(flk);
                    }
                    if (gradeReference != null) {
                        if (reffn == null) {
                            final int bhj = (Integer) before.getParameter(NdsTerms.HALBJAHR);
                            final String lbl = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatListe.uebertrag.label", bhj);
                            reffn = list.addFootnote(lbl);
                        }
                        val.setFootnote(reffn);
                    }
                    if (vorschlag) {
                        if (vorschlagfn == null) {
                            final String lbl = NbBundle.getMessage(NdsFormatter.class, "FopFormatter.formatListe.vorschlag.label");
                            vorschlagfn = list.addFootnote(lbl);
                        }
                        val.setFootnote(vorschlagfn);
                    }
                    if (val instanceof ZensurenListeXml.ColumnXml) {
                        final ZensurenListeXml.ColumnXml valXml = (ZensurenListeXml.ColumnXml) val;
                        final String color = builderFactory.getSchulvorlage().getColoring(g);
                        valXml.setColor(color);
                    }
                }
            }
//
//            if (count != 0) {
//                final double mean = sum / (double) count;
//                final String sVal = dFormat.formatReports(mean);
//                l.setNote("\u00F8: " + sVal); //Majuskel U+00D8 //  \u2300:    Glyph "⌀" (0x2300) not available in font "Helvetica".]]
//            } else 
//            if (!gCount.isEmpty()) {
//                final int num = gCount.values().stream().reduce((s, i) -> s + i).orElse(0);
//                if (num != 0) {
//                    final StringJoiner sj = new StringJoiner(", ");
//                    final String s = Integer.toString(num);
//                    gCount.forEach((g, i) -> sj.add(g.getShortLabel() + ": " + Integer.toString(i) + "/" + s));
//                    l.setNote(sj.toString());
//                }
//            }
        }
        return true;
    }

    private Marker getDocumentSGL(FastTargetDocuments2 tgtae, DocumentId d) {
        for (final Marker m : tgtae.getDocumentMarkers(d)) {
            if (m.getConvention().equals(sglConvention)) {
                return m;
            }
        }
        return null;
    }

    private Marker getStudentSGL(final StudentId student, final Date asOf) {
        final DocumentId d = builderFactory.forName(CommonDocuments.STUDENT_CAREERS_DOCID);
        return d != null ? sllb.getMarkerEntry(student, d, asOf) : null;
    }
}
