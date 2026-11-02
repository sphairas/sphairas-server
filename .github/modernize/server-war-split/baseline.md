# Behavior baseline for the web ↔ service split

Task `behavior-baseline`. Purpose: record how the affected workflows behave **before** web switches from EJB to REST, so each migration task can be checked against it.

**Status:** procedures and the REST script are written. They have **not yet been run against a live system**, and the repository has almost no automated tests (only `NdsFormatReportsBeanTest`, an FOP/XSL smoke test, and `VersionChangeLogTest`). Run section 1 once on the current EAR deployment and keep the output as the reference before starting `web-report-calendar-rest-migration`.

## 1. Automated part: REST endpoints that exist today

[rest-baseline.sh](./rest-baseline.sh) calls the existing service endpoints and writes status and body per check into `OUT/`. Run it before and after each change and `diff -r` the two output directories. It only needs `bash` and `curl` on the host; it talks to the container through the published port 8181 (`docker-compose.yml`).

**Steps (host shell, repo root, app container running):**

1. Take the certificate and key that `bin/pre-boot.sh` generated in the `secrets` volume (mounted at `/run/secrets`). The service authenticates by client certificate (`CLIENT-CERT`), and the web WAR presents this server certificate, so using it reproduces the web-to-service call.
   ```
   mkdir -p /tmp/sphairas-bl && cd /tmp/sphairas-bl
   docker compose -f ~/Projekte/sphairas-server/docker-compose.yml cp app:/run/secrets/server.crt .
   docker compose -f ~/Projekte/sphairas-server/docker-compose.yml cp app:/run/secrets/server.key .
   ```
   (`sudo` may be needed for docker. The key is encrypted with `AS_ADMIN_MASTERPASSWORD` from `docker.env`; pass it as `KEYPASS`. Do not paste it into shared logs.)
2. Run the script (parameters you do not know can be left out; those checks are skipped):
   ```
   BASE=https://taurus:8181/service/api RESOLVE=taurus:8181:127.0.0.1 \
   CERT=server.crt KEY=server.key KEYPASS="$AS_ADMIN_MASTERPASSWORD" CACERT=server.crt \
   SIGNEE=<signee> STUDENT=<studentId> TERM=<termId> TARGET=<documentId> UNIT=<unitId> ZGN=<head-teachers-doc> \
   OUT=baseline-before ~/Projekte/sphairas-server/.github/modernize/server-war-split/rest-baseline.sh
   ```
   `CACERT=server.crt` works because the certificate is self-signed; if the TLS hostname does not match `localhost`, drop `CACERT` (the script then uses `-k`).
3. After the change under test, rebuild/restart the container and run it again with `OUT=baseline-after`, then `diff -r baseline-before baseline-after`.
4. The ids can be read from the web UI or the database (`docker compose exec db mysql -usphairas -p sphairas`).

**Id format:** ids are `<id>@<authority>`; a `DocumentId` also has a version, `<id>/<version>@<authority>`, and an omitted version means `unspecified`, **not** `latest`. Documents stored with version `latest` must be addressed as `<id>/latest@<authority>` (e.g. `klassenlehrer/latest@spdev2/1`); take them as the UI/DB shows them. A malformed id gives **404** (JAX-RS parameter conversion), so a 404 on every call usually means wrong id values, not a wrong URL. `UNKNOWN_STUDENT` is optional: a well-formed id of a non-existing student, to record how "not found" behaves.

**Reading the first run:** `ping` must return 200 and name the certificate principal. If it returns 401/403, the certificate is not mapped to the `internal` role (group `internals`); that is a finding for `web-rest-client-configuration`, not a script error. Likewise record what `ftd-fast-term` (needs `unitadmin`) returns.

Covered: `ping`, signee common name/groups/primary unit, student vCard, applicable tickets, `select`, report date, fast-target-documents (including the suspected doubled `/api/api` path), and the negative cases below.

| Case | Expectation to record |
|---|---|
| no client certificate | 401/403 or TLS failure, never 200 |
| plain HTTP | not served (CONFIDENTIAL) |
| well-formed but unknown student (`UNKNOWN_STUDENT`) / malformed id (404) | the current status and body (documents whether the service returns null, 204, 400 or 500) |
| `submit-grade-value` with missing parameters | 400 or 500 (currently `IllegalArgumentException`) |
| `fast-target-documents` with the server certificate | confirms or refutes the `unitadmin` vs `internal` question from the contract document |

