package org.thespheres.betula.web.auth;

/**
 * OAuth2 login is available only if OAUTH_PROVIDER_URI is set.
 */
public final class OAuthConfig {

    private OAuthConfig() {
    }

    public static boolean isEnabled() {
        final String uri = System.getenv("OAUTH_PROVIDER_URI");
        return uri != null && !uri.isBlank();
    }
}
