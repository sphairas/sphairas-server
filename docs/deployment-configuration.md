# Deployment configuration

This guide covers the configuration needed for the web application to call the
service API, the calendar route changes, and the web report-generation resources.
Deploy `server-web` and `server-service` as separate WARs on Payara Full, at
`/web` and `/service` respectively. The Docker image no longer deploys the
legacy `server-admin-authentication` WAR or an EAR; the legacy module's source
is retained.

Each application WAR packages its own `sphairas-server-libs` and legacy
`javax.xml.bind` dependencies. Keep the `javax` JAXB API: external Container
types and existing JAXB annotations use the legacy namespace. The Docker
image also provisions server-wide libraries, including the MySQL driver, as
configured in the Dockerfile.

## Authentication

Each WAR has its own authentication configuration. The web WAR's
`AuthHandler` declares a custom form authentication mechanism and handles web
authentication; the service WAR's `ServiceAuthHandler` implements a
certificate-based `HttpAuthenticationMechanism`. A mechanism handler alone
does not register an authentication mechanism.

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

## Calendar routes

The calendar module is now packaged inside the service WAR. Update external
clients and reverse-proxy routes before deploying this version; the separate
`/calendar` context is no longer deployed.

Updated desktop clients detect server version 2.1 and use the new URLs by
default. Existing explicit URL settings take precedence and must be updated
or removed if they still refer to the old `/admins` or calendar routes.
Configured `calendarBaseURL`
values must have a trailing slash (e.g.
`https://host:8181/service/api/calendar/`), because the client appends
`reports/` directly.

The calendar REST resources now use the service API base URL with a
`calendar/` prefix:

| Old URL path | New URL path |
| --- | --- |
| `/calendar/resource/students` | `/service/api/calendar/students` |
| `/calendar/resource/tickets` | `/service/api/calendar/tickets` |
| `/calendar/resource/reports` | `/service/api/calendar/reports` |

The corresponding servlet endpoints keep their suffixes under the service
context:

| Old URL path | New URL path |
| --- | --- |
| `/calendar/students/...` | `/service/calendar/students/...` |
| `/calendar/tickets/...` | `/service/calendar/tickets/...` |
| `/calendar/reports/...` | `/service/calendar/reports/...` |

For example, the new REST URL for report dates is
`https://server.example.org:8181/service/api/calendar/reports`. If a reverse
proxy publishes the application below an additional prefix such as `/admins`,
include that prefix in the externally configured URL.

For desktop client overrides, `calendarBaseURL` points to the new calendar
REST base, ending in `/service/api/calendar/`, and `studentsUrl` points to
`/service/api/calendar/students`. Existing values pointing to the old
`/calendar` context must be updated; otherwise those clients will continue to
call the removed context.

The internal web-to-service calendar calls continue to use
`BETULA_SERVICE_API_URL`; no separate calendar service URL is needed.

## Database schema

The persistence descriptors retain their existing automatic schema-generation
settings. `betula0` creates tables and writes the generated DDL to
`<domain-dir>/config/betuladb-script`; `studentsPU` uses the Jakarta `create`
action, and `calendarsPU` uses `create` plus EclipseLink
`create-or-extend-tables`. Packaging these persistence units in the service WAR
does not intentionally change that behavior. Review Payara startup logs and
the generated DDL script when deploying, especially when using an existing
database.

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
