# Publication review and proposed release plan

Reviewed October 17, 2026. This is a reviewable plan, not a deployment record.
Updated October 18, 2026 with owner-confirmed backend Render settings. The owner
subsequently authorized the four reviewed commits, a push only of
`feature/tradeball-publication-review`, and a PR against `main` with remote CI
review. Merge/deployment remain unauthorized. No hosting configuration,
credentials, or paid resource was created or changed. The completed benchmark was not repeated. Prior implementation and
raw evidence remain preserved.

### Pre-push verification on October 18

With `VITE_API_BASE=https://tradeball-backend.onrender.com/api/v1` set explicitly,
`npm ci`, all 10 frontend tests, strict TypeScript checking, and production build
passed. `www/index.html`, its JS files, `www/compare/index.html`, and all referenced
React assets exist. The generated React JS contains the requested HTTPS API base
and no localhost API fallback. Frontend CI now sets that same build variable.
Generated `www/compare` remains ignored and must be rebuilt by the static host.
Default local builds without the variable retain the documented local API fallback.
Remote CI results are to be checked on the pushed PR, not inferred from local checks.
Maven verification also passed: Surefire 31 passed/0 skipped; Failsafe four
PostgreSQL correctness tests passed and one opt-in benchmark skipped. The
benchmark was not run. A first local run was stopped after a sandbox JVM attach
pipe failure; the verification rerun completed successfully. No application
change was needed for that local execution restriction.

## Review findings

| Area | Finding and disposition |
|---|---|
| Credentials | No apparent live credentials found by manual diff review and pattern scanning of tracked plus non-ignored untracked candidates. One scanner match is the `USER:PASSWORD` URI example in `backend/README.md`, not an account credential. This is a scoped review, not a guarantee covering every historical commit. |
| Intentional fixture values | Compose database passwords, documented development accounts, test JWTs, and development-only signing values are local/test fixtures. `.env.example` contains placeholders. Production secrets must remain in host settings. |
| Files and paths | No logs, database files, archives, private-key files, or machine-specific user paths found in the publish candidates. Build outputs, node_modules, local test caches, and native build directories are ignored. Additional ignore rules cover local env variants, logs, database dumps/files, and heap dumps. |
| Screenshots | New public screenshots capture only player selection and category results. Account names, roster names, email/password fields, and trade IDs are excluded. Full-page screenshots remain on disk, ignored by Git, and in the prior local evidence bundle. |
| README | Replaced outdated production/current-data assertions with verified product behavior and explicit local/deployment status. Added React/TypeScript, exact setup/testing, precise benchmark scope, and the 1-for-1 non-improvement. Removed the unsupported MIT badge/license claim because no LICENSE file exists. Owner license choice remains open. |
| Reproducibility | Kept the opt-in PostgreSQL benchmark fixture/test, standard-library summarizer, and full methodology/distributions. Raw measurements and original evidence archives remain outside the repository. |
| Application | No new feature, valuation change, auth change, or backend change was made during publication preparation. The original app/API scripts and Capacitor config are unchanged. |

### Commands and CI path checks

The root `package.json` points all frontend commands to `frontend/vite.config.ts`.
That config computes the frontend root from its own URL, uses `frontend/src`,
and builds only into `www/compare/`. Type checking includes source/tests and Vite
config. The frontend CI job runs at repository root with Node 24, `npm ci`,
`npm run test:frontend`, and `npm run build` (which includes type checking).
Backend CI sets its working directory to `backend`, where `pom.xml` exists.

At initial publication review, the new CI definition had not executed remotely.
Latest completed local evidence
is 10 frontend tests, strict type check/build, 31 Surefire tests, and four
PostgreSQL correctness tests passing. The one Failsafe skip is the opt-in
benchmark, not a counted PostgreSQL pass. Publication preparation did not rerun
that benchmark or represent these local checks as remote CI success.

### Dependency review

Fresh `npm audit --json` still reports four findings, all in the inherited
Capacitor/CLI dependency tree:

| Package | Severity | Scope |
|---|---|---|
| `@capacitor/android` | Critical | Existing native Android package, still 8.4.1 |
| `@xmldom/xmldom` | High | Existing Capacitor CLI dependency |
| `brace-expansion` | High | Existing CLI dependency |
| `tar` | High | Existing CLI dependency |

No audit findings are currently attributed to the added React/TypeScript/Vite/
Vitest/Testing Library tree. The earlier newly introduced Vite/Vitest findings
were resolved during the frontend milestone by selecting Vite 7.3.7 and Vitest
4.1.11. No dependencies were upgraded in this publication-preparation pass.
`npm audit` is a point-in-time advisory check, not comprehensive security testing.