## 2. Manual part: workflows reachable only through the web UI (EJB today)

These have no REST endpoint yet, so the only reference is current UI/PDF behavior. For each, record the result (screenshot or exported PDF/XML) with the same test data before and after migration. Use a test database, because several steps write.

| # | Workflow (web entry) | Backend calls | Steps and expected result | Error/authorization cases |
|---|---|---|---|---|
| W1 | Report notes read/edit (`ConfigureReportNotes2`, `BemerkungenBean`) | `ReportsBean.getNote/setNote/getCustomNotes/setCustomNotes`, `getKopfnote/setKopfnote`, `getIntegerValue/setIntegerValue` | open a report, change a note, Kopfnote and an integer value, save, reload: values persist | login as `signee` and as `unitadmin` (both must be allowed); set a value the backend rejects (`TermReportDataException`) and record the message |
| W2 | Report markers (`NdsFormatDetailsBean`, `VorschlagDecorationImpl`) | `getMarkers/addMarker/removeMarker` | add and remove a marker; return value (boolean) and persistence | removing a marker that does not exist |
| W3 | Report lookup (`NdsFormatReportsBean`, `NdsFormatter`, `ZeugnisArguments`, `PrimaryUnit`) | `findTermReports`, `getStudent`, `getTerm`, `getAGs` | open a term's reports for a unit: list, student names, AG entries are complete and identically ordered | student without a report (`create=false` → empty), unit without reports |
| W4 | Report PDF (`ZeugnisServlet`, `/zgnsrv/*`) | `FastTargetDocuments2`, `StudentsListsLocalBean`, `ReportsBean`, `CalendarsBean.getDate` | generate a report PDF for one student and a whole unit; save the PDFs (compare text content, not bytes: FOP embeds the creation date) | student without grades; missing report date (`AmbiguousDateException`) |
| W5 | Student data (`BetulaWebApplication`, `FormatListBean`) | `StudentsLocalBean.get`, `StudentsListsLocalBean.findPrimaryUnit/getMarkerEntry` | student list and the primary unit shown per student | unknown student id |
| W6 | Target documents (`DocumentMapper`, `rest/ClientBean`) | `TargetDocumentsLocalBean.*`, `BetulaWebService.*` | list a signee's target documents, their markers and subject names | signee without documents; document of another signee (is it visible to `signee`? to `unitadmin`?) |
| W7 | Grade entry (`DocumentsService`, `internal/submit-*`) | `submitSingleGradeValue/TextValue` | submit a grade, read it back via `select`; a JMS document event arrives in the web (`DocumentsMessagesListener`) | role `signee` writing to a target of another signee |
| W8 | Calendar/tickets (external clients of `/calendar/resource/{students,tickets,reports}`, `internal/applicable-tickets`, `internal/report-date`) | calendar module | record request/response of each external endpoint with `curl` (status, content type, body sample) | wrong role, unknown id |
| W9 | App resources (`AppResourcesServlet`, `AppConfiguration`, DAV) | JNDI `AppResourcesContext` | the web UI config and templates load; a changed resource is picked up (JMS `AppResourcesNotificator`) | missing resource (`MissingConfigurationResourceException`) |
| W10 | Login (`AuthHandler`, `IServIMAPIdentityStore`, client-cert) | `IservLogin`, `signee-groups` | log in via IServ and via client certificate; groups and roles resulting in `signee`/`unitadmin` | wrong password, unknown user, no role |

Record for **every** manual case: user and role used, input data, resulting UI/PDF/XML, and the log lines of the service on failure.

## 3. Authorization-sensitive behavior to freeze (from the code analysis)
- Calls that today run with the end user's identity and are filtered by it: `DecoratedUnitDocumentFacade` (`getSigneePrincipal`, `isCallerInRole("unitadmin")`), `BaseDocumentFacade`, `ServiceUtils.checkUnitAdmin`, `@RolesAllowed` on `BetulaService` and the target-document facades. W6 and W7 must show for a `signee` vs `unitadmin` account which documents are visible and writable; after the migration the same matrix must result.
- `ReportsBean*` and the student/calendar beans have no role checks: W1–W5 baseline must show both roles allowed.

