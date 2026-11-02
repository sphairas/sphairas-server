# Deployment configuration

This guide covers the configuration needed for the web application to call the
service API and to load its report-generation resources.

## Service API URL

Set `BETULA_SERVICE_API_URL` in the environment of the Payara process, or set
the Java system property `betula.service.api-url`. The system property takes
precedence when both are set. The value must be the HTTPS base URL for the
service API, including its context path, for example:

```text
BETULA_SERVICE_API_URL=https://server.example.org:8181/service/api
```

In the Docker deployment, add the setting to `docker.env` (copied from
`example.docker.env`). Replace the example hostname with the DNS name that the
web server can resolve and use to reach the service. Do not leave a placeholder
or use `localhost` unless the service is actually reachable there.

The hostname in the URL must match a DNS name in the service certificate's
subject alternative names. Certificate hostname verification is enabled; the
application does not provide a bypass option. The Payara JVM must also have
the service certificate trusted and a client certificate available for
mutual TLS. The Docker startup scripts configure the JVM key store,
trust store, and outbound certificate alias from the mounted secrets. Keep
those settings and the service's `internal` role mapping in place.

An invalid or missing URL causes client initialization to fail rather than
silently using a default endpoint.

## Web app-resources directory

The web application reads FOP configuration, XSL-FO files, fonts, and logos
from its local `app-resources` directory. Provision those files where the web
deployment can read them.

By default, the directory is `<Payara instanceRoot>/app-resources`. This is
the existing Docker layout: the Payara domain's `app-resources` path points to
the mounted `/app-resources` volume.

To use a different directory, set the Java system property
`sphairas.web.app-resources` to its absolute path on the web server, for
example:

```text
-Dsphairas.web.app-resources=/srv/sphairas/web-app-resources
```

On Payara, this can be configured as a server system property, for example:

```text
create-system-properties sphairas.web.app-resources=/srv/sphairas/web-app-resources
```

Restart or redeploy the web application after changing the property. The
directory must contain the files referenced by `schulvorlage.xml`, including
the configured XSL-FO file and any resources that FOP resolves relative to its
base URL.

## Existing TLS material

These settings are not new, but are required by the service URL configuration:

- `SECRETS/server.crt` and `SECRETS/server.key` provide the server certificate
  and private key used by the Docker startup scripts.
- The scripts generate Payara's outbound key store and trust store under
  `SECRETS_DIR`, then set the JVM properties `javax.net.ssl.keyStore`,
  `javax.net.ssl.trustStore`, and `com.sun.enterprise.security.httpsOutboundKeyAlias`.
- The service maps the configured server certificate to the `internal` role
  for internal REST endpoints.

Protect private keys and passwords as secrets. Do not put private-key
contents, passwords, or other credentials in `docker.env` or this document.
