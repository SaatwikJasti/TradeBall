# Completed deployment handoff

Application deployment verified October 18, 2026; documentation finalized October
19. This record supersedes the pending release steps in publication-review.md.

## Public clients and revision

- Original app: https://tradeball-web.onrender.com/index.html . Create new accounts here.
- React/TypeScript comparison: https://tradeball-web.onrender.com/compare/index.html . Use the same account.
- Backend API: https://tradeball-backend.onrender.com/api/v1 .
- Health: https://tradeball-backend.onrender.com/actuator/health .
- Application SHA: `b3ff73f403358aca528ff67337f67550489e1f7d`, the merge of PR #1.
- Backend merge deploy: `dep-dbak27m7bikc73eoeu0g`, succeeded/live.
- Backend final CORS deploy: `dep-dbak5b690tus73co9gq0`, succeeded/live on the same SHA.
- Frontend deploy: `dep-dbak4p67bikc73euu1l0`, succeeded/live on the same SHA.

The README/documentation revision may be newer than the deployed application
SHA. That does not imply the application needs redeployment for documentation.

## Observed host settings

Backend: repository SaatwikJasti/TradeBall, branch main, root backend, Dockerfile
backend/Dockerfile, Docker build context backend/, On Commit auto-deploy, no
additional build filters shown. Compute stayed Free; existing Supabase and Redis
were preserved. The Docker image serves the API, not www.

Frontend: Render Static Site named tradeball-web, main branch, blank Root
Directory, build `npm ci && npm run build`, Publish Directory `www`, Auto-Deploy
Off. Environment variables: `NODE_VERSION=24.18.0`, `SKIP_INSTALL_DEPS=true`,
`VITE_API_BASE=https://tradeball-backend.onrender.com/api/v1`. No frontend secrets,
custom domain, catch-all rewrite, or paid instance was added.

At release, the Hobby workspace had no card on file and usage was within the
included allowances (1 MB/5 GB bandwidth, 0/500 pipeline minutes before release).
These are dated observations, not ongoing quota guarantees. No paid upgrade or
additional paid usage was enabled. No hosting settings changed for this handoff.

Saved CORS preserved all existing entries and appended only the exact frontend
origin:

```text
http://localhost:3000,http://localhost:5500,http://127.0.0.1:5500,http://127.0.0.1:5501,http://localhost:5501,capacitor://localhost,http://localhost,https://tradeball-web.onrender.com
```

Preflight checks returned 200 with the exact frontend origin, 200 for the
preserved localhost:5500 origin, and 403 with no allow-origin header for an
unrelated origin. No wildcard was used. An initial environment form save retained
the old value; the redundant deployment was canceled and the corrected saved
list and final successful rollout were verified. Other environment values were
unchanged.

## Verification and practical limits

- Latest reviewed PR head and merged application SHA passed frontend/backend
  GitHub Actions CI. Logs showed 10 frontend tests, TypeScript/build success,
  31 Surefire passes, four PostgreSQL Failsafe passes, one opt-in benchmark skip.
- Public original client: dedicated account registration, roster creation, Josh
  Hart search/addition, Nikola Jokic search/selection, trade evaluation, saved history.
- Public React client: shared session restoration, explicit logout/login, existing
  roster restoration, Stephen Curry addition, empty-trade validation, outgoing
  removal/reselection, disabled duplicates and cross-side overlap, real evaluation.
- Both Josh Hart → Nikola Jokic evaluations returned score 100, Great Trade,
  heuristic-v2, and nine categories; outgoing/incoming values were 6.22/13.17.
  Both appeared in shared history. The test account was signed out afterwards;
  credentials were not published. No real user roster or trade was modified.
- Both public HTML pages and generated React JS returned HTTP 200; deployed JS
  contained the HTTPS API base and no localhost API fallback. Health returned
  HTTP 200/UP with liveness/readiness groups after the merge deployment. Render
  then marked the final CORS rollout live and public API/browser checks passed.
- Free backend startup can be slow. The original client eagerly fetches stats
  for all 506 observed players, so initial loading is slow. No new optimization
  or benchmark was performed during publication.
- NBA data freshness remains unverified. The 506-player deployed dataset is
  distinct from the 500-player/1,000-stat-row local benchmark fixture.
- The 74→38 SQL statements and 234→144 ms median for 10-for-10 trades are local
  in-process API measurements at concurrency 1, not production measurements.
  1-for-1 did not improve. Public smoke tests are not scale or uptime tests.
- Session expiry and network-failure scenarios were tested locally previously;
  production lifetimes and availability were not manipulated for verification.
- Native-device validation and four inherited mobile/CLI audit findings remain
  open; no dependency upgrades are part of this documentation update.

GitHub Pages was also found publishing main's repository root at
https://saatwikjasti.github.io/TradeBall/ . It was left unchanged and does not
build the ignored React output; use the Render URLs above as the verified demo.

## Recorded rollback reference

Before merge, backend deploy `dep-dactibmq1p3s73dntqe0` was live on
`4e16161e39318732d4e7f5acc6c737de4ec34398`, deployed September 3, 2026 at
3:31:42 PM CDT. A prior same-SHA successful deploy was also visible.

If a future application release fails, use the backend Deploys page to select
an available known-good successful deploy and choose Rollback, then verify its
SHA, health, authentication, and history. Dashboard rollback disables auto-deploys;
leave them disabled until the issue is resolved. Keep Free compute. Free rollback
artifact retention is limited, so the historical pre-release artifact may no
longer be selectable after later deployments. Do not upgrade to obtain it.

A code rollback does not restore database state. This release introduced no
schema migration; do not reset Supabase or delete user data. Review CORS after a
rollback because environment settings may differ between deploys. Rollback was
documented, not executed. See Render's [rollback documentation](https://render.com/docs/rollbacks).
