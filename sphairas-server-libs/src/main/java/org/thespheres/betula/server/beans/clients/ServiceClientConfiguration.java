package org.thespheres.betula.server.beans.clients;

import java.net.URI;
import java.util.Locale;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

public final class ServiceClientConfiguration {

    public static final String SERVICE_API_URL_PROPERTY = "betula.service.api-url";
    public static final String SERVICE_API_URL_ENV = "BETULA_SERVICE_API_URL";

    private ServiceClientConfiguration() {
    }

    public static URI serviceApiUri() {
        final String configured = System.getProperty(SERVICE_API_URL_PROPERTY,
                System.getenv(SERVICE_API_URL_ENV));
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Set " + SERVICE_API_URL_PROPERTY
                    + " or " + SERVICE_API_URL_ENV + " to the service API HTTPS URL");
        }
        final URI uri;
        try {
            uri = URI.create(configured);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid service API URL in "
                    + SERVICE_API_URL_PROPERTY, ex);
        }
        if (!"https".equals(uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalStateException("Service API URL must be an absolute HTTPS URL without "
                    + "user info, query, or fragment");
        }
        return uri;
    }

    public static RestClientBuilder newRestClientBuilder() {
        return RestClientBuilder.newBuilder().baseUri(serviceApiUri());
    }
}
