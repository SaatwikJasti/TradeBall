# Local trade-evaluation benchmark

This is a deterministic PostgreSQL experiment, not production traffic or a capacity test.
The benchmark lives in `TradeEvaluationPostgresIT.benchmark`; the same class also
tests valuation/history consistency, resource ownership, missing stats, and invalid input
against real PostgreSQL.

## Protocol

- Java 21, Spring Boot 3.3.5, Hibernate 6.5.3, PostgreSQL 16 Alpine in a fresh Testcontainers container.
- Fixture v1: 500 active players, 500 season-2025 stat rows, 500 season-2024 rows with deliberately different values, and two users. Exact formulas are in `fixture()`; there is no random seed or upstream dependency.
- Trade sizes: 1, 3, and 10 players on each side. Incoming IDs are `1..size`; outgoing IDs are `101..100+size`. No roster is supplied for measured requests.
- Concurrency 1. Two rounds; sizes run `1,3,10` then `10,3,1`.
- Each scenario/round has 30 untimed warmups and 100 measured requests: 200 measured requests per size, 600 total per implementation.
- Spring cache type `none`; Hibernate L2 cache off; a fresh transactional persistence context per request. PostgreSQL and OS buffers are warm, not flushed. This is neither a Redis benchmark nor a cold-start benchmark.
- Authenticated MockMvc POST requests include JWT parsing, validation, SQL, scoring, history inserts, transaction commit, and JSON serialization. There is no TCP/HTTP socket or browser overhead. Report numbers as in-process API latency.
- Fixture creation, account registration, request-body creation, history truncation, JSON comparison, report generation, and JVM/container startup are outside measurements. History is truncated before every warmup/measured request so table size does not grow across samples. The truncation warms some database metadata; it occurs identically in both implementations.
- The request-local Hibernate `StatementInspector` counts SQL preparations, with JDBC batching disabled. SELECT and INSERT counts are reported separately, plus total statements. This is not a packet/round-trip counter; Flyway, JDBC fixture commands, and transaction protocol messages are excluded.
- Raw samples include every completed request's status and elapsed nanoseconds converted to milliseconds. The summarizer uses nearest-rank p50/p95/p99, with error rate defined as non-200 responses / measured requests. Exceptions fail the benchmark rather than silently count as success. Inspect the log if no complete raw report was produced.
- Every successful response is compared against its scenario's first response after excluding the generated trade ID. The after run additionally compares all response snapshots to the before report, including scores, category deltas, verdicts, signals, explanations, and model version.

## Run

With Docker Desktop running, from `backend/`:

```sh
mvn --batch-mode -Dtest=TradeApiTest -Dit.test=TradeEvaluationPostgresIT -Dtradeball.benchmark=true -Dtradeball.benchmark.output=target/benchmark-before.json verify
```

After the measured optimization, repeat the identical command, changing only report paths:

```sh
mvn --batch-mode -Dtest=TradeApiTest -Dit.test=TradeEvaluationPostgresIT -Dtradeball.benchmark=true -Dtradeball.benchmark.output=target/benchmark-after.json -Dtradeball.benchmark.baseline=target/benchmark-before.json verify
python scripts/summarize_trade_benchmark.py target/benchmark-before.json target/benchmark-after.json
```

The ordinary verification command is `mvn --batch-mode verify`. The performance
method is opt-in and will be reported as skipped unless `tradeball.benchmark=true`.
When Docker is unavailable, PostgreSQL tests can also skip; those skips are not passes.

The before implementation's `TradeEvaluationService.java` is unchanged from baseline
commit `e89090fcce9e98e40845569b6eb881c93839fcb3`. For an A/B reproduction, use a separate
checkout with the benchmark harness and original service rather than reverting ongoing
work in a shared checkout. Preserve the earlier input-validation and search-cache fixes
in both cases.

## Interpretation

For `N` selected players across both sides, the original loader uses `2N`
SELECTs. The measured baseline has another four SELECTs (three population reads
and one user lookup), and `N + 10` INSERTs (one trade, N player rows, nine category
rows). Its observed total is therefore `3N + 14` statements: 20, 32, and 74 at
the tested sizes. These counts apply to valid authenticated trades without a roster;
roster authorization and rejected inputs have different paths.

The optimization changes only selected-player loading: one `findAllById` query
and one season-filtered `IN` query per side. Maps keyed by player ID support
reconstruction in original request order. Query result order is never assumed.
Missing players and missing current-season stats retain separate not-found errors;
validation and ownership checks still run first. Scoring and persistence code are
unchanged. No population cache, invalidation policy, or database schema is added.

The tradeoff is a small O(N) map allocation and `IN` parameter lists. This is useful
for the measured multi-player inputs, with no read-count advantage for 1-for-1.
Very large input lists and database parameter limits were not tested. Two batch
reads for the entire trade would be a further change; this experiment keeps the
existing per-side loader boundary to minimize behavioral change.

