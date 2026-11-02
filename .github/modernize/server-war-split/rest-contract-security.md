# REST contracts and caller identity (approved)

Task `rest-contract-security`. Based on static analysis of the code (see [inventory.md](./inventory.md)). Items marked **(verify)** have not been confirmed at runtime and are checked in `behavior-baseline` / `standalone-war-deployment`.

## 1. Current identity behavior (what must be preserved or consciously changed)

1. **Web user identity.** Web authenticates users via Jakarta Security (form login against IServ IMAP/LDAP, roles `signee`, `unitadmin`; client-cert login via the legacy `server-admin-authentication`).
2. **EJB calls carry that identity.** Today, EJBs invoked from web run in the same JVM, so the caller principal and its roles are the end user's. The backend uses this in two ways:
   - Container roles: `@RolesAllowed` on `BetulaService` (`signee`), the target-document facades (`signee`/`unitadmin`), `BetulaAdminService` (`unitadmin`); programmatic `isCallerInRole("unitadmin")` in `ServiceUtils` and `DecoratedUnitDocumentFacade`.
   - Principal-derived signee: `SigneeLocal.getSigneePrincipal(...)` → filters data by the current signee (`DecoratedUnitDocumentFacade`, `BaseDocumentFacade`).
3. **`ReportsBeanImpl`, `StudentVCardsImpl`, `CalendarsBeanImpl`, `StudentsListsLocalBeanImpl`, `TargetDocumentsLocalBeanImpl`** declare no `@RolesAllowed` (ReportsBeanImpl grep-confirmed). Authorization for these is effectively "any role allowed in web.xml" (`signee`, `unitadmin`), enforced only by the web WAR. **After the split, the service WAR must enforce this itself.**
4. **Existing REST calls from web carry no user identity.** `ServiceInternalClient` is built in `AppConfiguration` without credentials. It relies on the JVM TLS client certificate **(verify: system properties `javax.net.ssl.keyStore`)**, which maps to role `internal` (group `internals`) for `/api/internal/*`. The acting signee is passed as an explicit `signee` query parameter. So the existing REST pattern is "trusted service + asserted subject".
5. **Service web.xml:** `/*` requires `unitadmin`; `/api/internal/*` requires `internal`; both CONFIDENTIAL; `CLIENT-CERT` auth. `FastTargetDocumentsRestService` lives outside `/api/internal/*`, so it requires `unitadmin`. **(verify)** whether the web's cert principal is in `unitadmins`; if not, the existing `FastTargetDocumentsRestClient` cannot work today and may be unused.
6. **Observed defects to fix on the way (tracked in tasks 5/6):**
   - Hard-coded base URL `https://localhost:8181/service/api` and accept-all hostname verifier in `AppConfiguration` and `DocumentsService`.
   - State-changing operations are exposed as `GET` (`submit-grade-value`, `submit-text-value`).
   - `FastTargetDocumentsRestService` swallows exceptions into HTTP 500 with `e.getMessage()` as the body.
   - Both `@ApplicationPath("api")` and `@Path("/api/fast-target-documents")` exist, which may yield `/service/api/api/fast-target-documents` **(verify)**.

## 2. Identity model (approved, see section 5)

**Recommendation: trusted web tier with asserted subject** (extends the existing pattern; no new infrastructure).

