# Spring Boot and JPA Performance Contract

Read this reference whenever changing a repository query, list/detail endpoint, transaction boundary, cache, bulk operation, external call, or diagnosing backend slowness.

## Query workflow

1. Capture endpoint latency, query count, and the slow SQL for the affected path.
2. Inspect the generated SQL. Do not infer efficiency from a repository method name or JPA annotation.
3. Run `EXPLAIN` or the database equivalent with representative parameters and data volume.
4. Fix the largest cause: N+1, over-fetching, missing/incorrect index, bad join order, unbounded result, expensive count, or repeated query.
5. Repeat the same measurement.

## Query shape and loading

- Prefer DTO/interface projections when the response needs only a subset of columns.
- Choose among projection, batch fetching, `@EntityGraph`, `JOIN FETCH`, and dedicated queries based on the access path.
- Do not fetch-join a collection in a paginated query unless SQL, duplicates, count query, and in-database pagination are verified.
- Avoid multiple collection fetch joins that create Cartesian multiplication.
- Set bounded page sizes at the API boundary. Use `Slice` when a total count is unnecessary; consider keyset pagination for deep offsets.
- Avoid repository calls inside loops. Fetch or write in batches.

## Index discipline

- Derive indexes from actual `WHERE`, `JOIN`, and `ORDER BY` patterns.
- For composite indexes, verify column order against equality, range, and sort predicates.
- Do not add indexes solely because a column is present. Account for write amplification and selectivity.
- Add or change indexes only through Flyway migrations.
- Keep before/after plans for hot-path query changes.

## Transactions and connections

- Keep transactions short and exclude slow external calls when consistency does not require them inside the transaction.
- Use `@Transactional(readOnly = true)` for read paths when supported by the stack and verify its effect rather than assuming it is an optimization.
- Configure transaction/query timeouts for bounded failure behavior.
- Inspect HikariCP active, idle, pending, timeout, and acquisition metrics before changing pool size.
- Do not increase the pool to hide slow SQL or an overloaded database.

## Bulk work

- Use JDBC/Hibernate batching for bulk inserts and updates when supported.
- Flush and clear the persistence context in controlled chunks for large jobs.
- Avoid loading an entire dataset into memory for export or processing; stream or page with a bounded chunk size.

## Cache contract

Document keys, TTL, invalidation, consistency tolerance, concurrency/stampede behavior, and metrics. Prefer cache-aside for read-heavy stable data. Never cache mutable approval/routing state without proving stale data cannot produce an invalid business decision.

## Observability and tests

- Track endpoint latency distributions, error rate, query latency/count, connection-pool saturation, JVM CPU/heap/GC, and external-call latency.
- Use integration tests with representative database behavior for important repositories.
- Add a query-count regression test for paths previously affected by N+1 when suitable tooling exists.
- Do not log every service call at `INFO`. Prefer sampled/thresholded performance telemetry and metrics to high-volume timing logs.

