# Server WAR Split Refactoring Plan

## Goal

Refactor the application into two independently deployable WARs, `server-web` and `server-service`, with REST as the runtime boundary between them. The WARs may both depend on `sphairas-server-libs`, but must not depend on cross-module EJB/JNDI method invocations or any other shared-JVM behavior.

The split is performed on Payara Full. Preserve the existing modernized UI and its technology. The `server-admin-authentication` WAR remains a separate legacy support deployment for parallel client-certificate authentication and web login; it is not part of the future two-WAR bundle. Moving to a MicroProfile-only runtime is a separate future major effort, outside this plan.

## Approved architectural decisions

- Both WARs may use `sphairas-server-libs`; each must be independently deployable and must not require the other WAR's classes or in-process services.
- Web-to-service business operations use REST. Do not retain direct EJB injection, implementation-specific JNDI lookups, or other in-process invocation across the WAR boundary.
- External clients may be reconfigured to use new calendar URLs; preserving the old calendar URLs is not required.
- Convert persistence and calendar to ordinary JAR modules included by `server-service`.
- Remove the EAR from the future application bundle and deploy `server-web` and `server-service` as separate WARs.
- Keep Payara Full and the current UI technology for this milestone. MicroProfile-only compatibility is explicitly deferred.

## Current-state evidence and scope

- Status: the EAR has been removed. The reactor builds `server-persistence` and `server-calendar` (JARs bundled into `server-service`), `server-web` and `server-service` (independent WARs), and `sphairas-server-libs` (JAR). The Dockerfile deploys the two WARs and, separately, `server-admin-authentication`.
- `server-web` imports numerous `org.thespheres.betula.server.beans` types. The relevant issue is not use of the shared JAR itself, but use of server-owned services by in-process EJB/JNDI calls.
- REST client/server code already exists, including `ServiceInternalClient`, `InternalAPI`, and fast target-document REST endpoints. Extend and reuse these contracts where appropriate.
- The web module contains direct service invocations and global JNDI lookups, including report, calendar, student, target-document and resource access. These callers must be inventoried and routed through REST or web-owned behavior.
- Calendar has its own WAR descriptors, servlet routes, REST application and persistence units. Its HTTP resources and runtime descriptors must be deliberately incorporated into the service deployment when calendar becomes a JAR.
- Persistence configuration currently includes schema-generation settings such as `create` and `create-tables`; their impact must be checked before deploying the new packaging.

## Implementation sequence

### Phase 1 — Establish contracts and a behavioral baseline

1. Inventory all web-to-service calls, shared-JAR imports, EJB injections, JNDI lookups, REST routes, authentication/authorization rules, persistence units, and externally consumed URLs.
2. Record focused baseline tests or reproducible checks for the affected web and service behavior, including report viewing/editing, student and target-document access, signee information, calendar operations, and report-date lookup.
3. Define the REST boundary and identity propagation. Specify request/response types, serialization, error mapping, TLS/service URL configuration, and authorization for each operation. Existing EJB calls do not automatically carry the same identity semantics as HTTP calls, so preserve or explicitly replace those semantics.

**Exit gate:** each existing cross-boundary operation has a replacement contract or a documented reason to remain within one deployment; the identity and authorization design is explicit.

### Phase 2 — Expose service behavior through REST and migrate web callers

4. Extend the service REST API for all web-used operations not already covered. Reuse existing endpoints when their behavior matches. Include report reads and writes, calendar/date operations, student data, target-document operations, and any resource access currently obtained through a service JNDI lookup.
5. Add contract-level tests for response serialization, error cases, and security. Preserve significant current behavior, including ambiguous-date handling and operation failures.
6. Replace web EJB injections and implementation-specific JNDI lookups with REST clients and web-local adapters. Keep shared domain types and utilities where appropriate; do not replace the UI technology.
7. Configure service endpoints and TLS/authentication through runtime configuration. Remove hard-coded service destinations and permissive hostname verification.

**Exit gate:** affected web behavior works through REST; no web code invokes a service-owned bean through EJB, global JNDI, or another same-JVM mechanism.

### Phase 3 — Fold persistence and calendar into the service WAR

8. Change `server-persistence` and `server-calendar` to ordinary JAR packaging and add them as runtime dependencies of `server-service`.
9. Incorporate calendar REST resources and servlet functionality into the service deployment. Relocate or merge required web descriptors, CDI/EJB metadata, persistence descriptors, and resource configuration; resources under a former WAR's `src/main/webapp` will not automatically be included by JAR packaging.
10. Define the new calendar URLs under the service deployment, update clients/configuration, and verify that calendar, ticket, student, and report-date routes preserve their intended behavior and authorization.
11. Preserve the persistence-unit names and datasource bindings unless an explicit migration is approved. Verify entity discovery, CDI/EJB discovery, transaction behavior, timer/JMS behavior, and application-resource lookup after repackaging.

**Data-safety gate:** inspect and resolve the production effect of schema-generation settings before deploying; do not permit an accidental schema recreation or destructive migration.

**Exit gate:** a standalone service WAR contains and runs the service, persistence, and calendar functionality without relying on the EAR.

### Phase 4 — Deploy the two WARs independently and remove the EAR

12. Ensure each WAR packages its required shared-library dependencies or otherwise documents an intentional server-wide library provision. The recommended default is for each WAR to package its own dependencies so neither depends on the other's classpath.
13. Configure standalone deployment context roots and update Docker/build scripts to deploy `server-web` and `server-service` as separate WARs. Remove EAR assembly from the future bundle and eliminate EAR-specific JNDI names and assumptions.
14. Keep `server-admin-authentication` outside the two-WAR bundle. Verify that its existing parallel client-certificate and web-login role remains available during the split; do not silently remove or rewrite it as part of EAR removal.
15. Run independent deployment and integration checks for both WARs, including REST connectivity and authorization, authentication, report workflows, document access, calendar routes, persistence, and startup.

**Final exit gate:** the application bundle consists of the independently deployable web and service WARs, the web/service runtime boundary is REST-only, and the legacy authentication WAR remains a separately managed support deployment.

## Explicitly deferred

- Replacing Payara Full with a MicroProfile-only runtime.
- Replacing or redesigning the current UI technology.
- Removing `sphairas-server-libs` as a dependency from either WAR.
- Rewriting the separate `server-admin-authentication` module.
- Preserving the old calendar URLs for external clients.

## Acceptance criteria

- `server-web` and `server-service` build and deploy independently on Payara Full without the EAR or each other's classes.
- The web WAR has no direct EJB/JNDI invocation path to service-owned operations.
- All migrated web-to-service calls use tested REST contracts with configured destinations and explicit authentication/authorization behavior.
- Persistence and calendar are JARs included by the service WAR, and their resources, routes, persistence units, transactions, timers and messaging behavior are verified.
- External calendar consumers can use the new service URLs; old URLs need not be retained.
- No unintended database schema recreation occurs during packaging changes or deployment.
- The current UI remains unchanged in technology, and the MicroProfile-only migration remains a separate future effort.
- `server-admin-authentication` is excluded from the future two-WAR bundle but remains separately deployable for its current authentication role.

## Execution tracking

See [`tasks.json`](./tasks.json) for the dependency-ordered work items and their acceptance criteria.
