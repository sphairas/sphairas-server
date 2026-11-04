# Inventory: web → service/persistence/calendar boundary

Result of task `inventory-boundary`. This is the original static boundary snapshot of `server-web`; see the implementation notes in [rest-contract-security.md](./rest-contract-security.md) for subsequent migrations. It had no runtime verification at the time (see `behavior-baseline`).

## 1. Active web → backend invocations

### 1.1 EJB injections (`@EJB(beanName=…)`), in-JVM, identity propagated by container

| Bean (shared interface) | Provider (persistence/calendar) | Web callers | Methods used by web |
|---|---|---|---|
| `ReportsBean` | `ReportsBeanImpl` | `VorschlagDecorationImpl`, `ConfigureReportNotes2`, `NdsFormatReportsBean`, `NdsFormatDetailsBean`, `NdsFormatter`, `BemerkungenBean`, `ZeugnisArguments`, `PrimaryUnit` (JNDI) | `addMarker`, `removeMarker`, `findTermReports`, `getAGs`, `getCustomNotes`, `setCustomNotes`, `getIntegerValue`, `setIntegerValue`, `getKopfnote`, `setKopfnote`, `getNote`, `setNote`, `getMarkers`, `getStudent`, `getTerm` |
| `StudentsLocalBean` | `StudentVCardsImpl` (calendar) | `BetulaWebApplication`, `FormatListBean`, `NdsFormatReportsBean`, `NdsFormatter`, `ZeugnisArguments` | `get` |
| `StudentsListsLocalBean` | `StudentsListsLocalBeanImpl` | `BetulaWebApplication`, `FormatListBean`, `NdsFormatReportsBean`, `ZeugnisServlet`, `NdsFormatter` | `findPrimaryUnit`, `getMarkerEntry` |
| `CalendarsBean` | `CalendarsBeanImpl` (calendar) | `ZeugnisArguments` | `getDate` |
| `TargetDocumentsLocalBean` | `TargetDocumentsLocalBeanImpl` | `DocumentMapper` | `getDocumentMarkers`, `getSubjectAlternativeName`, `getTargetAssessmentDocuments` |
| `BetulaWebService` | `BetulaService` (persistence) | `rest/ClientBean` | `getDocuments`, `getFastTermTargetDocument(s)`, `getFastTextTermTargetDocument`, `getPrimaryUnit`, `getTargetAssessmentDocuments`, `solicit` (confirm in task 7) |
| `FastTargetDocuments2` (CDI `Instance`, request-scoped producer) | persistence | `ZeugnisServlet` | `getFastTermTargetDocument`, `getStudents`, `getTargetAssessmentDocumentsForTerm`, `getTextTargetAssessmentDocumentsForTerm` |

### 1.2 JNDI lookups (hard-coded EAR layout `Betula_Server/Betula_Persistence`; breaks when EAR is removed)

| Caller | Target |
|---|---|
| `PrimaryUnit:483` | `ReportsBeanImpl!…ReportsBean` |
| `AppConfiguration:328`, `AppResourcesServlet:37` | `AppResourcesContext` (`DirContext`, app resources/DAV store) |
| `IServIMAPIdentityStore:40-41` | `IservLoginImpl!…IservLogin` (two alternative names) |

### 1.3 Already REST (target pattern)

- `ServiceInternalClient` (`/service/api/internal/*`), built in `AppConfiguration`, plus `DocumentsService` and `NamingResolverImpl`. All use `ServiceClientConfiguration`; configure `betula.service.api-url` (system property) or `BETULA_SERVICE_API_URL` (environment variable). Client hostname verification is not overridden.
- `FastTargetDocumentsRestService` (`/api/fast-target-documents/*`, 20 operations) remains available. The web-side `FastTargetDocumentsRestClient` was removed during `web-student-document-rest-migration`; web target-document calls now use `/api/internal/target-documents/*` through `ServiceInternalClient`.
- `UnitsServiceClient` ↔ `UnitsServiceResource` (`/units/solicit`).
- `InternalAPI` (`/internal`): unit-common-name, signee-common-name (now also used by `NdsFormatDetailsBean`), signee-groups, signee-primary-unit, applicable-tickets, select, submit-grade/text-value, student-vcard, report-date, ping.
- **Gaps** with no REST equivalent yet: the `ReportsBean` read/write operations, `StudentsListsLocalBean.findPrimaryUnit/getMarkerEntry`, and `AppResourcesContext` (the service has `dav/*`, which may cover it).

