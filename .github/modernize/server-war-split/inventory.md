# Inventory: web → service/persistence/calendar boundary

Result of task `inventory-boundary`. Static analysis of `server-web` (no runtime verification yet; see `behavior-baseline`).

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

- `ServiceInternalClient` (`/service/api/internal/*`), built in `AppConfiguration`, plus `DocumentsService`. Base URL is the hard-coded `ServiceInternalClient.URI_SERVICE_API` (`https://localhost:8181/service/api`), with an accept-all hostname verifier.
- `FastTargetDocumentsRestClient` (MP config key `fast-target-documents`) ↔ `FastTargetDocumentsRestService` (`/api/fast-target-documents/*`, 20 operations). This covers most of `FastTargetDocuments2`/`TargetDocumentsLocalBean`; the replacement is still to be checked.
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
| service | `/service` | `@ApplicationPath("api")`: `internal/*`, `db/submit-task`, `units/solicit`, `dav/*`, `api/fast-target-documents/*` | `unitadmin` on `/*`; `internal` on `/api/internal/*`; CONFIDENTIAL |
| calendar | `/calendar` | `@ApplicationPath("resource")`: `students`, `tickets`, `reports` (plus servlets `/students/*`, `/tickets/*`, `/reports/*`) | `unitadmin`; non-portable JNDI names disabled |
| server-admin-authentication | `/admins` | `/*` | `unitadmin` role mapping |

The context-root assumptions (`/service/api`, `/calendar`, `/web`) are in the EAR pom (`maven-ear-plugin`), the Dockerfile and the hard-coded `URI_SERVICE_API`. Calendar's external clients move to new URLs (user-confirmed).

## 3. Persistence units
- `betula0` (`jdbc/betuladb`, persistence module) and `studentsPU` (`jdbc/studdb`) / `calendarsPU` (`jdbc/calendar`) (calendar module).
- `betula0` has schema-generation `create` / `eclipselink.ddl-generation=create-tables`; this needs a safety review before repackaging.
- `server-service/src/main/resources/META-INF/persistence.xml` declares an empty `my_persistence_unit`; clean it up when persistence moves in.
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
