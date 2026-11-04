# REST contracts and caller identity (approved)

Task `rest-contract-security`. Based on static analysis of the code (see [inventory.md](./inventory.md)). Items marked **(verify)** have not been confirmed at runtime and are checked in `behavior-baseline` / `standalone-war-deployment`.

## 1. Current identity behavior (what must be preserved or consciously changed)

1. **Web user identity.** Web authenticates users via Jakarta Security (form login against IServ IMAP/LDAP, roles `signee`, `unitadmin`; client-cert login via the legacy `server-admin-authentication`).
2. **EJB calls carry that identity.** Today, EJBs invoked from web run in the same JVM, so the caller principal and its roles are the end user's. The backend uses this in two ways:
   - Container roles: `@RolesAllowed` on `BetulaService` (`signee`), the target-document facades (`signee`/`unitadmin`), `BetulaAdminService` (`unitadmin`); programmatic `isCallerInRole("unitadmin")` in `ServiceUtils` and `DecoratedUnitDocumentFacade`.
   - Principal-derived signee: `SigneeLocal.getSigneePrincipal(...)` → filters data by the current signee (`DecoratedUnitDocumentFacade`, `BaseDocumentFacade`).
3. **`ReportsBeanImpl`, `StudentVCardsImpl`, `CalendarsBeanImpl`, `StudentsListsLocalBeanImpl`, `TargetDocumentsLocalBeanImpl`** declare no `@RolesAllowed` (ReportsBeanImpl grep-confirmed). Authorization for these is effectively "any role allowed in web.xml" (`signee`, `unitadmin`), enforced only by the web WAR. **After the split, the service WAR must enforce this itself.**
4. **Existing REST calls from web carry no user identity.** `ServiceInternalClient` is built in `AppConfiguration` without credentials. It relies on the JVM TLS client certificate **(system properties `javax.net.ssl.keyStore` and `javax.net.ssl.trustStore`)**, which maps to role `internal` (group `internals`) for `/api/internal/*`. The acting signee is passed as an explicit `signee` query parameter. So the existing REST pattern is "trusted service + asserted subject".
5. **Service web.xml:** `/*` requires `unitadmin`; `/api/internal/*` requires `internal`; both CONFIDENTIAL; `CLIENT-CERT` auth. `FastTargetDocumentsRestService` lives outside `/api/internal/*`, so it requires `unitadmin`. **(verify)** whether the web's cert principal is in `unitadmins`; the web-side `FastTargetDocumentsRestClient` has since been removed.
6. **Observed defects to fix on the way (tracked in tasks 5/6):**
   - Service API URL is runtime-configurable as system property `betula.service.api-url` or environment variable `BETULA_SERVICE_API_URL`; the URL must be HTTPS and its hostname must match the service certificate.
   - State-changing operations are exposed as `GET` (`submit-grade-value`, `submit-text-value`).
   - `FastTargetDocumentsRestService` swallows exceptions into HTTP 500 with `e.getMessage()` as the body.
   - Both `@ApplicationPath("api")` and `@Path("/api/fast-target-documents")` exist, which may yield `/service/api/api/fast-target-documents` **(verify)**.

## 2. Identity model (approved, see section 5)

**Recommendation: trusted web tier with asserted subject** (extends the existing pattern; no new infrastructure).

- Web → service uses mutual TLS. The web WAR's client cert maps to role `internal`. All new endpoints live under `/api/internal/*` and require `internal` only. They are not reachable by end users or external clients.
- **Acting signee and roles are sent only where the business logic needs them** (approved). Endpoints that do not depend on the user (e.g. the `ReportsBean` operations, which have no role checks today) carry no identity. Endpoints whose backend logic filters by signee or checks `unitadmin` (e.g. paths through `DecoratedUnitDocumentFacade`, `BaseDocumentFacade`, `ServiceUtils`) take the acting signee and roles explicitly (header `X-Betula-Signee`, `X-Betula-Roles`, or a query param like the existing endpoints). The service validates them (known signee, roles ⊆ {`signee`,`unitadmin`}) and applies the checks the EJBs did before. Since only `internal` callers are accepted, the values are trusted. Each endpoint's identity need is decided in `service-rest-implementation` by checking whether its call path reaches `getSigneePrincipal`/`isCallerInRole`.
- `SigneeLocal.getSigneePrincipal` and `isCallerInRole` inside the service must read the asserted identity when the call came through `/api/internal/*`. `ActingSubject` now carries that request-scoped identity and role set; persistence facades use it for internal requests and fall back to the container principal for other callers.
- Consequence: the `internal` role becomes powerful (it can act as any signee). It needs a restricted keystore, a pinned CA/trust, and removal of the accept-all hostname verifier.

