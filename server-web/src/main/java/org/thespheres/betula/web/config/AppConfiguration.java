/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.config;

import org.thespheres.betula.server.beans.clients.ServiceInternalClient;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Named;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.InvalidNameException;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.naming.directory.DirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.openide.util.Lookup;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.thespheres.betula.assess.AssessmentConvention;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.model.Subject;
import org.thespheres.betula.niedersachsen.NdsCommonConstants;
import org.thespheres.betula.niedersachsen.gs.CrossmarkSettings;
import org.thespheres.betula.niedersachsen.zeugnis.NdsReportBuilderFactory;
import org.thespheres.betula.niedersachsen.xml.NdsZeugnisSchulvorlage;
import org.thespheres.betula.niedersachsen.zeugnis.TermReportNoteSetTemplate;
import org.thespheres.betula.server.beans.MissingConfigurationResourceException;
import org.thespheres.betula.server.beans.config.CommonAppProperties;
import org.thespheres.betula.services.ServiceConstants;
import org.thespheres.betula.services.web.WebUIConfiguration;
import org.thespheres.betula.services.web.XmlWebUIConfiguration;
import org.thespheres.betula.util.CollectionUtil;
import org.thespheres.betula.web.BetulaWebApplication;
import org.thespheres.betula.web.Util;

/**
 *
 * @author boris.heithecker
 */
@Named("config")
@ApplicationScoped
public class AppConfiguration implements Serializable {

    private static final String SERVER_CRT_FILE = "server.crt";
    private javax.xml.bind.JAXBContext notesTemplateJAXB;
    private javax.xml.bind.JAXBContext vorlageJAXB;
    private javax.xml.bind.JAXBContext webUIJAXB;
    private javax.xml.bind.JAXBContext crossmarkSettingsJAXB;
//    private Date noteSetFileLastModified;
    private TermReportNoteSetTemplate noteSetTemplate;
    private XmlWebUIConfiguration webUIConfig;

//  Siehe Anmerkung bei ServiceInternalClient.java  
//    @Inject
//    @RestClient
    private ServiceInternalClient internalClient;
    private CrossmarkSettings crossmarks;
    private List<Grade> crossMarkGrade;
    private NdsReportBuilderFactory reportBuilderFactory;

