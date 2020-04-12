/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.config;

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
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.InvalidNameException;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import org.openide.util.Lookup;
import org.thespheres.betula.document.model.Subject;
import org.thespheres.betula.niedersachsen.NdsCommonConstants;
import org.thespheres.betula.niedersachsen.gs.CrossmarkSettings;
import org.thespheres.betula.niedersachsen.zeugnis.NdsReportBuilderFactory;
import org.thespheres.betula.niedersachsen.xml.NdsZeugnisSchulvorlage;
import org.thespheres.betula.niedersachsen.zeugnis.TermReportNoteSetTemplate;
import org.thespheres.betula.server.beans.MissingConfigurationResourceException;
import org.thespheres.betula.server.beans.config.CommonAppProperties;
import org.thespheres.betula.services.web.WebUIConfiguration;
import org.thespheres.betula.services.web.XmlWebUIConfiguration;
import org.thespheres.betula.util.CollectionUtil;

/**
 *
 * @author boris.heithecker
 */
@ApplicationScoped
public class ConfigImpl implements Serializable {

    private static final String SSL_SERVERCRT_PATH = "/ssl/server.crt";
    private JAXBContext notesTemplateJAXB;
    private JAXBContext vorlageJAXB;
    private JAXBContext webUIJAXB;
    private JAXBContext crossmarkSettingsJAXB;
//    private Date noteSetFileLastModified;
    private TermReportNoteSetTemplate noteSetTemplate;