Alternatives (not recommended now):

| Option | Why not now |
|---|---|
| Forward user credentials (Basic) | Web users authenticate via IServ/LDAP/cert; the web does not hold passwords after login, so this cannot work uniformly. |
| Signed token (MP-JWT) issued by web | Cleanest long-term and a fit for the MicroProfile vision, but needs key management, a verifier in service and a token format, which is too much for this split. It can replace the headers later without changing endpoint paths. |
| Run web calls as `unitadmin` | Removes per-user filtering and widens privileges. |

## 3. Endpoint contracts

Legend: **E** = exists, **N** = new (to be added to `ServiceInternalClient` or a new client interface in `sphairas-server-libs`), **V** = exists but needs verification or changes. Base: `/service/api/internal` (configurable). Scalars and ids use the existing `InternalParamConverterProvider` (text/plain); composite results use JSON; state changes use `POST`/`PUT`/`DELETE`.

### 3.1 Reports (`ReportsBean`, 15 methods, new resource `internal/reports`)

| Method (current) | Proposed endpoint | Notes |
|---|---|---|
| `findTermReports(student, term, create)` | `GET reports?student&term` (create=false), `POST reports?student&term` (create=true) → `DocumentId[]` | split because create writes |
| `getStudent(zgn)`, `getTerm(zgn)` | `GET reports/{zgn}/student`, `/term` | text |
| `getKopfnote` / `setKopfnote(zgn, convention[, grade])` | `GET`/`PUT reports/{zgn}/kopfnote/{convention}` | `Grade` as text; PUT returns boolean; `TermReportDataException` → 409 |
| `getIntegerValue` / `setIntegerValue(zgn, type[, value])` | `GET`/`PUT reports/{zgn}/integers/{type}` | same |
| `getMarkers`, `addMarker`, `removeMarker` | `GET reports/{zgn}/markers` (JSON), `POST reports/{zgn}/markers`, `DELETE reports/{zgn}/markers` | `Marker` as JSON body |
| `getNote` / `setNote(zgn, key[, value])` | `GET`/`PUT reports/{zgn}/notes/{key}` | text; the most-called method (11 sites) → consider batch `GET reports/{zgn}/notes` |
| `getCustomNotes` / `setCustomNotes` | `GET`/`PUT reports/{zgn}/custom-notes` (JSON) | |
| `getAGs(student, term)` | `GET reports/ags?student&term` | |

### 3.2 Other bean replacements

| Current | Replacement | Status |
|---|---|---|
| `StudentsLocalBean.get` | `GET internal/student-vcard` | E |
| `CalendarsBean.getDate` | `GET internal/report-date` | E (V: argument parity) |
| `StudentsListsLocalBean.findPrimaryUnit`, `getMarkerEntry` | `GET internal/student-primary-unit`, `/student-marker-entry` | E |
| `TargetDocumentsLocalBean.getTargetAssessmentDocuments`, `getDocumentMarkers` | `GET/POST internal/target-documents/*` via `ServiceInternalClient` | E (implemented; identity headers required) |
| `TargetDocumentsLocalBean.getSubjectAlternativeName` | `GET internal/subject-alternative-name` via `ServiceInternalClient` | E |
| `FastTargetDocuments2` (web-used operations) | `GET/POST internal/target-documents/*` via `ServiceInternalClient` | E (implemented; identity headers required) |
| `BetulaWebService` (`ClientBean`) | none | Removed as unused; no REST replacement |
| JNDI `ReportsBeanImpl` in `PrimaryUnit` | `internal/reports` via `ServiceInternalClient` | E |
| JNDI `AppResourcesContext` | `GET internal/app-resource?path=...` via `ServiceInternalClient` | E |
| JNDI `IservLogin` | none | Dead lookup removed; no REST replacement |

