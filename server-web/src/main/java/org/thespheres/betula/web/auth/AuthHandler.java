package org.thespheres.betula.web.auth;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanismHandler;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.authentication.mechanism.http.LoginToContinue;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.cert.X509Certificate;

/**
 *
 * @author boris.heithecker@gmx.net
 */
@CustomFormAuthenticationMechanismDefinition(
        loginToContinue = @LoginToContinue(loginPage = "/ui/login.xhtml", errorPage = "/error-pages/error.xhtml", useForwardToLogin = true),
        qualifiers = WebAuth.class
)
@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
public class AuthHandler implements HttpAuthenticationMechanismHandler {

    @Inject
    @WebAuth
    private HttpAuthenticationMechanism formAuth;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext context) throws AuthenticationException {
//        final String path = request.getRequestURI();
        final Object certs = request.getAttribute("jakarta.servlet.request.X509Certificate");
        if (certs != null && ((X509Certificate[]) certs).length > 0) {
            final X509Certificate cert = ((X509Certificate[]) certs)[0];

//            final String name = cert.getSubjectX500Principal().getName();
//            Set<String> groups = rolesForPrincipal(name);
//            return context.notifyContainerAboutLogin(name, groups);
        }
        return formAuth.validateRequest(request, response, context);
    }

}
