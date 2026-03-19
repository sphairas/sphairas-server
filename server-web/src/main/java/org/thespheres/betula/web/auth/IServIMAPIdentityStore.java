package org.thespheres.betula.web.auth;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.login.LoginException;
import org.apache.commons.net.imap.AuthenticatingIMAPClient;
import org.apache.commons.net.imap.IMAPSClient;
import org.thespheres.betula.security.iservlogin.IservLogin;

/**
 *
 * @author boris.heithecker@gmx.net
 */
@ApplicationScoped
public class IServIMAPIdentityStore implements IdentityStore {

//    final static String LOGIN_BEAN_NAME = "java:global/Betula_Server/Betula_Persistence/IservLoginImpl!org.thespheres.betula.security.iservlogin.IservLogin";
    static final String DEFAULT_PASSWORD = "changeit";
    static final String[] GROUPS = {"signees"};
    private boolean initialized = false;
    private String host;
    private int port;
    private boolean endpointChecking;
    private String signeeSuffix;
//    private String endpoints;
    private SSLContext ssl;
    @EJB
    IservLogin iservLogin;

    @PostConstruct
    public void init() {
        //create-auth-realm 
        //--classname org.thespheres.betula.security.iservlogin.IservRealm 
        //--property iserv.imap.host=${ENV=ISERV_IMAP_HOST}:iserv.imap.port=${ENV=ISERV_IMAP_PORT}:iserv.imap.signee-suffix=${ENV=LOGINDOMAIN} iserv
        final String h = System.getenv("ISERV_IMAP_HOST");
        final String pt = System.getenv("ISERV_IMAP_PORT");
        final String hv = System.getenv("ISERV_IMAP_CHECK_ENDPOINT");
        final String suffix = System.getenv("LOGINDOMAIN");
//        final String ep = props.getProperty("com.sun.appserv.iiop.endpoints");
        if (h == null || pt == null || h.isEmpty() || pt.isEmpty()) {
            Logger.getLogger(IServIMAPIdentityStore.class.getName()).info("IServ-Login nicht initializiert.");
            return;
        }
        int p = Integer.parseInt(pt);
        AuthenticatingIMAPClient cl = null;
        try {
            initSSLContext();
            cl = new AuthenticatingIMAPClient(IMAPSClient.DEFAULT_PROTOCOL, true, ssl);
            //Hostname verfications must be disabled because
            //server may be running on IServ as host machine
            final boolean checkEndpoint = Boolean.parseBoolean(hv);
            cl.setEndpointCheckingEnabled(checkEndpoint);
            cl.connect(h, p);
            host = h;
            port = p;
            signeeSuffix = (suffix == null || suffix.trim().isEmpty()) ? null : suffix.trim();
            endpointChecking = checkEndpoint;
//            if (ep != null && !ep.trim().isEmpty()) {
//                this.endpoints = ep.trim();
//            }
            this.initialized = true;
            Logger.getLogger(IServIMAPIdentityStore.class.getName()).info("IServ-Login nicht initializiert.");
        } catch (Exception ex) {
            Logger.getLogger(IServIMAPIdentityStore.class.getName()).log(Level.WARNING, ex.getLocalizedMessage(), ex);
            Logger.getLogger(IServIMAPIdentityStore.class.getName()).info("IServ-Login nicht initializiert.");
        } finally {
            try {
                if (cl != null) {
                    cl.disconnect();
                }
            } catch (IOException ex) {
                Logger.getLogger(IServIMAPIdentityStore.class.getName()).log(Level.WARNING, ex.getLocalizedMessage(), ex);
            }
        }
    }

    public String getIservImapHost() {
        return host;
    }

    public int getIservImapPort() {
        return port;
    }

    public boolean isEndpointChecking() {
        return endpointChecking;
    }

    public String getSigneeSuffix() {
        return signeeSuffix;
    }

    public CredentialValidationResult validate(final UsernamePasswordCredential credential) throws LoginException {
//        return new CredentialValidationResult("test", Set.of("signees"));
        if (!initialized) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }
        final String[] grpList = authorize(credential);
        if (grpList == null || grpList.length == 0) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }

        boolean success = false;
        AuthenticatingIMAPClient cl;
        try {
            cl = new AuthenticatingIMAPClient(IMAPSClient.DEFAULT_PROTOCOL, true, ssl);
            cl.setEndpointCheckingEnabled(isEndpointChecking());
            cl.connect(getIservImapHost(), getIservImapPort());
        } catch (IOException ex) {
            Logger.getLogger(IServIMAPIdentityStore.class.getName()).log(Level.WARNING, "Keine Verbindung zu IServ", ex);
            throw new LoginException();
        }
        try {
            success = cl.authenticate(AuthenticatingIMAPClient.AUTH_METHOD.PLAIN, credential.getCaller(), credential.getPasswordAsString());
            cl.logout();
        } catch (IOException ex) {
            Logger.getLogger(IServIMAPIdentityStore.class.getName()).log(Level.WARNING, "IServ-Login fehlgeschlagen.", ex);
            final LoginException lex = new LoginException();
            lex.initCause(ex);
            throw lex;
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException ex) {
            Logger.getLogger(IServIMAPIdentityStore.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            final LoginException lex = new LoginException();
            lex.initCause(ex);
            throw lex;
        } finally {
            try {
                cl.disconnect();
            } catch (IOException ex) {
            }
        }
        if (success) {
            return new CredentialValidationResult(credential.getCaller(), Set.of(GROUPS));
        }
        return CredentialValidationResult.INVALID_RESULT;
    }

    private String[] authorize(final UsernamePasswordCredential credential) throws LoginException {
        //            final Properties props = new Properties();
//            addProperties(props);
//            final IservLogin lb = (IservLogin) new InitialContext(props).lookup(LOGIN_BEAN_NAME);
        final String sfx = getSigneeSuffix();
        final String suffix = sfx != null ? sfx : getIservImapHost();
        return iservLogin.getGroups(credential.getCaller(), suffix);
    }

    @Override
    public int priority() {
        return 10;
    }

    private void initSSLContext() throws Exception {
        final SSLContext ctx = SSLContext.getInstance("TLSv1.3");
        final KeyManagerFactory kstorefac = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        final Path kspath = Paths.get(System.getProperty("javax.net.ssl.keyStore"));
        final KeyStore kstore = KeyStore.getInstance(System.getProperty("javax.net.ssl.keyStoreType", KeyStore.getDefaultType()));
        kstore.load(Files.newInputStream(kspath, StandardOpenOption.READ), DEFAULT_PASSWORD.toCharArray());
        kstorefac.init(kstore, DEFAULT_PASSWORD.toCharArray());
        final KeyManager[] kms = kstorefac.getKeyManagers();
        final TrustManagerFactory tstorefac = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        final Path tspath = Paths.get(System.getProperty("javax.net.ssl.trustStore"));
        final KeyStore tstore = KeyStore.getInstance(System.getProperty("javax.net.ssl.trustStoreType", KeyStore.getDefaultType()));
        tstore.load(Files.newInputStream(tspath, StandardOpenOption.READ), DEFAULT_PASSWORD.toCharArray());
        tstorefac.init(tstore);
        ctx.init(kms, tstorefac.getTrustManagers(), new SecureRandom());
        ssl = ctx;
    }
}