### 3.3 Cross-cutting contract rules
- **Serialization:** DTOs shared via `sphairas-server-libs` (`FastTermTargetDocument`, `FastTextTermTargetDocument`, `Marker`, `Grade`, ids); JSON for composites, text for scalars. No Java serialization.
- **Errors:** one `BadRequest` JSON body (existing `BadRequestResponseMapper`) with HTTP status: 400 validation, 404 missing entity (`NoEntityFoundException`), 409 `TermReportDataException`/`AmbiguousResultException`/`AmbiguousDateException`, 403 identity or role failure, 500 never carries an exception message. Web maps these back to the exceptions it already handles.
- **Idempotency/safety:** reads `GET`; writes `PUT`/`POST`/`DELETE` (the two existing `GET submit-*` endpoints become `POST`; no `GET` alias, since the endpoints are internal only).
- **Transactions:** each endpoint is one transaction in the service; web must not assume multi-call atomicity (relevant for sequences like `findTermReports(create)` followed by `setNote`).
- **Performance:** batch reads (`notes`, markers per student list) for the loops in `NdsFormatter`, `ZeugnisArguments`, `NdsFormatReportsBean`.
- **TLS/config:** base URI via system property `betula.service.api-url` (preferred) or environment variable `BETULA_SERVICE_API_URL`; an absolute HTTPS URL is required. Payara configures the JVM client key/trust stores (`javax.net.ssl.keyStore`, `javax.net.ssl.trustStore`) and outbound key alias. No custom hostname verifier is installed, so normal certificate hostname verification remains enabled.
- **No JVM sharing:** no endpoint may take or return container-specific types. Final verification of remaining web-to-service calls is tracked by `verify-web-boundary`.

## 4. Remaining tasks
- `verify-web-boundary`: find and resolve any remaining active in-process service calls.
- `convert-persistence-calendar-jars` and `integrate-calendar-service-deployment`: package persistence/calendar for the service WAR and publish calendar endpoints there.
- `make-war-dependencies-self-contained` and `standalone-war-deployment`: package and verify each WAR without the EAR or the other WAR.

## 5. Decisions (approved by project owner)
1. Identity: mutual TLS, web authenticates as `internal`; acting signee/roles are sent only if required by the business logic.
2. The `internal` role may act as any signee. Mitigation: dedicated certificate and network-level restriction.
3. No `GET submit-*` aliases; the endpoints are internal only, so they can change to `POST` directly.

## Implementation notes

