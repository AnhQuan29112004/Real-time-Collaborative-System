---
trigger: model_decision
description: Apply when code adds or changes API calls, server-state fetching, large-list rendering, Spring services/repositories, database queries, caching, external calls, or when the user reports slowness, latency, high CPU, memory, or load.
---

# Performance Contract

Performance work is evidence-driven. Architecture patterns and memoization are not proof that code is fast.

## Required workflow

1. **Baseline:** capture the current symptom with the closest available signal: request count/timing, React Profiler, bundle report, endpoint latency, query count, SQL plan, JVM metric, or connection-pool metric.
2. **Locate:** identify whether time is spent in the browser, network, API orchestration, JVM, database, cache, or external service.
3. **Fix one bottleneck:** prefer removing work, duplicate calls, waterfalls, over-fetching, or unnecessary queries before micro-optimizing.
4. **Validate:** repeat the same measurement and report before/after evidence. If measurement is unavailable, state that limitation and do not claim a performance improvement.

## Frontend and API calls

- Use the project's server-state library for reusable list/detail/master data. Do not implement ordinary fetching with `useEffect` plus `useState`.
- Keep raw HTTP calls in API service files. Components consume feature hooks.
- Define stable query keys and explicitly decide cache freshness, invalidation, retry, refetch, pagination, and enablement behavior.
- Pass cancellation signals when supported so obsolete searches, filters, and route requests do not continue in the background.
- Start independent requests concurrently. Do not create avoidable request waterfalls.
- Debounce user-driven search/filter calls where rapid input would otherwise create redundant traffic.
- Do not add `useMemo`, `useCallback`, or `React.memo` by default. Use them only for measured expensive work or required referential stability.
- For large collections, use server pagination first and virtualization when rendering cost is measured to be significant.

## Spring, JPA, and database

- Review every changed hot-path repository query for selected columns, joins, pagination, sort order, and expected indexes.
- Use `EXPLAIN` or the database equivalent before adding or changing an index. Commit indexes through a migration and explain the query pattern they support.
- Prevent N+1 with the smallest suitable tool: projection, batch fetching, entity graph, explicit join, or a dedicated query. Do not fetch-join a collection in a paginated query unless the generated SQL and pagination behavior are verified.
- Bound list endpoints with validated page sizes. Prefer slice/keyset pagination when exact totals are not required or offset cost becomes significant.
- Batch bulk writes and avoid per-row repository calls inside loops.
- Keep transactions as short as correctness allows. Use read-only transactions for read paths when supported and define timeouts for expensive or external operations.
- Inspect connection-pool saturation, slow-query data, JVM CPU/heap/GC, and downstream latency before blaming application logic.

## Cache safety

Every cache must document:

- data suitability and consistency requirement;
- key structure and scope;
- TTL/freshness policy;
- invalidation owner and mutation paths;
- stampede/concurrency behavior where load is high;
- hit/miss/error metrics when the cache is operationally important.

Do not add cache as the first response to an unknown bottleneck. Never cache authorization decisions or mutable workflow state without an explicit correctness analysis.

## Definition of done

- [ ] Baseline and bottleneck are recorded.
- [ ] Request/query counts and payload size did not regress unintentionally.
- [ ] Cache and invalidation behavior are explicit if caching changed.
- [ ] Relevant tests, type checks, and builds pass.
- [ ] The same measurement was repeated and the result was reported.

