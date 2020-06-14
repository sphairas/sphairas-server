/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.openide.util.NbBundle;
import org.thespheres.betula.Identity;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.services.CommonTargetProperties;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.web.WebUIConfiguration;
import org.thespheres.betula.niedersachsen.Faecher;
import org.thespheres.betula.niedersachsen.Profile;
import org.thespheres.betula.niedersachsen.NdsTerms;
import org.thespheres.betula.niedersachsen.zeugnis.listen.ZensurenListeXml;
import org.thespheres.betula.niedersachsen.zeugnis.listen.ZensurenListenCollectionXml;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;
import org.thespheres.betula.server.beans.StudentsListsLocalBean;
import org.thespheres.betula.server.beans.StudentsLocalBean;
import org.thespheres.betula.server.beans.annot.Current;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.services.scheme.spi.TermNotFoundException;
import org.thespheres.betula.services.scheme.spi.TermSchedule;
import org.thespheres.betula.util.CollectionUtil;
import org.thespheres.betula.util.StudentComparator;
import org.thespheres.betula.web.PrimaryUnit;
import org.thespheres.betula.services.LocalProperties;
import org.thespheres.betula.document.model.MultiSubject;
import org.thespheres.betula.niedersachsen.kgs.SGL;
import org.thespheres.betula.niedersachsen.NdsZeugnisFormular;
import org.thespheres.betula.niedersachsen.NdsZeugnisFormular.ZeugnisMappe;
import org.thespheres.betula.niedersachsen.xml.NdsZeugnisSchulvorlage;
import org.thespheres.betula.niedersachsen.zeugnis.NdsReportBuilderFactory;
import org.thespheres.betula.niedersachsen.zeugnis.listen.DetailsListXml;
import org.thespheres.betula.niedersachsen.zeugnis.listen.StudentDetailsXml;
import org.thespheres.betula.niedersachsen.zeugnis.listen.ZensurenListeCsv;
import org.thespheres.betula.server.beans.FastTargetDocuments2;
import org.thespheres.betula.server.beans.NoEntityFoundException;
import org.thespheres.betula.server.beans.ReportsBean;
import org.thespheres.betula.services.ServiceConstants;
import org.thespheres.betula.services.ws.CommonDocuments;

/**
 *
 * @author boris.heithecker
 */
@Startup
@LocalBean //(name = "java:global/Betula_Web/FOPFormatter!org.thespheres.betula.web.docsrv.NdsFormatter", beanInterface = NdsFormatter.class)
@Singleton
//Annotation required to ensure that context.isCallerInRole works as espected
@RolesAllowed({"unitadmin", "signee"})
public class NdsFormatter {

    public static final String BACKGROUND_URL = "url(resource:org/thespheres/betula/web/docsrv/Probedruck.png)";
    @EJB
    private FormatListBean formatListBean;
    @EJB
    private NdsFormatDetailsBean formatDetailsBean;
    @EJB
    private NdsFormatReportsBean formatReportsBean2;
    @EJB(beanName = "StudentVCardsImpl")
    private StudentsLocalBean studentCardBean;
    @EJB(beanName = "ReportsBeanImpl")
    private ReportsBean zeugnisBean;
    @EJB
    private StudentsListsLocalBean sllb;
    @Inject
//    @SessionScoped
    private CommonTargetProperties targetProps;
    @Default
    @Inject
//    @SessionScoped
    private TermSchedule termSchedule;
    @Current
    @Inject
//    @SessionScoped
    private Term currentTerm;
    @Default
    @Inject
//    @SessionScoped
    private NamingResolver namingResolver;
    @Inject
//    @SessionScoped
    private WebUIConfiguration webConfig;
    @Inject
//    @SessionScoped
    private NdsReportBuilderFactory builderFactory; //TODO make this sessionscoped
    @Inject
//    @SessionScoped
    private LocalProperties properties;
    private JAXBContext jaxb;
    private Templates template;
    private FopFactory fopFactory;
    private TransformerFactory factory;
    private JAXBContext listJaxb;
    private JAXBContext detailsJaxb;
    private Templates listTemplate;
    private String sglConvention;
    private final NumberFormat dFormat = NumberFormat.getNumberInstance(Locale.GERMANY);
    private final Collator collator = Collator.getInstance(Locale.GERMANY);
    private String defaultEditingTargetType;
    private Templates detailsTemplate;
    @Resource
    protected EJBContext context;