    @PostConstruct
    public void initialize() {
        internalClient = RestClientBuilder.newBuilder()
                .baseUri(URI.create(ServiceInternalClient.URI_SERVICE_API))
                .hostnameVerifier((hostname, session) -> true) // Optional: specific verifier
                .build(ServiceInternalClient.class);
        try {
            notesTemplateJAXB = javax.xml.bind.JAXBContext.newInstance(TermReportNoteSetTemplate.class);
        } catch (javax.xml.bind.JAXBException ex) {
            throw new IllegalStateException(ex);
        }
        try {
            vorlageJAXB = javax.xml.bind.JAXBContext.newInstance(NdsZeugnisSchulvorlage.class);
        } catch (javax.xml.bind.JAXBException ex) {
            throw new IllegalStateException(ex);
        }
        try {
            webUIJAXB = javax.xml.bind.JAXBContext.newInstance(XmlWebUIConfiguration.class);
        } catch (javax.xml.bind.JAXBException ex) {
            throw new IllegalStateException(ex);
        }
        try {
            crossmarkSettingsJAXB = javax.xml.bind.JAXBContext.newInstance(CrossmarkSettings.class);
        } catch (javax.xml.bind.JAXBException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public ServiceInternalClient getInternalClient() {
        return internalClient;
    }

    @Produces
    public XmlWebUIConfiguration getWebUIConfiguration() {
        if (webUIConfig == null) {
            webUIConfig = findWebUIConfiguration();
        }
        return webUIConfig;
    }

    private XmlWebUIConfiguration findWebUIConfiguration() {
        final String bp = null; //getProvider();
        if (bp != null) {
            final WebUIConfiguration swc = Lookup.getDefault().lookupAll(WebUIConfiguration.class).stream()
                    .filter(wc -> wc.getName() != null)
                    .filter(wc -> wc.getName().equals(bp))
                    .collect(CollectionUtil.singleOrNull());
            if (swc != null) {
                return (XmlWebUIConfiguration) swc;
            }
        }
//        throw new ConfigurationException(WebUIConfiguration.class.getName(), WebAppProperties.BETULA_WEB_UI_SERVICE_PROVIDER_PROPERTY);
        final DirContext dc = lookupAppResourcesContext();
        final String file = "web-ui-configuration.xml";
        final Resource res;
        try {
            res = (Resource) dc.lookup(file);
        } catch (NamingException ex) {
            Logger.getLogger(AppConfiguration.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
            throw new MissingConfigurationResourceException(file);
        }
        try (final InputStream is = res.streamContent()) {
            return (XmlWebUIConfiguration) webUIJAXB.createUnmarshaller().unmarshal(is);
        } catch (IOException | javax.xml.bind.JAXBException ex) {
            final MissingConfigurationResourceException th = new MissingConfigurationResourceException(file);
            th.initCause(ex);
            throw th;
        }
    }

    X509Certificate findServerCertificate() {
        final Path file = Paths.get(System.getenv("SECRETS"), SERVER_CRT_FILE);
        final Certificate[] certs;
        try (final InputStream is = Files.newInputStream(file)) {
            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
            certs = cf.generateCertificates(is).stream()
                    .toArray(Certificate[]::new);
        } catch (IOException | CertificateException ex) {
            Logger.getLogger(AppConfiguration.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
            final ConfigurationException th = new ConfigurationException(SERVER_CRT_FILE, "certificate");
            th.initCause(ex);
            throw th;
        }
        if (certs.length > 0) {
            final X509Certificate cert = (X509Certificate) certs[0];
            return cert;
        }
        throw new MissingConfigurationResourceException(SERVER_CRT_FILE);
    }

//    @Produces
//    public Comparator<Subject> findSubjectComparator(NdsReportBuilderFactory fac) {
//        return (s1, s2) -> fac.forCareer(null).compare(s1.getSubjectMarker(), s2.getSubjectMarker());
//    }
    @Typed(NdsReportBuilderFactory.class)
    @Produces
    public NdsReportBuilderFactory getReportBuilderFactory() {
        if (reportBuilderFactory == null) {
            reportBuilderFactory = findZeugnisConfiguratorService();
        }
        return reportBuilderFactory;
    }

    private NdsReportBuilderFactory findZeugnisConfiguratorService() {
        final DirContext dc = lookupAppResourcesContext();
        final String file = NdsReportBuilderFactory.SCHULVORLAGE_FILE;
        final NdsZeugnisSchulvorlage vorlage;
        if (hasResource(dc, NdsReportBuilderFactory.SCHULVORLAGE_FILE)) {
            final Resource res;
            try {
                res = (Resource) dc.lookup(file);
            } catch (NamingException ex) {
                Logger.getLogger(AppConfiguration.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
                throw new MissingConfigurationResourceException(file);
            }
            try (final InputStream is = res.streamContent()) {
                vorlage = (NdsZeugnisSchulvorlage) vorlageJAXB.createUnmarshaller().unmarshal(is);
            } catch (IOException | javax.xml.bind.JAXBException ex) {
                final MissingConfigurationResourceException th = new MissingConfigurationResourceException(file);
                th.initCause(ex);
                throw th;
            }
        } else {
            vorlage = new NdsZeugnisSchulvorlage(CommonAppProperties.provider());
        }
        configureTemplate(vorlage);
        return new NdsReportBuilderFactory(vorlage);
    }

    private void configureTemplate(final NdsZeugnisSchulvorlage vorlage) {
        final X509Certificate server = findServerCertificate();
        final String subject = server.getSubjectX500Principal().getName();
        final Map<String, Object> m;
//            m.get("ST").toString();//Bundesland
        try {
            m = new LdapName(subject).getRdns().stream()
                    .collect(Collectors.toMap(Rdn::getType, Rdn::getValue));
        } catch (final InvalidNameException ex) {
            final ConfigurationException th = new ConfigurationException(SERVER_CRT_FILE, "subject");
            th.initCause(ex);
            throw th;
        }
        final Object org = m.get("O");
        if (vorlage.getSchoolName() == null && org != null) {
            vorlage.setSchoolName(org.toString());
        }
        final Object loc = m.get("L");
        if (vorlage.getSchoolLocation() == null && loc != null) {
            vorlage.setSchoolLocation(loc.toString());
        }
    }

//    @SessionScoped
//    @jakarta.faces.view.ViewScoped
//    @RequestScoped
    @Dependent  //soll sessionscoped, aber funktioniert nicht;TermReportNoteSetTemplate muss serializable sein
    @Produces
    //Do not cache!!!
    public TermReportNoteSetTemplate findTermReportNoteSetTemplate() throws NamingException {
        final DirContext dc = lookupAppResourcesContext();
        final String file = NdsReportBuilderFactory.SIGNEE_BEMERKUNGEN_FILE;
        try {
            dc.lookup(file);
        } catch (NamingException nex) {
            final String msg = "No file " + file + " found!";
            Logger.getLogger(AppConfiguration.class.getPackage().getName()).log(Level.WARNING, msg);
            return new TermReportNoteSetTemplate("null");
        }
        Date lm = null;
        try {
            final ResourceAttributes attr = (ResourceAttributes) dc.getAttributes(file);
            if (attr != null) {
                lm = attr.getCreationOrLastModifiedDate();
            }
        } catch (NamingException | ClassCastException ex) {
            Logger.getLogger(AppConfiguration.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
            throw ex;
        }
//        if (noteSetFileLastModified != null && noteSetTemplate != null && lm != null && !lm.after(noteSetFileLastModified)) {
//            return noteSetTemplate;
//        }
        final Resource res;
        try {
            res = (Resource) dc.lookup(file);
        } catch (NamingException ex) {
            Logger.getLogger(AppConfiguration.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
            throw new MissingConfigurationResourceException(file);
        }
        try (final InputStream is = res.streamContent()) {
            noteSetTemplate = (TermReportNoteSetTemplate) notesTemplateJAXB.createUnmarshaller().unmarshal(is);
        } catch (IOException | javax.xml.bind.JAXBException ex) {
            final MissingConfigurationResourceException th = new MissingConfigurationResourceException(file);
            th.initCause(ex);
            throw th;
        }
//        noteSetFileLastModified = lm;
        return noteSetTemplate;
    }

    @Dependent
    @Produces
    public CrossmarkSettings getCrossmarkSettings() {
        if (crossmarks == null) {
            crossmarks = createCrossmarkSettings();
        }
        return crossmarks;
    }

    private CrossmarkSettings createCrossmarkSettings() {
        final DirContext dc = lookupAppResourcesContext();
        final String file = NdsCommonConstants.ANKREUZZEUGNISSE_FILE;
        final Resource res;
        try {
            res = (Resource) dc.lookup(file);
        } catch (NamingException ex) {
            return new CrossmarkSettings();
        }
        try (final InputStream is = res.streamContent()) {
            return (CrossmarkSettings) crossmarkSettingsJAXB.createUnmarshaller().unmarshal(is);
        } catch (IOException | javax.xml.bind.JAXBException ex) {
            final MissingConfigurationResourceException th = new MissingConfigurationResourceException(file);
            th.initCause(ex);
            throw th;
        }
    }

    private DirContext lookupAppResourcesContext() {
        try {
            final Context c = new InitialContext();
            return (DirContext) c.lookup("java:global/Betula_Server/Betula_Persistence/AppResourcesContext");
        } catch (NamingException ne) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
            throw new RuntimeException(ne);
        }
    }

    public static boolean hasResource(final DirContext dc, final String res) {
        try {
            final NamingEnumeration<NameClassPair> l = dc.list("");
            while (l.hasMore()) {
                if (l.next().getName().equals(res)) {
                    return true;
                }
            }
        } catch (NamingException ex) {
            Logger.getLogger(AppConfiguration.class.getPackage().getName()).log(Level.WARNING, "An exception occured listing resources in " + dc.toString(), ex); //Vor Jakarta dc.getContextName()
        }
        return false;
    }

    public StreamedContent getImage() {
        final String image = getWebUIConfiguration().getLogoResource();
        if (image != null) {
            try {
                final Path rp = ServiceConstants.configBase().resolve(image);
                final InputStream is = Files.newInputStream(rp);
                return DefaultStreamedContent.builder()
                        .stream(() -> is)
                        .contentType("image/png")
                        .build();
            } catch (IOException ex) {
                Logger.getLogger(BetulaWebApplication.class.getName()).log(Level.WARNING, ex.getLocalizedMessage(), ex);
            }
        }
        return null;
    }

    public List<Grade> getExtraGrades() {
        final String extra = getWebUIConfiguration().getProperty("extra.grades.permitted");
        return Optional.ofNullable(extra)
                .map(p -> p.split(","))
                .map(Arrays::stream)
                .orElse(Stream.empty())
                .map(Util::find)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Grade> getCrossMarkGrades() {
        if (crossMarkGrade == null) {
            final List<Grade> l = new CopyOnWriteArrayList<>();
            final AssessmentConvention ac = getCrossMarkAssessmentConvention();
            if (ac != null) {
                l.addAll(Arrays.asList(ac.getAllGradesReverseOrder()));
            }
            l.addAll(getExtraGrades());
            crossMarkGrade = l;
        }
        return crossMarkGrade;
    }

    public List<String> getCrossMarkSubjectConventions() {
        return Arrays.asList(crossmarks.conventions());
    }

    public AssessmentConvention getCrossMarkAssessmentConvention() {
        return crossmarks.getAssessmentConvention();
    }

    public String[] getTargetTypes() {
        return getWebUIConfiguration().getCommitTargetTypes();
//        return new String[]{"quartalsnoten", "zeugnisnoten", "arbeitsverhalten", "sozialverhalten"};
    }

    public Comparator<Subject> getSubjectComparator() {
        return (s1, s2) -> getReportBuilderFactory().forCareer(null).compare(s1.getSubjectMarker(), s2.getSubjectMarker());
    }

    public String getAppName() {
        return "sphairas";
    }

    public boolean isSettingsEnabled() {
        return false;
//        return true;
    }

    public String getHelpLink() {
//                return null;
        return "https://www.sphairas.de/faq";
    }

    public String getCopyrightFooter() {
        return "© sphairas";
    }

    public String getPrivacyPolicyUrl() {
        return null;
    }
}
