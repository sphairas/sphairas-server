package org.thespheres.betula.server.beans.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * The JVM truststore contains the default CAs (needed e.g. for the OAuth2 IdP),
 * so a successful TLS client handshake does not mean the certificate may log
 * in. Only certificates of this server (the server certificate itself or
 * certificates signed by it) are accepted.
 */
public final class ServerCertificates {

    private static volatile X509Certificate server;

    private ServerCertificates() {
    }

    public static boolean isIssuedByThisServer(final X509Certificate cert) {
        try {
            final X509Certificate s = loadServerCertificate();
            if (s == null) {
                return false;
            }
            if (cert.equals(s)) {
                return true;
            }
            cert.verify(s.getPublicKey());
            return true;
        } catch (IOException | GeneralSecurityException ex) {
            return false;
        }
    }

    private static X509Certificate loadServerCertificate() throws IOException, GeneralSecurityException {
        X509Certificate s = server;
        if (s == null) {
            final String dir = System.getenv("SECRETS");
            if (dir == null) {
                return null;
            }
            try (InputStream is = Files.newInputStream(Path.of(dir, "server.crt"))) {
                s = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
            }
            server = s;
        }
        return s;
    }
}