Capacitor core/CLI/Android stay at their existing locked 8.4.1 versions. The only
changed pre-existing shared type packages are `@types/node` 26.0.1 to 24.9.1 and
its `undici-types` 8.3.0 to 7.16.0, aligned with the new direct Node 24 type pin.
Do not use a blanket `npm audit fix` to mix native upgrades into this change set.
Review the native/CLI advisories before a mobile release or relying on those
tools with untrusted input. Static publication does not establish Android safety.

## Proposed commit grouping

These reviewed groups were authorized for local commits and feature-branch push. Use
interactive staging for README/ignore hunks shared between concerns, then inspect
each staged diff; do not apply an exported patch to this checkout.

1. **`fix(api): validate trade IDs and include sort in search cache keys`**
   - `TradeEvaluateRequest`, `PlayerService`, `TradeApiTest`, `PlayerApiTest`.
   - Relevant input-validation/cache documentation hunks.
2. **`perf(trades): batch player reads with PostgreSQL regression coverage`**
   - `PlayerStatsRepository`, `TradeEvaluationService`, `TradeEvaluationPostgresIT`.
   - `backend/scripts/summarize_trade_benchmark.py`, `backend/docs/trade-performance.md`,
     and corresponding backend README hunks.
   - Keep measurements explicitly local and preserve request ordering/season filtering.
3. **`feat(web): add the React TypeScript trade comparison workflow`**
   - `frontend/`, root package files, frontend CI, generated-output ignore rules,
     one legacy entry-point link, and public screenshot files.
   - Include the full guide and privacy ignore rules so the commit's links resolve.
4. **`docs: finalize verified setup benchmark and publication status`**
   - Final root README, this review/release plan, and any remaining documentation/
     hygiene hunks. Include only sanitized screenshots; no raw logs or build output.

Resumes and local evidence archives are outside the project checkout and are not
part of these repository commit groups unless the owner separately requests it.

## Hosting facts versus unknown account settings

### Confirmed existing backend service

The owner confirmed these dashboard settings on October 18, 2026:

| Setting | Current value |
|---|---|
| Repository | `SaatwikJasti/TradeBall` |
| Deployment branch | `main` |
| Root directory | `backend` |
| Dockerfile | `backend/Dockerfile` (as reported by the owner) |
| Docker build context | `backend/` |
| Additional build filters | None shown |
| Auto-Deploy | **On Commit** |
| Compute plan/cost | **Free**, confirmed by the owner; preserve without upgrade. Workspace usage/other resource charges remain unverified. |
| Currently live commit | Unconfirmed |

**Pushing or merging the prepared backend changes into `main` can trigger an
automatic backend deployment.** Treat that push/merge as a release action, not
merely a way to run CI. Changes under the `backend` root satisfy the relevant
path scope. Pushing only `feature/tradeball-publication-review` would not trigger
this existing service's linked-branch auto-deploy; other services, preview
configuration, and external deploy hooks have not been audited. Only the feature
branch push/PR is authorized; do not change Render settings or merge to main.

Keep the reported root/Dockerfile/context values intact in this record; before
an eventual release, compare the actual dashboard-resolved paths and successful
build logs rather than guessing a path correction. Frontend hosting remains a
separate pending decision; the backend configuration does not serve `www`.

Repository facts:

- The shared legacy client defaults to `https://tradeball-backend.onrender.com/api/v1`.
  The owner confirmed this backend URL. A read-only GET to
  `https://tradeball-backend.onrender.com/actuator/health` on October 17, 2026
  (Chicago time; October 18 UTC) initially timed out after 45 seconds. On October
  18 the owner restored paused Supabase and verified the existing backend health
  endpoint returned `{"status":"UP","groups":["liveness","readiness"]}`.
  Existing backend health is verified from that owner-provided response; this
  assistant has not independently repeated the check. Health does not identify
  the live revision or prove that the new local changes have been deployed.
  The owner confirms none of this weekend's changes have been committed,
  pushed, or deployed.
- No Render Blueprint, Netlify/Vercel config, or other frontend hosting manifest
  identifies the live frontend provider, deploy branch, root, plan, or build settings.
- `backend/Dockerfile` builds from a context containing `pom.xml` and `src`, and
  packages a Java image. It does not build or serve `www/`.
- The new React build uses `VITE_API_BASE` at build time; absent configuration it
  falls back to local 18080 in a web build. That fallback must not be published.
- The legacy page does not read `VITE_API_BASE`. It needs its existing runtime API
  base/default independently confirmed. Publishing `www` preserves both pages.

Owner/account confirmation still required: current frontend host/URL, currently
live backend commit, workspace plan,
usage/spend limits, and whether the owner wants to update an
existing static host or create a separate one. Do not invent these from README claims.

### Recommended frontend setup (pending approval): Render Static Site