    public NdsFormatter() {
    }

    @PostConstruct
    public void initialize() {
        Locale.setDefault(Locale.GERMANY);
        //        WebUIConfiguration webConfig = SystemProperties.findWebUIConfiguration();
//        studentSGLMarkerDocId = WebAppProperties.STUDENT_BILDUNGSGANG_DOCID;
        sglConvention = SGL.NAME;
//        Locale.setDefault(Locale.GERMANY);
        defaultEditingTargetType = webConfig.getDefaultCommitTargetType();
        try {
//                Context c = new InitialContext();
            jaxb = JAXBContext.newInstance(ZeugnisMappe.class);
            listJaxb = JAXBContext.newInstance(ZensurenListenCollectionXml.class);
            detailsJaxb = JAXBContext.newInstance(DetailsListXml.class);
            fopFactory = FopFactory.newInstance();
//            foUserAgent = fopFactory.newFOUserAgent();
            factory = TransformerFactory.newInstance();
            final String xslFoFile = builderFactory.getSchulvorlage().getXslFoFile();
            final InputStream is;
            if (xslFoFile != null) {
                final Path xslFo = findAppResourcesBase().resolve(xslFoFile);
                if (!Files.exists(xslFo)) {
                    throw new IllegalStateException("No XSL-FO file at " + xslFoFile);
                }
                is = Files.newInputStream(xslFo);
            } else {
                is = NdsFormatter.class.getResourceAsStream("empty.fo.xsl");
            }
            template = factory.newTemplates(new StreamSource(is));
            final InputStream is2 = ZensurenListenCollectionXml.class.getResourceAsStream("listen.fo.xsl");
            listTemplate = factory.newTemplates(new StreamSource(is2));
            final InputStream is3 = ZensurenListenCollectionXml.class.getResourceAsStream("details.fo.xsl");
            detailsTemplate = factory.newTemplates(new StreamSource(is3));
//            set2 = zeugnisConfigService.createTermReportNoteSetTemplate();
            dFormat.setMaximumFractionDigits(2);
            //
        } catch (JAXBException | TransformerConfigurationException | IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static Path findAppResourcesBase() {
        final String path = System.getProperty("com.sun.aas.instanceRoot");
        return Paths.get(path, ServiceConstants.APP_RESOURCES + "/");
    }

    public String getSglConvention() {
        return sglConvention;
    }

    public byte[] formatReports(final FastTargetDocuments2 tgtae, final UnitId pu, final Map<MultiSubject, Set<DocumentId>> docMap, final Map<MultiSubject, Set<DocumentId>> reports, final DocumentId zgnId, final TermId term, final StudentId student, final Date studentsAsOf, final String mime) throws IOException {

        final ArrayList<DocumentId> reportDoc = new ArrayList<>();

        Date termEnd = null;
        if (zgnId != null) {
            reportDoc.add(zgnId);
        } else if (term != null) {
            termEnd = termEnd(term);
            final Date useDate = studentsAsOf != null ? studentsAsOf : termEnd;
            for (final StudentId s : tgtae.getStudents(pu, useDate)) {
                if (student != null && !student.equals(s)) {
                    continue;
                }
                for (final DocumentId r : zeugnisBean.findTermReports(s, term, true)) {
                    reportDoc.add(r);
                }
            }
        }

        final ZeugnisMappe collection = new ZeugnisMappe();

        final Map<DocumentId, FastTermTargetDocument> map = new HashMap<>();
        docMap.values().stream()
                .flatMap(Set::stream)
                .forEach(d -> map.putIfAbsent(d, tgtae.getFastTermTargetDocument(d)));

        final Map<DocumentId, FastTextTermTargetDocument> rMap;
        if (reports != null) {
            rMap = reports.values().stream()
                    .flatMap(Set::stream)
                    .distinct()
                    .collect(Collectors.toMap(Function.identity(), tgtae::getFastTextTermTargetDocument));
        } else {
            rMap = Collections.EMPTY_MAP;
        }

        final boolean setBackground = !builderFactory.getSchulvorlage().getProperty(NdsZeugnisSchulvorlage.PROP_SIGNEES_NO_BACKGROUND)
                .map(p -> Boolean.parseBoolean(p.getValue()))
                .orElse(false) && !context.isCallerInRole("unitadmin");

        final boolean toXml = "text/xml".equals(mime);

        for (final DocumentId d : reportDoc) {
            final NdsZeugnisFormular data;
            try {
                data = formatReportsBean2.createReport(tgtae, d, pu, docMap, reports, map, rMap, termEnd, builderFactory, toXml);
//               formatReportsBean2.createZgn(tgtae, d, pu, docMap, reports, map, rMap, termEnd);
                if (data == null) {
                    continue;
                }
            } catch (NoEntityFoundException ex) {
                continue;
            }
            collection.add(data);
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        if (toXml) {

            try {
                // marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
                Marshaller marshaller = jaxb.createMarshaller();
                marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
                marshaller.marshal(collection, out);
            } catch (JAXBException ex) {
                throw new IOException(ex);
            } finally {
                try {
                    out.close();
                } catch (IOException ex) {
                }
            }

        } else {

            final DOMResult result = new DOMResult();
            try {
                jaxb.createMarshaller().marshal(collection, result);
            } catch (JAXBException ex) {
                throw new IOException(ex);
            }

            Term t = null;
            try {
                t = termSchedule.resolve(term);
            } catch (Exception ex) {
                Logger.getLogger(NdsFormatter.class.getCanonicalName()).log(Level.WARNING, ex.getMessage());
            }
            final FOUserAgent foUserAgent = createFOUserAgent(pu, t);

            try {
                // Construct fop with desired output formatReports
                final Fop fop = fopFactory.newFop(mime, foUserAgent, out);

                // Transform document to xsl-fo document
                // Setup XSLT
                final Transformer transformer = template.newTransformer();
                // Set the value of a <param> in the stylesheet
                transformer.setParameter("versionParam", "2.0");
                if (setBackground) {
                    transformer.setParameter("background-image", BACKGROUND_URL);
                }
                // Setup input for XSLT transformation
                final Source src = new DOMSource(result.getNode());
                // Resulting SAX events (the generated FO) must be piped through to FOP
                final DOMResult res = new DOMResult();
                // Start XSLT transformation and FOP processing
                transformer.transform(src, res);

                //Create fop output
                final Transformer fopTransformer = factory.newTransformer();
                final Source forSource = new DOMSource(res.getNode());
                final SAXResult fopResult = new SAXResult(fop.getDefaultHandler());
                fopTransformer.transform(forSource, fopResult);
            } catch (final Exception ex) {
                throw new IOException(ex);
            } finally {
                try {
                    out.close();
                } catch (IOException ex) {
                }
            }
        }
        return out.toByteArray();
    }

    public byte[] formatListe(FastTargetDocuments2 tgtae, UnitId pu, Map<String, Map<MultiSubject, Set<DocumentId>>> docMap, String[] listTypes, NamingResolver resolver, Term current, Term before) throws IOException {
        final Map<DocumentId, FastTermTargetDocument> fttd = new HashMap<>();
        docMap.values().stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .forEach(d -> fttd.putIfAbsent(d, tgtae.getFastTermTargetDocument(d)));

        final ZensurenListenCollectionXml collection = new ZensurenListenCollectionXml();
        collection.setFooterCenter(builderFactory.getSchulvorlage().getSchoolName());

        for (final String ltype : listTypes) {
            formatListBean.oneListe(false, tgtae, pu, current, ltype, docMap, resolver, fttd, before, collection);
        }
        if (before != null) {
            formatListBean.oneListe(true, tgtae, pu, before, "zeugnisnoten", docMap, resolver, fttd, before, collection);
        }

        final DOMResult result = new DOMResult();
        try {
            listJaxb.createMarshaller().marshal(collection, result);
        } catch (JAXBException ex) {
            throw new IOException(ex);
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final FOUserAgent foUserAgent = createFOUserAgent(pu, current);

        try {
            // Construct fop with desired output formatReports
            Fop fop = fopFactory.newFop(org.apache.xmlgraphics.util.MimeConstants.MIME_PDF, foUserAgent, out);

            // Setup XSLT
            Transformer transformer = listTemplate.newTransformer();
            // Set the value of a <param> in the stylesheet
            transformer.setParameter("versionParam", "2.0");
            // Setup input for XSLT transformation
            Source src = new DOMSource(result.getNode());
            // Resulting SAX events (the generated FO) must be piped through to FOP
            DOMResult res = new DOMResult();
            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);

            transformer = factory.newTransformer();
            src = new DOMSource(res.getNode());
            SAXResult finalres = new SAXResult(fop.getDefaultHandler());
            transformer.transform(src, finalres);

            return out.toByteArray();
        } catch (Exception ex) {
            throw new IOException(ex);
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
            }
        }
    }

    public byte[] csv(final FastTargetDocuments2 tgtae, final UnitId pu, Map<String, Map<MultiSubject, Set<DocumentId>>> docMap, final String[] listTypes, final NamingResolver resolver, final Term current, final Term before, final String enc) throws IOException {
        final Map<DocumentId, FastTermTargetDocument> fttd = new HashMap<>();
        docMap.values().stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .forEach(d -> fttd.putIfAbsent(d, tgtae.getFastTermTargetDocument(d)));

        final List<ZensurenListeCsv> coll = new ArrayList<>();
        for (final String ltype : listTypes) {
            formatListBean.oneListeCsv(false, tgtae, pu, current, ltype, docMap, resolver, fttd, before, coll);
        }
        formatListBean.oneListeCsv(true, tgtae, pu, before, "zeugnisnoten", docMap, resolver, fttd, before, coll);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (final ZensurenListeCsv p : coll) {
                final ZipEntry ze = new ZipEntry(p.getListName() + ".csv");
                zos.putNextEntry(ze);
                final byte[] str = p.toString(enc);
                zos.write(str, 0, str.length);
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    public byte[] formatDetails(final Collection<StudentId> students, final Map<DocumentId, FastTermTargetDocument> targets, final Map<DocumentId, FastTermTargetDocument> agTargets, final Map<TermId, Map<String, Map<MultiSubject, Set<DocumentId>>>> map, UnitId pu, Term current, String mime, int preTermsCount) throws IOException {

        final DetailsListXml collection = new DetailsListXml();
        collection.setFooterCenter(builderFactory.getSchulvorlage().getSchoolName());

        final String jahr = Integer.toString((Integer) current.getParameter(NdsTerms.JAHR));
        final int hj = (Integer) current.getParameter(NdsTerms.HALBJAHR);
        String kla;
        try {
            kla = namingResolver.resolveDisplayName(pu, current);
        } catch (IllegalAuthorityException ex) {
            kla = pu.getId();
        }
        final String ldate = NbBundle.getMessage(PrimaryUnit.class, "primaryUnits.download.details.date", new Date());

        for (final StudentId student : students) {

            final Marker sgl = getStudentSGL(student, termEnd(current.getScheduledItemId()));
            final MappedStudent card = new MappedStudent(student, sgl);
            card.setVCard(studentCardBean.get(student));
            final String lname = NbBundle.getMessage(PrimaryUnit.class, "primaryUnits.download.details.title", card.getDisplayName(), kla, jahr, hj);
            final StudentDetailsXml details = new StudentDetailsXml();
            details.setListDate(ldate);
            details.setListName(lname);
            details.setSortString(StudentComparator.sortStringFromDirectoryName(card.getDirectoryName()));
            formatDetailsBean.oneStudent(details, card, pu, current, preTermsCount, map, targets, agTargets);

            collection.list.add(details);
        }

        Collections.sort(collection.list, Comparator.comparing(StudentDetailsXml::getSortString, collator));

        DOMResult result = new DOMResult();

        try {
            detailsJaxb.createMarshaller().marshal(collection, result);
        } catch (JAXBException ex) {
            throw new IOException(ex);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        final FOUserAgent foUserAgent = createFOUserAgent(pu, current);

        try {
            // Construct fop with desired output formatReports
            Fop fop = fopFactory.newFop(mime, foUserAgent, out);

            // Setup XSLT
            Transformer transformer = detailsTemplate.newTransformer();
            // Set the value of a <param> in the stylesheet
            transformer.setParameter("versionParam", "2.0");
            // Setup input for XSLT transformation
            Source src = new DOMSource(result.getNode());
            // Resulting SAX events (the generated FO) must be piped through to FOP
            DOMResult res = new DOMResult();
            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);

            transformer = factory.newTransformer();
            src = new DOMSource(res.getNode());
            SAXResult finalres = new SAXResult(fop.getDefaultHandler());
            transformer.transform(src, finalres);

            return out.toByteArray();
        } catch (Exception ex) {
            throw new IOException(ex);
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
            }
        }
    }

    public byte[] formatCSVListe(FastTargetDocuments2 tgtae, DocumentId[] target, NamingResolver resolver, Term current, Term[] before, String mime, final String encoding) throws IOException {

        final FastTermTargetDocument[] fttds = Arrays.stream(target)
                .map(tgtae::getFastTermTargetDocument)
                .filter(Objects::nonNull)
                .toArray(FastTermTargetDocument[]::new);

        if (fttds.length == 0) {
            return null;
        }

        final Marker fach = Arrays.stream(target)
                .map(tgtae::getDocumentMarkers)
                .flatMap(Collection::stream)
                .filter(m -> m.getConvention().equals(Faecher.CONVENTION_NAME) || m.getConvention().equals(Profile.CONVENTION_NAME))
                .collect(CollectionUtil.singleOrNull());

        class LineData implements Comparable {

            private final Collator collator = Collator.getInstance(Locale.GERMANY);
            final String fName;
            private String pu;
            private String sgl;
            private String[] values;

            private LineData(String fName) {
                this.fName = fName;
            }

            @Override
            public int compareTo(Object o) {
                return collator.compare(fName, ((LineData) o).fName);
            }

            private String toLine() {
                StringJoiner sj = new StringJoiner(";", "", "\n");
                sj.add(fName);
                sj.add(pu != null ? pu : "");
                sj.add(sgl != null ? sgl : "");
                Arrays.stream(values).forEach(sj::add);
                return sj.toString();
            }

        }
        final ArrayList<LineData> list = new ArrayList<>();
        for (FastTermTargetDocument fttd : fttds) {
            for (StudentId student : fttd.getStudents(current.getScheduledItemId())) {
                final String sName = studentCardBean.get(student).getFN();
                final Marker sgl = getStudentSGL(student, null);
                final UnitId studentPu = sllb.findPrimaryUnit(student, null);
                final LineData sData = new LineData(sName);
                list.add(sData);
                if (studentPu != null) {
                    try {
                        final String pudn = resolver.resolveDisplayNameResult(studentPu).getResolvedName(current);
                        if (pudn != null) {
                            sData.pu = pudn;
                        }
                    } catch (IllegalAuthorityException ex) {
                    }
                }
                if (sgl != null) {
                    sData.sgl = sgl.getShortLabel().replace("KGS ", "");
                }

                final String[] values = new String[before.length + 1];
                int i = 0;
                for (Term vornoten : before) {
                    String v = oneCsvGrade(vornoten, fttd, student, fach, tgtae, sgl);
                    values[i++] = v;
                }
                String v = oneCsvGrade(current, fttd, student, fach, tgtae, sgl);
                values[i] = v;
                sData.values = values;
            }
        }
        Collections.sort(list);

        LineData headers = new LineData("Name, Vorname");
        headers.pu = "Klasse";
        headers.sgl = "Schulzweig";
        headers.values = new String[before.length + 1];
        int i = 0;
        for (Term vornoten : before) {
            headers.values[i++] = vornoten.getDisplayName();
        }
        headers.values[i] = current.getDisplayName();

        list.add(0, headers);

        StringBuilder sb = new StringBuilder();

        list.stream().forEach(sd -> sb.append(sd.toLine()));

        return sb.toString().getBytes(encoding);
    }

    private String oneCsvGrade(Term vornoten, FastTermTargetDocument fttd, StudentId student, final Marker fach, FastTargetDocuments2 tgtae, final Marker sgl) {
        final TermId tid = vornoten.getScheduledItemId();
        final FastTermTargetDocument.Entry entry = fttd.selectEntry(student, tid);
        Grade g = entry != null ? entry.grade : null;
        if (g == null && fach != null) {
            g = tgtae.findSingle(student, tid, fach, defaultEditingTargetType);
        }
        String flk = null;
        if (g != null) {
            Marker kurssgl = getDocumentSGL(tgtae, fttd.getDocument());
            flk = checkAndGetFLK(sgl, kurssgl);
        }
        String v = g != null ? g.getShortLabel() : "";
        if (flk != null) {
            v += "[" + flk + "]";
        }
        return v;
    }

    public byte[] formatKursListe(FastTargetDocuments2 tgtae, DocumentId[] target, NamingResolver resolver, Term current, Term[] before, String mime, Identity<String> display) throws IOException {
        FastTermTargetDocument[] fttds = Arrays.stream(target)
                .map(tgtae::getFastTermTargetDocument)
                .filter(Objects::nonNull)
                .toArray(FastTermTargetDocument[]::new);

        if (fttds.length == 0) {
            return null;
        }

        Marker fach = Arrays.stream(target)
                .map(tgtae::getDocumentMarkers)
                .flatMap(Collection::stream)
                .filter(m -> m.getConvention().equals(Faecher.CONVENTION_NAME) || m.getConvention().equals(Profile.CONVENTION_NAME))
                .collect(CollectionUtil.singleOrNull());

        ZensurenListenCollectionXml collection = new ZensurenListenCollectionXml();
        collection.setFooterCenter(builderFactory.getSchulvorlage().getSchoolName());

        ZensurenListeXml list = new ZensurenListeXml();
        list.firstColumnWidth = "8.0cm";
        String jahr = Integer.toString((Integer) current.getParameter(NdsTerms.JAHR));
        int hj = (Integer) current.getParameter(NdsTerms.HALBJAHR);
        String kla;
        try {
            kla = resolver.resolveDisplayNameResult(display).getResolvedName(current);
        } catch (IllegalAuthorityException ex) {
            kla = display.getId();

        }
        String lname = NbBundle.getMessage(PrimaryUnit.class, "primaryUnits.download.kurs.title", kla, jahr, hj);
        String ldate = NbBundle.getMessage(PrimaryUnit.class, "primaryUnits.download.allelisten.date", new Date());
        list.setListName(lname);
        list.setListDate(ldate);
        for (FastTermTargetDocument fttd : fttds) {
            for (StudentId student : fttd.getStudents(current.getScheduledItemId())) {
                String sName = studentCardBean.get(student).getFN();
                Marker sgl = getStudentSGL(student, termEnd(current.getScheduledItemId()));
                UnitId studentPu = sllb.findPrimaryUnit(student, null);
//            ZensurenListeXml.DataLineXml l = list.addLine(sName);
                StringJoiner sj = new StringJoiner(", ", "(", ")");
                if (studentPu != null) {
                    try {
                        String pudn = resolver.resolveDisplayNameResult(studentPu).getResolvedName(current);
                        if (pudn != null) {
                            sj.add(pudn);
                        }
                    } catch (IllegalAuthorityException ex) {
                    }
                }
                if (sgl != null) {
                    sj.add(sgl.getShortLabel().replace("KGS ", ""));
                }
//            l.setStudentHint(sj.toString());
                ZensurenListeXml.DataLineXml l = list.addLine(sName + " " + sj.toString());
                Term[] noten;
                boolean includeCurrentTerm = true;
                if (includeCurrentTerm) {
                    noten = Arrays.copyOf(before, before.length + 1);
                    noten[noten.length - 1] = current;
                } else {
                    noten = before;
                }
                for (Term vornoten : noten) {
                    TermId tid = vornoten.getScheduledItemId();
                    FastTermTargetDocument.Entry entry = fttd.selectEntry(student, tid);
                    Grade g = entry != null ? entry.grade : null;
                    if (g == null && fach != null) {
                        g = tgtae.findSingle(student, tid, fach, defaultEditingTargetType);
                    }
                    String flk = null;
                    if (g != null) {
                        Marker kurssgl = getDocumentSGL(tgtae, fttd.getDocument());
                        flk = checkAndGetFLK(sgl, kurssgl);
                    }

                    ZensurenListeXml.ColumnXml val = list.setValue(l, vornoten, g, null);
                    if (flk != null) {
                        val.setLevel(flk);
                    }
                }
            }
        }
        list.sort();
        collection.LISTS.add(list);

        DOMResult result = new DOMResult();
        try {
            listJaxb.createMarshaller().marshal(collection, result);
        } catch (JAXBException ex) {
            throw new IOException(ex);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        final FOUserAgent foUserAgent = createFOUserAgent(display, current);

        try {
            // Construct fop with desired output formatReports
            Fop fop = fopFactory.newFop(mime, foUserAgent, out);

            // Setup XSLT
            Transformer transformer = listTemplate.newTransformer();
            // Set the value of a <param> in the stylesheet
            transformer.setParameter("versionParam", "2.0");
            // Setup input for XSLT transformation
            Source src = new DOMSource(result.getNode());
            // Resulting SAX events (the generated FO) must be piped through to FOP
            DOMResult res = new DOMResult();
            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);

            transformer = factory.newTransformer();
            src = new DOMSource(res.getNode());
            SAXResult finalres = new SAXResult(fop.getDefaultHandler());
            transformer.transform(src, finalres);

            return out.toByteArray();
        } catch (Exception ex) {
            throw new IOException(ex);
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
            }
        }
    }

    FOUserAgent createFOUserAgent(final Identity<?> pu, final Term term) throws MalformedURLException {
        final FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
        final Path p = findAppResourcesBase();
        final URL base = p.toUri().toURL();
        foUserAgent.setBaseURL(base.toExternalForm());
        //no file foUserAgent, create new every time
        foUserAgent.setAuthor(builderFactory.getSchulvorlage().getSchoolName());//??Schule
        foUserAgent.setCreator("sphairas");//Betula
        foUserAgent.setProducer("Apache FOP");//Apache FOP
        foUserAgent.setCreationDate(new Date());
        if (pu != null) {
            try {
                final String title = namingResolver.resolveDisplayNameResult(pu).getResolvedName(term);
                foUserAgent.setTitle(title);
            } catch (Exception ex) {
                Logger.getLogger(NdsFormatter.class.getCanonicalName()).log(Level.WARNING, ex.getMessage());
            }
        }
        foUserAgent.setURIResolver(new ResourceResolverAdapter());
        return foUserAgent;
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

    public Date termEnd(final TermId term) {
        if (!currentTerm.getScheduledItemId().equals(term)) {
            try {
                final Term t = termSchedule.resolve(term);
                return t.getEnd();
            } catch (TermNotFoundException | IllegalAuthorityException ex) {
                Logger.getLogger(NdsFormatter.class.getCanonicalName()).log(Level.WARNING, ex.getMessage());
            }
        }
        return null;
    }

    static String checkAndGetFLK(Marker ssgl, Marker kurssgl) {
        if (kurssgl == null || ssgl == null) {
            return null;
        }
        if (!ssgl.equals(kurssgl)) {
            return kurssgl.getId().substring(0, 1).toUpperCase();
        }
        return null;
    }

}