    @PostConstruct
    public void initialize() {
        try {
            notesTemplateJAXB = JAXBContext.newInstance(TermReportNoteSetTemplate.class);
        } catch (JAXBException ex) {
            throw new IllegalStateException(ex);
        }
        try {
            vorlageJAXB = JAXBContext.newInstance(NdsZeugnisSchulvorlage.class);
        } catch (JAXBException ex) {
            throw new IllegalStateException(ex);
        }
        try {
            webUIJAXB = JAXBContext.newInstance(XmlWebUIConfiguration.class);
        } catch (JAXBException ex) {
            throw new IllegalStateException(ex);
        }
        try {
            crossmarkSettingsJAXB = JAXBContext.newInstance(CrossmarkSettings.class);
        } catch (JAXBException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Produces
    public WebUIConfiguration findWebUIConfiguration() {
        final String bp = null; //getProvider();
        if (bp != null) {
            final WebUIConfiguration swc = Lookup.getDefault().lookupAll(WebUIConfiguration.class).stream()
                    .filter(wc -> wc.getName() != null)
                    .filter(wc -> wc.getName().equals(bp))
                    .collect(CollectionUtil.singleOrNull());
            if (swc != null) {
                return swc;
            }
        }
//        throw new ConfigurationException(WebUIConfiguration.class.getName(), WebAppProperties.BETULA_WEB_UI_SERVICE_PROVIDER_PROPERTY);
        final ProxyDirContext dc = lookupAppResourcesContext();
        final String file = "web-ui-configuration.xml";
        final Resource res;
        try {
            res = (Resource) dc.lookup(file);
        } catch (NamingException ex) {
            Logger.getLogger(ConfigImpl.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
            throw new MissingConfigurationResourceException(file);
        }
        try (final InputStream is = res.streamContent()) {
            return (XmlWebUIConfiguration) webUIJAXB.createUnmarshaller().unmarshal(is);
        } catch (IOException | JAXBException ex) {
            final MissingConfigurationResourceException th = new MissingConfigurationResourceException(file);
            th.initCause(ex);
            throw th;
        }
    }

    X509Certificate findServerCertificate() {
        final Path file = Paths.get(SSL_SERVERCRT_PATH);
        final Certificate[] certs;
        try (final InputStream is = Files.newInputStream(file)) {
            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
            certs = cf.generateCertificates(is).stream()
                    .toArray(Certificate[]::new);
        } catch (IOException | CertificateException ex) {
            Logger.getLogger(ConfigImpl.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
            final ConfigurationException th = new ConfigurationException(SSL_SERVERCRT_PATH, "certificate");
            th.initCause(ex);
            throw th;
        }
        if (certs.length > 0) {
            final X509Certificate cert = (X509Certificate) certs[0];
            return cert;
        }
        throw new MissingConfigurationResourceException(SSL_SERVERCRT_PATH);
    }

    @Produces
    public Comparator<Subject> findSubjectComparator(NdsReportBuilderFactory fac) {
        return (s1, s2) -> fac.forCareer(null).compare(s1.getSubjectMarker(), s2.getSubjectMarker());
    }

    @Typed(NdsReportBuilderFactory.class)
    @Produces
    public NdsReportBuilderFactory findZeugnisConfiguratorService() {
        final ProxyDirContext dc = lookupAppResourcesContext();
        final String file = NdsReportBuilderFactory.SCHULVORLAGE_FILE;
        final NdsZeugnisSchulvorlage vorlage;
        if (hasResource(dc, NdsReportBuilderFactory.SCHULVORLAGE_FILE)) {
            final Resource res;
            try {
                res = (Resource) dc.lookup(file);
            } catch (NamingException ex) {
                Logger.getLogger(ConfigImpl.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
                throw new MissingConfigurationResourceException(file);
            }
            try (final InputStream is = res.streamContent()) {
                vorlage = (NdsZeugnisSchulvorlage) vorlageJAXB.createUnmarshaller().unmarshal(is);
            } catch (IOException | JAXBException ex) {
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
            final ConfigurationException th = new ConfigurationException(SSL_SERVERCRT_PATH, "subject");
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
//    @javax.faces.view.ViewScoped
//    @RequestScoped
    @Dependent  //soll sessionscoped, aber funktioniert nicht;TermReportNoteSetTemplate muss serializable sein
    @Produces
    //Do not cache!!!
    public TermReportNoteSetTemplate findTermReportNoteSetTemplate() throws NamingException {
        final ProxyDirContext dc = lookupAppResourcesContext();
        final String file = NdsReportBuilderFactory.SIGNEE_BEMERKUNGEN_FILE;
        try {
            dc.lookup(file);
        } catch (NamingException nex) {
            final String msg = "No file " + file + " found!";
            Logger.getLogger(ConfigImpl.class.getPackage().getName()).log(Level.WARNING, msg);
            return new TermReportNoteSetTemplate("null");
        }
        Date lm = null;
        try {
            final ResourceAttributes attr = (ResourceAttributes) dc.getAttributes(file);
            if (attr != null) {
                lm = attr.getCreationOrLastModifiedDate();
            }
        } catch (NamingException | ClassCastException ex) {
            Logger.getLogger(ConfigImpl.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
            throw ex;
        }
//        if (noteSetFileLastModified != null && noteSetTemplate != null && lm != null && !lm.after(noteSetFileLastModified)) {
//            return noteSetTemplate;
//        }
        final Resource res;
        try {
            res = (Resource) dc.lookup(file);
        } catch (NamingException ex) {
            Logger.getLogger(ConfigImpl.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
            throw new MissingConfigurationResourceException(file);
        }
        try (final InputStream is = res.streamContent()) {
            noteSetTemplate = (TermReportNoteSetTemplate) notesTemplateJAXB.createUnmarshaller().unmarshal(is);
        } catch (IOException | JAXBException ex) {
            final MissingConfigurationResourceException th = new MissingConfigurationResourceException(file);
            th.initCause(ex);
            throw th;
        }
//        noteSetFileLastModified = lm;
        return noteSetTemplate;
    }

    @Dependent
    @Produces
    public CrossmarkSettings createCrossmarkSettings() {
        final ProxyDirContext dc = lookupAppResourcesContext();
        final String file = NdsCommonConstants.ANKREUZZEUGNISSE_FILE;
        final Resource res;
        try {
            res = (Resource) dc.lookup(file);
        } catch (NamingException ex) {
            return new CrossmarkSettings();
        }
        try (final InputStream is = res.streamContent()) {
            return (CrossmarkSettings) crossmarkSettingsJAXB.createUnmarshaller().unmarshal(is);
        } catch (IOException | JAXBException ex) {
            final MissingConfigurationResourceException th = new MissingConfigurationResourceException(file);
            th.initCause(ex);
            throw th;
        }
    }

    private ProxyDirContext lookupAppResourcesContext() {
        try {
            final Context c = new InitialContext();
            return (ProxyDirContext) c.lookup("java:global/Betula_Server/Betula_Persistence/AppResourcesContext");
        } catch (NamingException ne) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
            throw new RuntimeException(ne);
        }
    }

    public static boolean hasResource(final ProxyDirContext dc, final String res) {
        try {
            final NamingEnumeration<NameClassPair> l = dc.list("");
            while (l.hasMore()) {
                if (l.next().getName().equals(res)) {
                    return true;
                }
            }
        } catch (NamingException ex) {
            Logger.getLogger(ConfigImpl.class.getPackage().getName()).log(Level.WARNING, "An exception occured listing resources in " + dc.getContextName(), ex);
        }
        return false;
    }
}