- `internal/reports` is implemented as `ReportsResource`. Deviation from section 3.1: the report document id is the query parameter `document` (ids contain slashes); `convention`, `type`, `key` are query parameters too, e.g. `PUT internal/reports/kopfnote?document=..&convention=..` (text body). Markers are text (`Marker.toString()`), `DELETE markers?document&marker`.
- `submit-grade-value` and `submit-text-value` are now `POST` (same query parameters); missing parameters give 400.
- `select` returns 204 instead of 500 when no grade is set.
- `getPrimaryUnit` and `getPrimaryUnitsSignees` no longer create the signee-unit-map document on read (it is still created by `setPrimaryUnit`); an unknown document yields `UnitId.NULL` / empty map.
- `Grade` and `Marker` JSON-B adapters (`JsonbContextResolver`) are shared by service and web client.
- New in `InternalAPI` (text/plain, 204 if no value, 400 for missing parameters): `GET internal/student-primary-unit?student[&asOf]`, `GET internal/student-marker-entry?student&document[&asOf]`, `GET internal/subject-alternative-name?document`. The service uses `StudentsListsLocalBeanImpl` and `CommonDocuments` + `GradeTargetDocumentFacade` directly, because `TargetDocumentsLocalBeanImpl` is `@RolesAllowed({signee,unitadmin})` and would reject the `internal` role. `getMarkerEntry` no longer creates the student-marker document on read. `getDocumentMarkers` already exists as `api/fast-target-documents/document-markers`. `getStringEntry` is unused in web and not exposed. `InternalServices` in web now calls the new endpoints.
- `AmbiguousDateExceptionMapper` returns 409 (text/plain message), as in the contract. The web REST client maps 409 on `internal/report-date` back to `AmbiguousDateException`; 400 remains mapped to `BadRequest`.
- Acting-subject handling is implemented for the student/document migration. `InternalAPI` requires `X-Betula-Roles` for identity-dependent target-document endpoints, accepts only the `signee` and `unitadmin` roles, and sets the request-scoped `ActingSubject`. A signee is required when the `signee` role is asserted; `unitadmin` may be asserted without a signee for certificate-authenticated clients. Persistence facades consult that subject for role checks and signee filtering, with container identity retained for other calls.
- Configured documents (schulvorlage: head teachers, student careers, common names, subject names) are no longer created by lookups. `CommonDocumentsInitializer` (`@Singleton @Startup`, persistence module) creates the missing ones at startup; `AdminStudentsProcessor.processStudentsMarkers` additionally creates the student-marker document on write instead of failing with an NPE. Limitation: a document added to the schulvorlage while the server runs is only created on first write (or next restart); readers return null/empty meanwhile.
- `ClientBean` (`BetulaWebService`) had no references in server-web (no Java, no xhtml), and the `IservLogin` JNDI names in `IServIMAPIdentityStore` were an unused constant with commented-out lookup; both were removed without REST replacements.
- Dead code removed: `rest/ClientBean` (no references), the `IservLogin` JNDI constant and commented lookup in `IServIMAPIdentityStore`, all `UnsupportedOperationException` stubs in `config/InternalServices`, and the unused web-side `FastTargetDocumentsRestClient`. `InternalServices` is the web-side REST facade; student and target-document calls use `ServiceInternalClient`.
- `AppResourcesContext` (JNDI) replacement: `GET internal/app-resource?path=<relative file>` (`AppResourceReadResource`, role internal, path-traversal-safe, 404 if missing, `Last-Modified` header) and `ServiceInternalClient.getAppResource`. `AppConfiguration.openAppResource` is used for `web-ui-configuration.xml`, the schulvorlage, signee-bemerkungen and ankreuzzeugnisse. `AppResourcesServlet` (WebDAV at `/web/dav/*`) and its security constraint are removed; clients use the service `/api/dav` resource (decision: REMOVE).
- **Decision:** FOP stays in web; its files (`fop.xconf`, XSL-FO, logos) are read from web's own app-resources directory (`-Dsphairas.web.app-resources`, default `<instanceRoot>/app-resources`) and provisioned as deployment configuration. Remaining JNDI `AppResourcesContext` lookups in `sphairas-server-libs` (`CommonAppProperties`, `ConfigurationPropertiesImpl`, bundle/xml-definition support) are handled in `make-war-dependencies-self-contained`.
- Web logo reads now use the same `sphairas.web.app-resources` directory as FOP.
- All clients of the service API (`AppConfiguration`, `DocumentsService`, and `NamingResolverImpl`) use `ServiceClientConfiguration`; no client overrides hostname verification.
- `AmbiguousDateResponseMapper` maps HTTP 409 from `internal/report-date` back to `AmbiguousDateException`; 400 remains mapped to `BadRequest`.
- `web-report-calendar-rest-migration` uses `GET internal/reports/notes/batch?document=...&key=...` and `GET internal/reports/markers/batch?document=...` during report rendering to fetch repeated values in a single request.
- Verification: after the report/calendar migration, the Docker `rest-baseline.sh` run with `OUT=baseline-after` completed with rc=0; all executed checks passed, including both batch endpoints. `UNKNOWN_STUDENT` was not provided and its optional check was skipped. The user also manually verified the UI and clients. The `web-report-calendar-rest-migration` task is complete.
- Verification: `rest-baseline.sh` with `OUT=baseline-after` (incl. `REPORT`) passed 30/30 checks (rc=0) against the Docker app, including the new endpoints, reports, app-resource and the 500→400 fix. Task closed; acting-subject and batch endpoints are handled in the web migration tasks.
- `web-student-document-rest-migration` is implemented but remains `in_progress` pending deployment/runtime verification. The JDK 25 Maven package build and available Maven tests pass; the extended baseline and manual UI/client checks have not yet been rerun for this migration.
