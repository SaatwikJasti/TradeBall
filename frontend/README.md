# React + TypeScript trade comparison

One incremental workflow alongside the original JavaScript app. The original
`www/index.html` and scripts remain the application entry point. React source is
in `frontend/src`; Vite builds only into `www/compare/`. The existing Capacitor
configuration still uses `www`. No backend source or benchmark was changed in
this milestone.

## Local startup: Windows PowerShell

Prerequisites verified locally: Java 21, Maven, Node 24/npm, Python, Docker Desktop
with the Linux engine running. Run commands from the repository root unless noted.

Start isolated PostgreSQL (port 15432 avoids the existing database on 5432):

```powershell
docker compose -p tradeball-react-local -f frontend/compose.local.yml up -d
```

In a separate terminal, start the existing backend:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:DATABASE_URL = 'jdbc:postgresql://localhost:15432/tradeball_react'
$env:DATABASE_USERNAME = 'tradeball'
$env:DATABASE_PASSWORD = 'tradeball'
$env:SERVER_PORT = '18080'
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:5500,http://127.0.0.1:5500,http://localhost:5173,http://127.0.0.1:5173,http://localhost,capacitor://localhost'
$env:NBA_API_BASE_URL = 'http://127.0.0.1:9'
$env:NBA_API_MAX_RETRIES = '0'
$env:NBA_API_CONNECT_TIMEOUT_MS = '500'
$env:NBA_API_READ_TIMEOUT_MS = '500'
$env:JWT_EXPIRATION_MS = '86400000'
mvn --batch-mode spring-boot:run
```

Wait for startup **and** the seed sync to finish. This intentionally invokes the
existing development-only fallback snapshot after a fast local connection
failure. It seeds 40 players and season-2026 statistics, not live NBA data. Trade
scores are computed and persisted by the real Spring Boot backend against
PostgreSQL; there are no mocked responses in the application.

Seeded local account: `demo@tradeball.local` / `Demo1234!`. These are existing
development credentials, never production credentials. Do not use this profile
or fixture database for production.

In another terminal at the repository root:

```powershell
npm ci
npm run dev
```

Open http://127.0.0.1:5173/. Vite proxies `/api` to port 18080. The CORS allow-list
above is still required because the browser Origin header is forwarded. Initially
the demo account has no roster in this new database: create one and add a player
you own, then select them as outgoing.

## Production build and original-app navigation

```powershell
npm run build
python -m http.server 5500 --bind 127.0.0.1 --directory www
```

Open http://localhost:5500/compare/index.html. This serves the built static bundle,
not Vite. Its local default API base is `http://localhost:18080/api/v1`.
http://localhost:5500/index.html still serves the original app and includes a link
to the comparison page. The comparison page links back to the original app.

For a different API origin, set `VITE_API_BASE` before `npm run dev` or
`npm run build`; it must include `/api/v1`. The backend must allow that frontend
origin. This is a public URL, not a secret. The original app retains its previous
API-base configuration; this milestone did not redirect it or validate all its
screens against the new local database.

Vite uses relative asset paths so the page can live beneath `www/compare/`.
`www/compare/` is generated and ignored; build it before `npm run sync` when
including the new page in a Capacitor package. Android retains the existing
client's emulator base if no explicit API base is supplied. With this isolated
backend, set `VITE_API_BASE=http://10.0.2.2:18080/api/v1` before a mobile build.
An Android device/emulator build was not verified.

## Checks

```powershell
npm run typecheck
npm run test:frontend
npm run build
cd backend
mvn --batch-mode verify
```

If a sandboxed Windows run reports EPERM renaming a Vitest cache file under the
system temp directory, use a writable workspace temp directory for that process:

```powershell
New-Item -ItemType Directory -Force frontend/.test-temp | Out-Null
$env:TEMP = (Resolve-Path frontend/.test-temp).Path
$env:TMP = $env:TEMP
npm run test:frontend
```

This redirects only test-process temporary files; it does not disable a security
check. Linux CI uses normal temporary files. `.github/workflows/frontend-ci.yml`
adds npm install, focused tests, and production build/typecheck to CI. That new
workflow has not run remotely because these changes are uncommitted/unpushed.

## Components and state

- `App.tsx`: `App` restores the existing JWT session and switches between sign-in
  and the trade screen. `SignIn` posts email/password through the existing API
  client. `TradeWorkflow` owns rosters, selected IDs/players, result, loading,
  validation, and API-error state. `Selected` renders removable selections.
- `PlayerSearch.tsx`: a reusable controlled search input for roster additions,
  outgoing roster filtering, and incoming backend search. Incoming/add-to-roster
  searches debounce for 250 ms. Effect cleanup prevents old responses replacing
  newer results or updating a component after navigation.
- `TradeResult.tsx`: renders the actual score, verdict, explanation, raw side
  values, and every returned category. Turnover impact comes from the response,
  rather than assuming a positive raw delta is always good. Percentage deltas
  are labeled in percentage points.
