package org.thespheres.betula.calendar.rest;

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
import java.util.Set;

/**
 *
 * @author boris.heithecker
 */
@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
public class CalAuthHandler implements HttpAuthenticationMechanismHandler {

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
            return context.notifyContainerAboutLogin(name, Set.of("unitadmins"));
        }
        return context.doNothing();
    }

}
