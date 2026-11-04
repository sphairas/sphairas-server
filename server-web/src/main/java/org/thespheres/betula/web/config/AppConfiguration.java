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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
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
import org.thespheres.betula.server.beans.clients.InternalParamConverterProvider;
import org.thespheres.betula.server.beans.clients.ServiceClientConfiguration;
import org.thespheres.betula.server.beans.MissingConfigurationResourceException;
import org.thespheres.betula.server.beans.clients.json.JsonbContextResolver;
import org.thespheres.betula.server.beans.config.CommonAppProperties;
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

    private ServiceInternalClient internalClient;
    private CrossmarkSettings crossmarks;
    private List<Grade> crossMarkGrade;
    private NdsReportBuilderFactory reportBuilderFactory;

    @PostConstruct
    public void initialize() {
        internalClient = ServiceClientConfiguration.newRestClientBuilder()
                .register(InternalParamConverterProvider.class)
                .register(JsonbContextResolver.class)
                .register(BadRequestResponseMapper.class)
                .register(AmbiguousDateResponseMapper.class)
                .register(TermReportDataResponseMapper.class)
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
        final String file = "web-ui-configuration.xml";
        try (final InputStream is = openAppResource(file)) {
            if (is == null) {
                throw new MissingConfigurationResourceException(file);
            }
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
        final String file = NdsReportBuilderFactory.SCHULVORLAGE_FILE;
        NdsZeugnisSchulvorlage vorlage = null;
        try (final InputStream is = openAppResource(file)) {
            if (is != null) {
                vorlage = (NdsZeugnisSchulvorlage) vorlageJAXB.createUnmarshaller().unmarshal(is);
            }
        } catch (IOException | javax.xml.bind.JAXBException ex) {
            final MissingConfigurationResourceException th = new MissingConfigurationResourceException(file);
            th.initCause(ex);
            throw th;
        }
        if (vorlage == null) {
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
        final String file = NdsReportBuilderFactory.SIGNEE_BEMERKUNGEN_FILE;
        try (final InputStream is = openAppResource(file)) {
            if (is == null) {
                Logger.getLogger(AppConfiguration.class.getPackage().getName()).log(Level.WARNING, "No file " + file + " found!");
                return new TermReportNoteSetTemplate("null");
            }
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
        final String file = NdsCommonConstants.ANKREUZZEUGNISSE_FILE;
        try (final InputStream is = openAppResource(file)) {
            if (is == null) {
                return new CrossmarkSettings();
            }
            return (CrossmarkSettings) crossmarkSettingsJAXB.createUnmarshaller().unmarshal(is);
        } catch (IOException | javax.xml.bind.JAXBException ex) {
            final MissingConfigurationResourceException th = new MissingConfigurationResourceException(file);
            th.initCause(ex);
            throw th;
        }
    }

    /**
     * Opens an app resource via the service REST API.
     *
     * @return the content, or null if the resource does not exist
     */
    public InputStream openAppResource(final String file) throws IOException {
        try (final jakarta.ws.rs.core.Response r = getInternalClient().getAppResource(file)) {
            if (r.getStatus() == 404) {
                return null;
            }
            if (r.getStatus() < 200 || r.getStatus() >= 300) {
                throw new IOException("Service returned HTTP " + r.getStatus()
                        + " reading app resource " + file);
            }
            return new java.io.ByteArrayInputStream(r.readEntity(byte[].class));
        } catch (jakarta.ws.rs.WebApplicationException ex) {
            if (ex.getResponse().getStatus() == 404) {
                return null;
            }
            throw new IOException("Cannot read app resource " + file, ex);
        }
    }

    public StreamedContent getImage() {
        final String image = getWebUIConfiguration().getLogoResource();
        if (image != null) {
            final Path rp = org.thespheres.betula.web.docsrv.NdsFormatter.findAppResourcesBase().resolve(image);
            // The InputStream MUST be opened inside the supplier lambda, not here.
            // p:graphicImage makes a separate HTTP GET to fetch the bytes; by the
            // time that request arrives the eagerly-opened stream would already be
            // closed/consumed and PrimeFaces would serve an empty or broken image.
            return DefaultStreamedContent.builder()
                    .stream(() -> {
                        try {
                            return Files.newInputStream(rp);
                        } catch (IOException ex) {
                            Logger.getLogger(BetulaWebApplication.class.getName()).log(Level.WARNING, ex.getLocalizedMessage(), ex);
                            return null;
                        }
                    })
                    .contentType("image/png")
                    .build();
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
        return Arrays.asList(getCrossmarkSettings().conventions());
    }

    public AssessmentConvention getCrossMarkAssessmentConvention() {
        return getCrossmarkSettings().getAssessmentConvention();
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