### 1.4 Shared-library types used by web (`sphairas-server-libs`)
Value/DTO types are fine to share: `FastTermTargetDocument` (14 files), `FastTextTermTargetDocument` (10), `CommonDocuments` (6), `Current`/`Preceding`/`DocumentsRequest`/`Arbeitsgemeinschaft` qualifiers, exceptions (`AmbiguousResultException`, `AmbiguousDateException`, `TermReportDataException`, `NoEntityFoundException`, `MissingConfigurationResourceException`), `Utilities`, `config.*` (`LayerConfigUtilities`, `CommonAppProperties`, `ConfiguredModelException`), `clients.*`, `services.ws.*`.
Only the EJB interfaces `ReportsBean`, `StudentsLocalBean`, `StudentsListsLocalBean`, `CalendarsBean`, `TargetDocumentsLocalBean` and `FastTargetDocuments2` must stop being used as beans by web. Their types can remain as DTO/contract classes, or be replaced.
`SigneeLocal` was removed from web (done).

### 1.5 Messaging (server-level JMS, not in-JVM, so it survives the split)
- Web MDBs `DocumentsMessagesListener`, `push/DocumentListener` (topic `jms/documents-topic`, factory `jms/document-topic-factory`) and `TicketsMessageListener` (`jms/tickets-topic`) subscribe to topics published by persistence (`DocumentsNotificator`, `TicketsNotificator`) and calendar (`StudentsNotificator`).
- Both WARs must remain on the same broker and have the same JMS resources, which is a deployment prerequisite. Subscription names are fixed, so check for duplicate-durable-subscriber clashes.
- Calendar also has its own MDB (`TicketRemovalListener`, `jms/tickets-topic`), and persistence has `DocumentTicketExpiryListener`.

## 2. Routes, security, context roots

| Module | Context root (EAR) | Routes | Security |
|---|---|---|---|
| web | `/web` | Faces `/ui/*`, `*.xhtml`, `/zgnsrv/*`, `/dav/*` servlet mapping, `/resource/*`, MP `/provider/{name,version}` | roles `signee`, `unitadmin` on `/*`; public: login/index/resources |
| service | `/service` | `@ApplicationPath("api")`: existing service endpoints plus calendar REST `/calendar/{students,tickets,reports}`; servlet paths `/calendar/{students,tickets,reports}/*` | `unitadmin` on `/*`; `internal` on `/api/internal/*`; CONFIDENTIAL |
| calendar (merged into service) | removed | Previous REST `/calendar/resource/{students,tickets,reports}` now `/service/api/calendar/{students,tickets,reports}`; servlet suffixes remain at `/service/calendar/{students,tickets,reports}/*` | Service `unitadmin` policy and `unitadmins` role mapping |
| server-admin-authentication | `/admins` | `/*` | `unitadmin` role mapping |

The context-root assumptions (`/service`, `/web`) are in the EAR pom (`maven-ear-plugin`), the Dockerfile and service-client configuration. The calendar module is now a JAR within the service WAR; its old `/calendar` context is removed. Both WARs package `sphairas-server-libs` locally; it is no longer an EAR shared-library dependency. Deployers must update external calendar URLs as described in [deployment-configuration.md](../../../docs/deployment-configuration.md).
The service's `ServiceAuthHandler` remains the single certificate authentication mechanism; the former calendar-specific handler was removed to avoid competing Jakarta Security handlers after consolidation.

## 3. Persistence units
- `betula0` (`jdbc/betuladb`, persistence module) and `studentsPU` (`jdbc/studdb`) / `calendarsPU` (`jdbc/calendar`) (calendar module).
- The persistence and calendar modules now package as ordinary JARs and are runtime dependencies of `server-service`; their `persistence.xml` descriptors remain in their JAR roots.
- Existing schema generation is retained: `betula0` has `create` / `create-tables` and writes DDL to `domain-dir/config/betuladb-script`; `studentsPU` has `create`; `calendarsPU` has `create` / `create-or-extend-tables`. Packaging is not intended to change these behaviors.
- The unused empty `server-service/src/main/resources/META-INF/persistence.xml` (`my_persistence_unit`) has been removed.
- Web has no persistence units and no `@PersistenceContext`.

## 4. server-admin-authentication
A legacy module (`/admins`, role `unitadmin`) that enables parallel client-certificate authentication and web login for the web module. It is deployed separately (Dockerfile) and will **not** be part of the future bundle. It is outside the EAR and not affected by the split, but remove its deployment step when the legacy auth path is dropped.

## 5. Findings that shape later tasks
1. All persistent calls from web go through 6 bean interfaces; the gap list in 1.3 is the scope for tasks 6/7.
2. `ReportsBean` is by far the largest surface (15 methods, 8 call sites, including writes). Needs a dedicated REST resource and a clear identity/authorization story (task 3).
3. `NdsFormatter`, `ZeugnisArguments` and `NdsFormatReportsBean` call the beans synchronously in tight loops; a naive 1:1 REST translation risks N+1 latency. Consider batch endpoints.
4. JNDI names in web (3 places) are tied to the EAR; replace them via REST/config.
5. `provided` scope for `sphairas-server-libs` in web/service poms must become bundled (task 11).
6. JMS topics are the one remaining coupling between WARs besides REST; document it as an accepted, deployment-level dependency.