The simplest repository-compatible option is one Render Static Site in the
existing account, serving both clients from `www`. It requires no frontend server,
container, custom domain, new database, or backend plan upgrade. If an existing
frontend host is later identified, review reusing it before creating a duplicate.
This is a recommendation, not a finalized hosting decision or created resource.

This is the exact repository-derived candidate configuration; it has not been
applied or used to create a service. Prefer updating the confirmed existing static
host rather than creating duplicate resources.

| Setting | Proposed value |
|---|---|
| Service type | Static Site, separate from the existing Spring Boot Web Service |
| Repository | `https://github.com/SaatwikJasti/TradeBall` |
| Deploy branch | `main` after the reviewed feature PR is merged in an explicitly approved release |
| Root directory | Repository root (blank dashboard Root Directory) |
| Node version | `NODE_VERSION=24.18.0`, matching the completed local verification |
| Dependency handling | `SKIP_INSTALL_DEPS=true`; install explicitly in build command |
| Build command | `npm ci && npm run build` |
| Publish directory | `www` (not `dist`, `frontend`, or only `www/compare`) |
| React API base | `VITE_API_BASE=https://tradeball-backend.onrender.com/api/v1` |
| Legacy API base | Existing default matches the confirmed backend; verify no runtime override points elsewhere before release |
| Routes | `/index.html` and `/compare/index.html`; no catch-all SPA rewrite required |
| Automatic deploys | Disabled for initial review/manual release; inspect existing backend auto-deploy triggers before any later push |
| Custom domain | None proposed; use the assigned `onrender.com` hostname initially |
| Backend CORS | Add the final static site's exact HTTPS origin while preserving required existing origins; this is a later backend environment/deployment action requiring approval |

