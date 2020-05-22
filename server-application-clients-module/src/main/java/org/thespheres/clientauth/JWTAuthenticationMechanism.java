/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.clientauth;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author boris
 */
@Dependent
public class JWTAuthenticationMechanism implements HttpAuthenticationMechanism {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER = "Bearer ";

    @Inject
    private JWTAuthentication tokenProvider;

    @Override
    public AuthenticationStatus validateRequest(final HttpServletRequest request, final HttpServletResponse response, final HttpMessageContext context) throws AuthenticationException {
//        LOGGER.log(Level.INFO, "validateRequest: {0}", request.getRequestURI());
        // Get the (caller) name and password from the request
        // NOTE: This is for the smallest possible example only. In practice
        // putting the password in a request query parameter is highly insecure
//        String name = request.getParameter("name");
//        String password = request.getParameter("password");
        final String token = extractToken(context);

//        if (name != null && password != null) {
////            LOGGER.log(Level.INFO, "credentials : {0}, {1}", new String[]{name, password});
////            // validation of the credential using the identity store
////            CredentialValidationResult result = identityStoreHandler.validate(new UsernamePasswordCredential(name, password));
////            if (result.getStatus() == CredentialValidationResult.Status.VALID) {
////                // Communicate the details of the authenticated user to the container and return SUCCESS.
////                return createToken(result, context);
////            }
////            // if the authentication failed, we return the unauthorized status in the http response
////            return context.responseUnauthorized();
//        } else 
        if (token != null) {
            // validation of the jwt credential
            return validateToken(token, context);
        } else if (context.isProtected()) {
            // A protected resource is a resource for which a constraint has been defined.
            // if there are no credentials and the resource is protected, we response with unauthorized status
            return context.responseUnauthorized();
        }
        //no credentials AND the resource is not protected, 
        return context.doNothing();
    }

    private String extractToken(final HttpMessageContext context) {
        final String authorizationHeader = context.getRequest().getHeader(AUTHORIZATION_HEADER);
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER)) {
            return authorizationHeader.substring(BEARER.length()).trim();
        }
        return null;
    }

    private AuthenticationStatus validateToken(final String token, final HttpMessageContext context) {
        final JWTCredential credsd = tokenProvider.validateToken(token);
        if (credsd != null) {
            return context.notifyContainerAboutLogin(credsd.getPrincipal(), credsd.getGroups());
        }
//                    LOGGER.log(Level.INFO, "Security exception for user {0} - {1}", new String[]{eje.getClaims().getSubject(), eje.getMessage()});
        return context.responseUnauthorized();
    }
}