## 4. Test infrastructure
The project has no test infrastructure and none is added for this split (decision by the project owner). Report generation and the other web workflows are therefore verified manually with section 2, using the same test data and accounts before and after each migration task. Compare generated PDFs by their text content, not byte-for-byte.

## 5. Reference run (REST script, current EAR deployment)

Ids used: SIGNEE `iserv.nutzer@demo.schule.merkur2`, STUDENT `1793484836232@spdev2/1`, TERM `653@mk.niedersachsen.de`, TARGET `chemie-2025-a-zeugnisnoten/latest@spdev2/1`, UNIT `klasse-2025-a@spdev2/1`, ZGN `klassenlehrer/latest@spdev2/1`.

| Check | Result |
|---|---|
| ping, signee-common-name (`Nutzer, Iserv`), signee-groups (`signees`) | 200 |
| signee-primary-unit | 200, `klasse-2021-c@spdev2/1` (with the web certificate principal, so the caller-equals-signee/`unitadmin` filter did not block it; see note 2) |
| student-vcard | 200, vCard |
| student-vcard-malformed-id | 404 |
| applicable-tickets | 200, empty body (no tickets for this combination) |
| select | 200, `de.notensystem#2` |
| report-date | 204 (no date for this unit/term/category; rerun with a combination that has one) |
| submit-grade-missing-params | 500 (`IllegalArgumentException` not mapped to 400) |
| ftd-fast-term | **500** (note 1) |
| ftd-fast-term-doubled-api | 404, no doubled `/api/api` |
| student-vcard-unknown | not run (`UNKNOWN_STUDENT` unset) |

**Findings (inputs to `service-rest-implementation`)**
1. `GET /api/fast-target-documents/fast-term-target-document` fails with `JsonbException: Unable to serialize property 'grade' from FastTermTargetDocument.Entry`. The service cannot serialize `Grade` in JSON-B, so the existing `FastTargetDocumentsRestClient` endpoint is broken for documents with entries. The web side cannot use it as is; it needs a JSON-B adapter for `Grade` (and probably `Marker`/ids) or a different representation. Check the other fast-target-documents operations for the same problem.
2. `signee-primary-unit` returned data for the web certificate principal. Whether this works because the principal is mapped to `unitadmin` or because the check is bypassed is not yet known; confirm with `ping` body (principal name) and the group mapping, since the acting-signee design depends on it.
3. An id without version (`klassenlehrer@spdev2/1`) makes `signee-primary-unit` answer 500: `UnitDocumentFacadeImpl.findKlassenlehrerDoc` looks up version `unspecified`, finds nothing, tries to insert and hits a duplicate key on `klassenlehrer/latest`. Read endpoints must not create entities, and a missing version should resolve to `latest` or give 400/404.
4. `submit-*` endpoints map invalid input to 500, not 400.

## 6. Exact reference command

Run from `/tmp/sphairas-bl` (holds `server.crt` and the decrypted-by-passphrase `server.key` copied from the container). `KEYPASS` is the key passphrase (the default `changeit` unless `AS_ADMIN_MASTERPASSWORD` is set); it is deliberately not stored here.

```
BASE=https://taurus:8181/service/api RESOLVE=taurus:8181:127.0.0.1 \
CERT=server.crt KEY=server.key KEYPASS="$KEYPASS" CACERT=server.crt \
SIGNEE=iserv.nutzer@demo.schule.merkur2 STUDENT=1793484836232@spdev2/1 \
TERM=653@mk.niedersachsen.de CATEGORY=zeugnisausgabe \
TARGET=chemie-2025-a-zeugnisnoten/latest@spdev2/1 UNIT=klasse-2025-a@spdev2/1 \
ZGN=klassenlehrer/latest@spdev2/1 OUT=baseline-before \
~/Projekte/sphairas-server/.github/modernize/server-war-split/rest-baseline.sh
```

Result of the last run: all checks `ok` except `ftd-fast-term` (500, known defect, finding 1). `report-date` stays 204 for `CATEGORY=zeugnisausgabe`, `UNIT=klasse-2025-a`, `TERM=653`: no date configured, recorded as the baseline for the "no date" case.

## 7. Manual workflows
W1–W10 (section 2) were walked through by the project owner on the current EAR deployment and passed.