Render documents repository/build/publish settings for static deployments and
manual dependency installation via `SKIP_INSTALL_DEPS`.
[Deploy configuration](https://render.com/docs/your-first-deploy),
[static hosting](https://render.com/docs/static-sites),
[Node version setting](https://render.com/docs/node-version).

### Costs and access required before approval

Render documents static sites and managed TLS as free to deploy; usage counts
against workspace bandwidth and pipeline-minute allowances. Overage charges can
apply. The owner's current plan, remaining quota, payment method, and spend limit
are unknown, so a guaranteed zero bill cannot be quoted. The backend remains on
its confirmed Free plan; no upgrade is proposed. Inspect the dashboard
before selecting a release action. No paid backend/database/cache upgrade or
custom-domain purchase is proposed. Other service and workspace charges remain unverified.
[Static-site pricing scope](https://render.com/docs/static-sites),
[free-tier usage limits](https://render.com/docs/free),
[pipeline usage/billing](https://render.com/docs/build-pipeline),
[pricing](https://render.com/pricing).

Required access is the owner's authenticated hosting session with permission to
view/change the selected site and backend CORS settings, plus the host's authorized
GitHub connection to this repository. A manual dashboard flow does not require
posting an API token in chat. CLI automation would need a scoped host token only
if separately authorized. DNS access is needed only if a custom domain is later
requested. Database passwords and the JWT signing secret must never be placed in
`VITE_*` variables, frontend files, screenshots, or commit messages. No new database
credentials are needed for the browser; it talks only to the API.

## Later publication sequence and acceptance checks

1. Confirm owner/account settings, costs/spend limits, and whether an existing
   host can be reused. Identify the exact URLs and check both client bases.
2. Review/stage the proposed groups; run normal checks and observe remote CI on
   approved feature-branch revision only after push authorization. The existing
   backend watches `main` with On Commit auto-deploy: do not push to or merge
   into `main` until deployment is explicitly authorized. Inspect preview/other
   service triggers before any later feature-branch push. Record the current live
   commit and workspace usage/cost limits before approving a backend release;
   keep the confirmed backend Free plan unchanged.
3. Present the concrete hosting/CORS changes and expected cost impact for approval.
   Only then configure/build/deploy the selected frontend and required backend env.
4. Verify HTTPS, original-to-React navigation, static asset paths, no localhost API
   requests, CORS, authentication/session expiry, a real roster-scoped evaluation,
   and persisted history on the actual deployed service. Do not claim this now.
5. Keep the prior host configuration/build for rollback. No schema migration is
   needed for this frontend publication. Update the README/demo URL and resume
   deployment wording only after the deployed workflow is actually verified.

## Exact proposed execution steps (review only; not executed)

### A. Local commits after approval

Run from the existing repository root on `feature/tradeball-publication-review`.
Do not reapply patches, use `git add .`, or include local evidence/resumes.
The four groups below implement the earlier proposed grouping; shared README
documentation is deliberately consolidated in the final group.

```powershell
git branch --show-current
git diff --check
git add backend/src/main/java/com/tradeball/dto/TradeEvaluateRequest.java backend/src/main/java/com/tradeball/service/PlayerService.java backend/src/test/java/com/tradeball/api/TradeApiTest.java backend/src/test/java/com/tradeball/api/PlayerApiTest.java
git diff --cached
git commit -m "fix(api): validate trade IDs and include sort in search cache keys"

git add backend/src/main/java/com/tradeball/repository/PlayerStatsRepository.java backend/src/main/java/com/tradeball/service/TradeEvaluationService.java backend/src/test/java/com/tradeball/integration/TradeEvaluationPostgresIT.java backend/scripts/summarize_trade_benchmark.py backend/docs/trade-performance.md
git diff --cached
git commit -m "perf(trades): batch player reads with PostgreSQL regression coverage"

git add .gitignore frontend package.json package-lock.json .github/workflows/frontend-ci.yml www/index.html images/react-selection-public.jpg images/react-categories-public.jpg
git diff --cached
git commit -m "feat(web): add the React TypeScript trade comparison workflow"

git add README.md backend/README.md docs/publication-review.md
git diff --cached
git commit -m "docs: finalize verified setup benchmark and publication status"
git status --short
```

Inspect every staged diff before its commit; stop if unexpected files appear.
If anything remains after the final group, inspect it rather than staging blindly.

### B. Verify and push only the feature branch after approval

With Docker's Linux engine ready, execute normal verification (no opt-in benchmark):

```powershell
npm ci
npm run test:frontend
npm run build
Push-Location backend
mvn --batch-mode verify
Pop-Location
git diff --check
git log --oneline main..HEAD
git push --set-upstream origin feature/tradeball-publication-review
gh pr create --base main --head feature/tradeball-publication-review --title "Verify trade evaluation and add React comparison workflow" --body-file $prDescriptionPath
```

Set `$prDescriptionPath` to the supplied `TradeBall-proposed-pr-description.md`
deliverable after reviewing it; this variable is required before the PR command. The proposed branch push does not update the backend's linked `main`;
inspect any preview/external hooks first. Confirm both GitHub Actions jobs pass,
report Surefire and Failsafe separately, and do not count the skipped benchmark
as a PostgreSQL correctness pass. No merge or main push in this phase.

### C. Backend release only after separate deployment approval

1. Record the current live commit SHA, successful deployment, existing CORS
   value, database state/available recovery options, and dashboard-resolved Docker
   paths. Preserve Supabase and the backend Free plan; do not change resource tiers.
2. Review the PR's exact head SHA and green remote CI. Approve the merge as a
   deployment action because `main` has On Commit auto-deploy.
3. Merge the reviewed PR into `main` through GitHub. Do not separately run a
   manual backend deployment while the merge-triggered deployment is running.
4. Observe Render's deployment; verify the deployed commit corresponds to the
   merge result and health is `UP`. If it fails, stop frontend publication and
   use the recorded prior deployment for rollback; do not guess migration recovery.

### D. Frontend release only after hosting/CORS approval

1. Confirm no existing frontend host should be reused, inspect workspace usage
   limits, and authorize creating a Render **Static Site**, not a Web Service.
2. Configure the exact table above: repository root blank, branch `main`, Node
   24.18.0, `SKIP_INSTALL_DEPS=true`, `npm ci && npm run build`, publish `www`, and
   `VITE_API_BASE=https://tradeball-backend.onrender.com/api/v1`. Select **Off**
   for frontend Auto-Deploy. No custom domain, rewrites, or secret frontend vars.
   Creating the site starts its initial build/deployment: creation itself must
   wait for approval. Do not create it merely to reserve a hostname now.
3. Record the assigned HTTPS origin. Append that exact origin to the existing
   backend `CORS_ALLOWED_ORIGINS`, preserving required existing entries. Saving
   and applying the environment change can restart/redeploy the backend; include
   this action in release approval and leave the compute plan **Free**.
4. After the backend is ready, verify `/index.html` and `/compare/index.html`,
   assets and both API bases, actual login, roster scope, player selection/removal,
   evaluation/category output, persisted history, error/expired-session behavior,
   and absence of localhost requests. Use a designated account without exposing
   its information in screenshots. Mark each check passed or unverified.
5. Record frontend/backend deployed SHAs and URL. Keep Auto-Deploy Off for the
   frontend unless a later policy change is approved. Retain rollback references.

Free backend idle spin-down/cold starts remain a known demo limitation; this plan
does not add keep-alive jobs or a paid upgrade. Static hosting is free to deploy
but workspace bandwidth/build usage may create charges; inspect the account's
limits before approval. See [static sites](https://render.com/docs/static-sites)
and [Free service limits](https://render.com/docs/free).

The publication plan is reviewable now. Execution remains gated by the missing
owner hosting settings and the user's explicit instruction not to push/deploy.