There are three population SELECTs on every evaluation: one to compute population
statistics, then two inside raw-score calculation. They remain unchanged in this
experiment. History writes also grow with trade size. Batching selected-player reads
cannot make the entire request constant-cost or remove the population scoring work.

Two JVM runs are not a controlled production study. Compare individual rounds as well
as pooled distributions: JIT, garbage collection, Windows/WSL scheduling, and database
buffer state can dominate tails. A lower SQL count is direct evidence; a noisy latency
change is not proof of a production speedup. The largest input is a stress scenario,
not a claim about typical league trades. No throughput or production-scale claim follows
from a single-client benchmark.

## Observed results: October 16, 2026

Windows 11, Intel Core i7-13700H (14 cores / 20 logical processors), Temurin
21.0.12+8-LTS, Docker Desktop 29.7.2 (7,748 MB reported engine memory),
PostgreSQL 16.15 on Alpine x86_64. Both runs used the same locally cached
`postgres:16-alpine` image and reported the same PostgreSQL version. The image tag
is mutable: record/compare the actual version when reproducing rather than assume
future runs resolve to identical software. No machine-level background-load isolation
was enforced.

Each row pools 200 samples (100 per round). All query counts were constant within
their scenario, and both implementations had 0/600 request errors.

| Players per side | SELECTs before → after | INSERTs both | Total SQL before → after | p50 ms before → after | p95 ms before → after | p99 ms before → after |
|---|---|---|---|---|---|---|
| 1 | 8 → 8 | 12 | 20 → 20 | 102.200 → 103.205 | 160.717 → 137.032 | 239.933 → 201.622 |
| 3 | 16 → 8 | 16 | 32 → 24 | 130.050 → 107.805 | 184.121 → 176.889 | 244.787 → 207.693 |
| 10 | 44 → 8 | 30 | 74 → 38 | 234.002 → 143.838 | 322.887 → 214.489 | 390.767 → 288.887 |

The post-change formula is eight SELECTs and `N + 10` INSERTs, or `N + 18`
total statements for these valid, unscoped requests. Query reduction is directly
observed. The pooled 3-for-3 median was 17.1% lower; the 10-for-10 median was
38.5% lower. Those are local sample comparisons, not production performance claims.
The 1-for-1 median was about 1.0% higher with an unchanged query count: no
median improvement was observed for that case.

### Individual rounds

| Size | Round | p50 before → after ms | p95 before → after ms | p99 before → after ms |
|---|---|---|---|---|
| 1 | 1 | 113.365 → 109.645 | 178.393 → 142.160 | 279.935 → 156.468 |
| 1 | 2 | 88.799 → 89.803 | 116.849 → 127.748 | 190.701 → 201.622 |
| 3 | 1 | 139.485 → 110.380 | 191.056 → 144.346 | 331.512 → 201.825 |
| 3 | 2 | 121.977 → 104.347 | 168.459 → 197.307 | 206.142 → 244.709 |
| 10 | 1 | 237.009 → 145.441 | 326.208 → 238.745 | 581.076 → 288.887 |
| 10 | 2 | 231.853 → 142.088 | 283.454 → 210.719 | 381.536 → 283.790 |

In round 2, the 3-for-3 p95/p99 regressed despite the lower median. Small-trade
tails are noisy and do not uniformly improve. The 10-for-10 case improved median
and tails in both rounds, supporting retention of this limited batch-fetch change.
The experiment does not identify how much remaining latency comes from population
scoring, writes, GC, or Docker/Windows overhead.

The before benchmark method passed, generated all 600 samples, and found stable
responses. That Maven invocation nevertheless failed because the newly authored
functional history check compared nested numeric values parsed by JsonPath against
Jackson objects. The evaluated and persisted JSON category values agreed when
parsed with the same ObjectMapper. The check was corrected without changing the
benchmark method or production behavior. The after invocation passed both the
functional test and benchmark, with all response snapshots matching the before
report exactly except for generated trade IDs.

Source SHA-256 (working-file bytes):

- Before service: `3cc241036dabb920c2cee44033f41d6f4d4e2e321fe5bcc7c8a5635e508fde03`.
- After service: `95e5c69dac1971a719727577429256f70e7b2969593891c1ac41c3c9c84fbc1b`.
- After stats repository: `c44a7ad5200ec3c9cf6fea2c0cd63fa086f8edba9ab3461014513202db3f1adc`.

### Interview explanation

"I instrumented authenticated trade requests against a deterministic local
PostgreSQL fixture. Read count grew by two SELECTs per selected player. I replaced
per-player reads with two batch reads per side, reconstructing the original order
and retaining season filtering and not-found behavior. Ten-for-ten trades dropped
from 74 to 38 total SQL statements and from 234 to 144 ms median in this local
single-client experiment. One-for-one did not improve, and smaller-trade tails
were mixed. I compared the complete valuation response before and after and tested
history and ownership against PostgreSQL. I would validate under representative
network latency and concurrency before making a production claim."
