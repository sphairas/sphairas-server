/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.config;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.Resource;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.AbstractGrade;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.AbstractMarker;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.niedersachsen.xml.NdsZeugnisSchulvorlage;
import org.thespheres.betula.niedersachsen.zeugnis.NdsReportBuilderFactory;
import org.thespheres.betula.server.beans.MissingConfigurationResourceException;
import org.thespheres.betula.server.beans.annot.Arbeitsgemeinschaft;
import org.thespheres.betula.server.beans.annot.Authority;
import org.thespheres.betula.server.beans.annot.Current;
import org.thespheres.betula.server.beans.annot.Preceding;
import org.thespheres.betula.server.beans.config.CommonAppProperties;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.NoProviderException;
import org.thespheres.betula.services.UserRepresentation;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.scheme.spi.Scheme;
import org.thespheres.betula.services.scheme.spi.SchemeProvider;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.services.scheme.spi.TermNotFoundException;
import org.thespheres.betula.services.scheme.spi.TermSchedule;
import org.thespheres.betula.services.ws.CommonDocuments;
import org.thespheres.betula.util.IDUtilities;

/**
 *
 * @author boris.heithecker
 */
@ApplicationScoped
public class ConfigurationPropertiesImpl {

    static final Logger LOGGER = Logger.getLogger(AppProperties.LOGGER);
    private JAXBContext vorlageJAXB;

    @PostConstruct
    public void initialize() {
        try {
            vorlageJAXB = JAXBContext.newInstance(NdsZeugnisSchulvorlage.class);
        } catch (JAXBException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Arbeitsgemeinschaft
    @Produces
    public Marker getAGMarker() {
        return new AbstractMarker("kgs.unterricht", "ag", null);
    }

    @Arbeitsgemeinschaft
    @Produces
    public Grade getAGPart() {
        return new AbstractGrade("niedersachsen.teilnahme", "tg");
    }

    @Current
    @Produces
    public Term currentTerm(final LocalConfigProperties properties) {
        return findTerm(properties, false);
    }

    @Preceding
    @Produces
    public Term precedingTerm(final LocalConfigProperties properties) {
        return findTerm(properties, true);
    }

    private Term findTerm(final LocalConfigProperties properties, boolean preceding) throws ConfiguredModelException {
        final String prop = properties.getProperty(AppProperties.PROP_CURRENT_TERM);
        final TermSchedule ts = termSchedule();
        if (prop != null) {
            try {
                try {
                    final Term parsed = ((UserRepresentation<Term>) ts).parse(prop);//z.B. 2019/1
                    if (!preceding) {
                        return parsed;
                    }
                    final TermId ptid = findPrecedingTermId(parsed.getScheduledItemId());
                    return parsed.getSchedule().resolve(ptid);
                } catch (ClassCastException ccex) {
                    TermId tid = IDUtilities.parseTermId(prop);
                    if (preceding) {
                        tid = findPrecedingTermId(tid);
                    }
                    return ts.resolve(tid);
                }
            } catch (MissingResourceException | IllegalArgumentException | ParseException | TermNotFoundException | IllegalAuthorityException ex) {
                throw new ConfiguredModelException("CurrentTerm", ex);
            }
        } else if (ts != null) {
            final Term t = ts.getCurrentTerm();
            if (!preceding) {
                return t;
            } else {
                try {
                    final TermId ptid = findPrecedingTermId(t.getScheduledItemId());
                    return ts.resolve(ptid);
                } catch (TermNotFoundException | IllegalAuthorityException ex) {
                    throw new ConfiguredModelException("CurrentTerm", ex);
                }
            }
        }
        throw new ConfiguredModelException(new String[]{AppProperties.PROP_CURRENT_TERM});
    }

    protected TermId findPrecedingTermId(final TermId c) {
        return new TermId(c.getAuthority(), c.getId() - 1);
    }

    @Default
    @Produces
    public TermSchedule termSchedule() {
        final SchemeProvider sp;
        try {
            sp = SchemeProvider.find("mk.niedersachsen.de");
            return sp.getScheme(Scheme.DEFAULT_SCHEME, TermSchedule.class);
        } catch (NoProviderException npex) {
            throw new ConfiguredModelException("TermSchedule", npex);
        }
    }

    @Default
    @Produces
    public DocumentsModel createDocumentModel(final LocalConfigProperties properties) {
        final DocumentsModel dm = new DocumentsModel();
        try {
            dm.initialize(properties.getProperties());
        } catch (IllegalStateException e) {
            //DocumentsModel not properly initialized.
            throw new ConfiguredModelException("DocumentsModel", e);
        }
        return dm;
    }

    @Delegate
    @Produces
    public NamingResolver findNamingResolver(final LocalConfigProperties properties) { //@New NamingResolverDelegate ubean, 
//        final String prov = provider(properties);
        final String nprov = properties.getProperty("naming.providerURL", CommonAppProperties.provider());
        if (nprov == null || nprov.trim().isEmpty()) {
            throw new ConfiguredModelException(new String[]{"providerURL", "naming.providerURL"});
        }
        return NamingResolver.find(nprov);
    }

    @Produces
    public CommonDocuments commonDocuments() {
        final ProxyDirContext dc = CommonAppProperties.lookupAppResourcesContext();
        final String file = NdsReportBuilderFactory.SCHULVORLAGE_FILE;
        final Resource res;
        try {
            res = (Resource) dc.lookup(file);
        } catch (NamingException ex) {
            Logger.getLogger(ConfigurationPropertiesImpl.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
            throw new MissingConfigurationResourceException(file);
        }
        try (final InputStream is = res.streamContent()) {
            return (NdsZeugnisSchulvorlage) vorlageJAXB.createUnmarshaller().unmarshal(is);
        } catch (IOException | JAXBException ex) {
            final MissingConfigurationResourceException th = new MissingConfigurationResourceException(file);
            th.initCause(ex);
            throw th;
        }
    }

    @Produces
    @Authority
    public String authority(final LocalConfigProperties properties) {
        return properties.getProperty("authority");
    }

    @Produces
//    @Dependent
    public Properties properties() {
        final Properties p = new Properties();
        final ProxyDirContext dc = CommonAppProperties.lookupAppResourcesContext();
        if (hasResource(dc, AppProperties.INSTANCE_PROPERTIES_FILE)) {
            final Resource res;
            try {
                res = (Resource) dc.lookup(AppProperties.INSTANCE_PROPERTIES_FILE);
            } catch (NamingException ex) {
                logger().log(Level.WARNING, ex.getMessage(), ex);
                throw new MissingConfigurationResourceException(AppProperties.INSTANCE_PROPERTIES_FILE);
            }
            try {
                p.load(res.streamContent());
            } catch (IOException ex) {
                final MissingConfigurationResourceException th = new MissingConfigurationResourceException(AppProperties.INSTANCE_PROPERTIES_FILE);
                th.initCause(ex);
                throw th;
            }
        }
        return p;
    }

    @Produces
    public Logger logger() {
        return LOGGER;
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
            LOGGER.log(Level.WARNING, "An exception occured listing resources in " + dc.getContextName(), ex);
        }
        return false;
    }
}
