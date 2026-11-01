package org.thespheres.betula.web.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;

/**
 * OAuth2 / OpenID Connect login (authorization code flow). Configured through
 * environment variables from docker.env (read via EL {@code System.getenv}):
 * <ul>
 * <li>OAUTH_PROVIDER_URI (issuer, e.g. https://idp.example.org/realms/x)</li>
 * <li>OAUTH_CLIENT_ID</li>
 * <li>OAUTH_CLIENT_SECRET</li>
 * </ul>
 * The redirect URI is {@code <base URL>/oauth2/callback} and must be registered
 * at the IdP.
 * Entry point for users: {@code /oauth2/login}. The sub claim is mapped to a signee
 * by username in {@link OAuthUserMapper}; IdP groups are not used.
 */
@OpenIdAuthenticationMechanismDefinition(
        providerURI = "${System.getenv('OAUTH_PROVIDER_URI')}",
        clientId = "${System.getenv('OAUTH_CLIENT_ID')}",
        clientSecret = "${System.getenv('OAUTH_CLIENT_SECRET')}",
        redirectURI = "${baseURL}/oauth2/callback",
        scope = {"openid"},
        useSession = true,
        qualifiers = OAuth.class,
        claimsDefinition = @ClaimsDefinition(callerNameClaim = "sub")
)
@ApplicationScoped
public class OAuthMechanism {
}