- `selection.ts`: checks duplicate/cross-side IDs, nonempty sides, and outgoing
  roster membership before submitting. The server still independently validates
  input and ownership. Frontend validation does not provide authorization.
- `types.ts`: explicit shapes matching the backend DTOs: `Player`, `Roster`,
  `User`, paginated search, `Evaluation`, category results, and typed client
  methods. Literal unions restrict category, verdict, and impact values. These
  are manually maintained compile-time contracts, not runtime schema validation.
- `api.ts`: bundles `www/js/tradeball-api.js` and exposes typed access to it.
  Fetch, timeout, JWT storage, Bearer headers, response parsing, and 401 cleanup
  remain in that shared existing client. There is no second scoring/auth system.

React state is held at the lowest shared owner: searches own query/results;
the workflow owns both trade sides and the current evaluation; the app owns
authentication. Selecting/removing players or switching/refreshing rosters clears
the previous result. Evaluation disables mutations while it is pending. A 401
clears the shared session, unmounts private trade state, and prompts sign-in.
Network/API errors preserve selections so the user can retry. Searches show at
most the first 20 matches and ask the user to refine broader queries.

Roster scope is always supplied on evaluation. Outgoing selection searches only
the selected roster. New empty accounts can create a roster and add real players
without leaving this workflow. Refresh rosters recovers from external membership
changes and clears stale selections. Signup, roster-player removal, and full trade
history browsing remain in the original app.

## Verified October 17, 2026

- 10 focused tests passed across three files: complete component interaction,
  duplicate/overlap prevention, scoped payloads, removal/result invalidation,
  pending/validation/retry states, expiry cleanup, roster switching/setup,
  slow-search response ordering, empty/error search, and actual shared-client
  JWT/401 behavior. Test doubles exist only in test files.
- Strict TypeScript checking, including Vite config, and production build passed.
- Existing backend Surefire: 31 passed, 0 skipped. Failsafe: 4 passed, 1 skipped;
  the sole skip is the optional benchmark. No PostgreSQL correctness tests skipped.
  No optional benchmark was rerun.
- Browser tested Vite and the static production build against local PostgreSQL:
  demo login → create roster → add Josh Hart → select Hart outgoing/Jokic incoming
  → remove/reselect → validate empty side → prevent duplicate/overlap → evaluate.
  The backend returned score 100, nine category rows, model `heuristic-v2`.
  Production evaluation #2 was independently retrieved from persisted history.
- Real network failure was tested by stopping only this local backend; the page
  displayed an error with Retry search. Real session expiry was tested with a
  temporary 1,000 ms JWT lifetime; submitting after expiry returned to sign-in
  and removed selections. Normal 86,400,000 ms lifetime was restored afterward.
- Desktop 1280×900 and mobile 390×844 layouts were checked. At mobile size the
  document did not overflow horizontally; the category table scrolls internally.
- Screenshots capture empty, successful desktop/mobile, network-error, and
  expired-session states. Browser viewport overrides were reset after capture.

The original app's login page and the original → React → original navigation
links were also checked. The final preview was reauthenticated and evaluated
successfully after restoring the normal token lifetime.

Publication screenshots exclude account and roster UI:
[player selection](../images/react-selection-public.jpg) and
[category results](../images/react-categories-public.jpg). Historical full-page
desktop/mobile screenshots remain preserved as ignored local evidence.

Not verified: deployed origins, Android/native WebView behavior, all legacy app
screens, or execution of the new CI job on GitHub. Existing Capacitor packages
remain at 8.4.1; `npm audit` reports four inherited mobile/CLI dependency findings
(one critical, three high). They were not silently fixed through unrelated native
upgrades. The newly added frontend toolchain's reported findings were resolved
by selecting patched Vite/Vitest versions. See the saved audit evidence before
any later mobile release.

## Interview explanation

"I added a single React/TypeScript trade workflow alongside the existing app,
with a separate build output so the original JavaScript and Capacitor entry point
stayed intact. I reused the backend API client and JWT handling, and typed its
contracts instead of duplicating authentication or score calculations. I kept
roster ownership requirements, handled stale async search responses, invalidated
results when selections changed, and tested both component behavior and the
actual client payloads. Then I ran the development and production bundles against
local PostgreSQL, checked persisted evaluation results, and exercised network
failure and a genuinely expired JWT in the browser."

## Handoff: files changed in this milestone

New: `frontend/` source/components, types, tests, strict TypeScript/Vite config,
local PostgreSQL compose file, and this guide; `.github/workflows/frontend-ci.yml`.

Updated: root `package.json`/lockfile (React/tooling and scripts), `.gitignore`
(generated output/test temp), root README (entry/setup links), and
`www/index.html` (one comparison link). Capacitor configuration and the existing
API client/app scripts are unchanged. No backend Java source/test or prior
benchmark evidence was modified. No patches were reapplied. No commit, push,
deployment, cloud migration, or additional performance experiment was performed.