- Web → service uses mutual TLS. The web WAR's client cert maps to role `internal`. All new endpoints live under `/api/internal/*` and require `internal` only. They are not reachable by end users or external clients.
- **Acting signee and roles are sent only where the business logic needs them** (approved). Endpoints that do not depend on the user (e.g. the `ReportsBean` operations, which have no role checks today) carry no identity. Endpoints whose backend logic filters by signee or checks `unitadmin` (e.g. paths through `DecoratedUnitDocumentFacade`, `BaseDocumentFacade`, `ServiceUtils`) take the acting signee and roles explicitly (header `X-Betula-Signee`, `X-Betula-Roles`, or a query param like the existing endpoints). The service validates them (known signee, roles ⊆ {`signee`,`unitadmin`}) and applies the checks the EJBs did before. Since only `internal` callers are accepted, the values are trusted. Each endpoint's identity need is decided in `service-rest-implementation` by checking whether its call path reaches `getSigneePrincipal`/`isCallerInRole`.
- `SigneeLocal.getSigneePrincipal` and `isCallerInRole` inside the service must read the asserted identity when the call came through `/api/internal/*`. This is the only intrusive piece; it needs a request-scoped "acting subject" CDI bean in the service that falls back to the container principal for non-internal callers.
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
| `StudentsListsLocalBean.findPrimaryUnit`, `getMarkerEntry` | `GET internal/students-lists/primary-unit`, `/marker-entry` | N (`signee-primary-unit` covers a related case only) |
| `TargetDocumentsLocalBean.getTargetAssessmentDocuments`, `getDocumentMarkers` | `FastTargetDocumentsRestClient` `/target-assessment-documents`, `/document-markers` | E (V) |
| `TargetDocumentsLocalBean.getSubjectAlternativeName` | `GET internal/target-documents/{id}/subject-alternative-name` | N |
| `FastTargetDocuments2` (4 methods) | `FastTargetDocumentsRestClient` | E (V: role, double `/api`, JSON for DTOs) |
| `BetulaWebService` (`ClientBean`) | likely `FastTargetDocumentsRestClient` / `UnitsServiceClient` | V; needs a call-by-call check in `web-student-document-rest-migration` |
| JNDI `ReportsBeanImpl` in `PrimaryUnit` | `reports` resource | N |
| JNDI `AppResourcesContext` | service `dav/*` (`AppResourcesResource`) or a new read-only internal endpoint | V |
| JNDI `IservLogin` | `internal/signee-groups` (already used) + remaining methods | V |

### 3.3 Cross-cutting contract rules
- **Serialization:** DTOs shared via `sphairas-server-libs` (`FastTermTargetDocument`, `FastTextTermTargetDocument`, `Marker`, `Grade`, ids); JSON for composites, text for scalars. No Java serialization.
- **Errors:** one `BadRequest` JSON body (existing `BadRequestResponseMapper`) with HTTP status: 400 validation, 404 missing entity (`NoEntityFoundException`), 409 `TermReportDataException`/`AmbiguousResultException`/`AmbiguousDateException`, 403 identity or role failure, 500 never carries an exception message. Web maps these back to the exceptions it already handles.
- **Idempotency/safety:** reads `GET`; writes `PUT`/`POST`/`DELETE` (the two existing `GET submit-*` endpoints become `POST`; no `GET` alias, since the endpoints are internal only).
- **Transactions:** each endpoint is one transaction in the service; web must not assume multi-call atomicity (relevant for sequences like `findTermReports(create)` followed by `setNote`).
- **Performance:** batch reads (`notes`, markers per student list) for the loops in `NdsFormatter`, `ZeugnisArguments`, `NdsFormatReportsBean`.
- **TLS/config:** base URI via MP config (`betula.service.api-url` or `mp-rest` key); trust store plus hostname verification on by default; accept-all only behind an explicit dev flag.
- **No JVM sharing:** no endpoint may take or return container-specific types; all JNDI/EJB lookups in web are removed (verified in `verify-web-boundary`).

## 4. Impact on the following tasks
- `service-rest-implementation`: acting-subject bean, new resources under `internal/*`, error mapper, tests.
- `web-rest-client-configuration`: mTLS keystore config, URL config, remove accept-all verifier, pass the acting signee/roles.
- `behavior-baseline` should capture current role behavior for each of the 15 report methods before anything changes.

## 5. Decisions (approved by project owner)
1. Identity: mutual TLS, web authenticates as `internal`; acting signee/roles are sent only if required by the business logic.
2. The `internal` role may act as any signee. Mitigation: dedicated certificate and network-level restriction.
3. No `GET submit-*` aliases; the endpoints are internal only, so they can change to `POST` directly.
