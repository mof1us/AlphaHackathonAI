# Backend maintenance instructions

## Layout and ownership

This directory is the Gradle root. Use Java 21 and the checked-in wrapper.
The eight modules are utils, domain, data, authorization, integrations, server,
authorization-server, and temporal-runner. Keep the dependency graph acyclic:
domain -> utils; data -> domain/utils; integrations -> domain/utils; authorization
-> domain. Applications compose modules. Never make domain depend on adapters,
or make server depend on another executable application.

Keep HTTP controllers in server, servlet authorization endpoints in
authorization-server, reusable authorization code in authorization, persistence
in data, domain use cases/interfaces in domain, and Temporal adapters in
integrations. Domain services use repositories; repositories use DAO interfaces.
The authorization modules currently provide a scaffold, not an implemented login
or persistent OAuth model. Do not describe that scaffold as complete auth.

## Persistence

There is exactly one application database, configured by DB_HOST, DB_PORT,
DB_NAME, DB_USER and DB_PASSWORD. JDBC and R2DBC point to that same database.
Use the single unqualified JooqScope and ReactiveTransactionManager. Do not
introduce the reference project's problems/authorization database split.
JooqScope.use runs a suspending receiver block without an implicit transaction.
Its await helpers execute R2DBC publishers through Flux so coroutine/Reactor
context reaches CoreSubscriberProvider. Do not call blocking jOOQ fetch/execute
in DAOs. Keep JooqConnectionFactory outside the pool and transaction management
on the underlying pool; Spring owns connection release within a transaction.
ITransactionManager.transaction must roll back on exceptions and cancellation.

Migration sources belong in data/src/main/resources/migrations. :data:migrate
and :data:generateJooq are explicit maintenance tasks, not build dependencies.
Do not apply migrations to an existing/shared database without an explicit
request. Generated jOOQ Kotlin belongs in data/src/main/kotlin/.../data/jooq;
commit generator output and never hand-edit it. Test-only query fixtures are
not application schema metadata. No business schema has been defined yet.

## Temporal

integrations creates lazy shared WorkflowServiceStubs, WorkflowClient,
ScheduleClient and WorkerFactory. Only temporal-runner starts polling and
reconciles schedules. Its component scan excludes server and authorization.
Register each queue in TemporalTaskQueue, implementation/activity pairing in
TemporalWorkersFactory, and workflow interface in TemporalWorkflowTypes so
suspend workflow results retain their generic Kotlin types.

Keep the Kotlin Jackson 2 converter separate from Spring's HTTP mapper. Strip
only trailing Continuation arguments before serialization. Suspend activities
must complete through local manual completion after real suspension, not return
the suspension marker or use runBlocking. Preserve explicit retry options;
default maximumAttempts=1. Workflow code must use Temporal's deterministic APIs,
not coroutine dispatchers/delay or external I/O. Activities may suspend normally.
Propagate only user id and safe session metadata; never access tokens or cookies.
withActivityContext replaces HTTP identity and restores the caller on return.

WorkflowScheduler implementations must be open, have public no-arg constructors,
implement exactly one annotated workflow interface and be registered Spring
beans. Validate declarations before any schedule mutation. Reconcile declared
IDs with create/update; retain pause state and never wipe a namespace. Removing
a declaration intentionally leaves the stored schedule until manually retired.

The user explicitly excluded imported logging infrastructure. Do not restore
ContextLogger, SQL listeners, OpenTelemetry/Micrometer adapters or exporters.
Ordinary library/SLF4J logs are sufficient.

## Verification and runtime

Run ./gradlew build. Tests cover the single lazy pool, scope semantics,
transaction reuse/commit/rollback/cancellation, isolated application contexts,
embedded Temporal suspension/payloads/failures, and mocked schedule updates.
Do not aim tests at shared Temporal namespaces. The runner context test disables
workers explicitly; embedded tests validate actual workflow execution.

Dockerfile has server, authorization-server, temporal-runner targets, built from
source. Root Compose owns runtime topology. bootRun uses this Gradle root as
its working directory so all applications import the same .env. Keep secrets
out of Docker build contexts. Update the Russian README with ownership,
configuration, command, or contract changes.
