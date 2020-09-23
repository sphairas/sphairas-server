/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.config;

import java.io.IOException;
import java.text.ParseException;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import org.apache.naming.resources.Resource;
import org.thespheres.betula.TermId;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.NoProviderException;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.UserRepresentation;
import org.thespheres.betula.services.scheme.spi.Scheme;
import org.thespheres.betula.services.scheme.spi.SchemeProvider;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.services.scheme.spi.TermNotFoundException;
import org.thespheres.betula.services.scheme.spi.TermSchedule;
import org.thespheres.betula.util.IDUtilities;

/**
 *
 * @author boris.heithecker
 */
@ApplicationScoped
public class ConfigurationPropertiesImpl {

    public static final String PROP_CURRENT_TERM = "web.ui.current-term";
    static final Logger LOGGER = Logger.getLogger("org.thespheres.server.clients");
    @Inject
    private AppResourcesContext appResources;

    @Default
    @Produces
    public Term currentTerm(final LocalConfigProperties properties) {
        return findTerm(properties, false);
    }

    @Named("preceding")
    @Produces
    public Term precedingTerm(final LocalConfigProperties properties) {
        return findTerm(properties, true);
    }

    private Term findTerm(final LocalConfigProperties properties, boolean preceding) throws ConfigurationException {
        final String prop = properties.getProperty(PROP_CURRENT_TERM);
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
                throw new ConfigurationException("CurrentTerm", ex);
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
                    throw new ConfigurationException("CurrentTerm", ex);
                }
            }
        }
        throw new ConfigurationException("CurrentTerm", new String[]{PROP_CURRENT_TERM});
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
            throw new ConfigurationException("TermSchedule", npex);
        }
    }

    @Default
    @Produces
    public DocumentsModel createDocumentModel(final LocalConfigProperties properties) {
        final DocumentsModel dm = new DocumentsModel();
        try {
            dm.initialize(properties.getProperties());
        } catch (final IllegalStateException e) {
            //DocumentsModel not properly initialized.
            throw new ConfigurationException("DocumentsModel", e);
        }
        return dm;
    }

    @Delegate
    @Produces
    public NamingResolver findNamingResolver(final LocalConfigProperties properties) { //@New NamingResolverDelegate ubean, 
//        final String prov = provider(properties);
        final String nprov = properties.getProperty("naming.providerURL", CommonAppProperties.provider());
        if (nprov == null || nprov.trim().isEmpty()) {
            throw new ConfigurationException(properties.getName(), new String[]{"providerURL", "naming.providerURL"});
        }
        return NamingResolver.find(nprov);
    }

//    @Produces
//    public CommonDocuments commonDocuments() {
//        final ProxyDirContext dc = CommonAppProperties.lookupAppResourcesContext();
//        final String file = NdsReportBuilderFactory.SCHULVORLAGE_FILE;
//   
//        if (hasResource(dc, NdsReportBuilderFactory.SCHULVORLAGE_FILE)) {
//            final Resource res;
//            try {
//                res = (Resource) dc.lookup(file);
//            } catch (NamingException ex) {
//                Logger.getLogger(ConfigurationPropertiesImpl.class.getPackage().getName()).log(Level.WARNING, ex.getMessage(), ex);
//                throw new MissingConfigurationResourceException(file);
//            }
//            try (final InputStream is = res.streamContent()) {
//                return (NdsZeugnisSchulvorlage) vorlageJAXB.createUnmarshaller().unmarshal(is);
//            } catch (IOException | JAXBException ex) {
//                final MissingConfigurationResourceException th = new MissingConfigurationResourceException(file);
//                th.initCause(ex);
//                throw th;
//            }
//        } else {
//            return new NdsZeugnisSchulvorlage(CommonAppProperties.provider());
//        }
//    }
    @Named("instance")
    @Produces
//    @Dependent
    public Properties properties() {
        final Properties p = new Properties();
        if (hasResource(CommonAppProperties.INSTANCE_PROPERTIES_FILE)) {
            final Resource res;
            try {
                res = (Resource) appResources.lookup(CommonAppProperties.INSTANCE_PROPERTIES_FILE);
            } catch (NamingException ex) {
                logger().log(Level.WARNING, ex.getMessage(), ex);
                throw new ConfigurationException(CommonAppProperties.INSTANCE_PROPERTIES_FILE);
            }
            try {
                p.load(res.streamContent());
            } catch (IOException ex) {
                throw new ConfigurationException(CommonAppProperties.INSTANCE_PROPERTIES_FILE, ex);
            }
        }
        return p;
    }

    @Named("common-import")
    @Produces
    public Properties commonImportProperties() {
        final Properties p = new Properties();
        if (hasResource("common-import.properties")) {
            final Resource res;
            try {
                res = (Resource) appResources.lookup("common-import.properties");
            } catch (NamingException ex) {
                logger().log(Level.WARNING, ex.getMessage(), ex);
                throw new ConfigurationException("common-import.properties");
            }
            try {
                p.load(res.streamContent());
            } catch (IOException ex) {
                throw new ConfigurationException("common-import.properties", ex);
            }
        }
        return p;
    }

    public boolean hasResource(final String res) {
        try {
            final NamingEnumeration<NameClassPair> l = appResources.list("");
            while (l.hasMore()) {
                if (l.next().getName().equals(res)) {
                    return true;
                }
            }
        } catch (final NamingException ex) {
            LOGGER.log(Level.WARNING, "An exception occured listing resources in " + appResources.getContextName(), ex);
        }
        return false;
    }

    @Produces
    public Logger logger() {
        return LOGGER;
    }

}
