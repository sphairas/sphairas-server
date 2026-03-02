package org.thespheres.betula.server.service;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.interceptor.Interceptor;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanismHandler;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

/**
 *
 * @author boris
 */
@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
public class ServiceAuthHandler implements HttpAuthenticationMechanismHandler {

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext context) throws AuthenticationException {
//        final String path = request.getRequestURI();
        final Object certs = request.getAttribute("jakarta.servlet.request.X509Certificate");
        if (certs != null && ((X509Certificate[]) certs).length > 0) {
//            request.getAttribute("jakarta.servlet.request.X509Certificate") 
//            is only populated if the Payara SSL listener (Port 8181) successfully completed a TLS handshake, 
//            the certificate has already been cryptographically verified against your TrustStore.
            final X509Certificate cert = ((X509Certificate[]) certs)[0];
            final String name = cert.getSubjectX500Principal().getName();
            Set<String> groups = rolesForPrincipal(name);
            return context.notifyContainerAboutLogin(name, groups);
        }
        return context.doNothing();
    }

    public static Set<String> rolesForPrincipal(final String name) {
        final Set<String> groups = new HashSet<>();
        groups.add("unitadmins");
        if (extractCN(name).equals(System.getenv("SPHAIRAS_HOSTNAME"))) {
            groups.add("internals");
        }
        return groups;
    }

    private static String extractCN(String dn) {
        try {
            return new LdapName(dn).getRdns().stream()
                    .filter(rdn -> rdn.getType().equalsIgnoreCase("CN"))
                    .findAny()
                    .map(rdn -> rdn.getValue().toString())
                    .orElse("");
        } catch (InvalidNameException ex) {
            Logger.getLogger(ServiceAuthHandler.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        }
        return "";
    }
}
