# Overall Progress — Teacher Reading Assignment Portal

## Delivery checklist (what's left)

- [x] Design capture + decisions documented (`decision-rationale.md`)
- [x] Backend (Spring Boot 3.5 / Java 17): JWT cookie auth + CSRF, RBAC, H2, seed data
- [x] Frontend (React 19 / Vite / Bootstrap 5): login, teacher + student dashboards, reader + timer
- [x] Tests — **11/11 green** (auth/JWT/RBAC, CSRF session, monotonic minutes, concurrency regression)
- [x] Security pass — HttpOnly cookie, SameSite=Lax, double-submit CSRF, Secure-in-prod
- [x] Data integrity — `@Transactional` create + pessimistic-lock status update
- [x] UAT feedback — auto-IN_PROGRESS on reader open, teacher dashboard interaction-refresh + 15s poll
- [x] README — Quick Start, OpenAPI-style API reference, versions rationale, tradeoffs, UAT log
- [x] Git repo committed (local) — 6 commits
- [x] Deployment — live on Render (free): https://teacher-student-reading.onrender.com
- [x] README live URL filled + deployment caveats documented
- [x] **Post-deploy validation:** API smoke test + browser smoke test (login → teacher dash → assign → student reader → complete → teacher view) all green
- [x] Post-validation polish: one-click demo-account login (teacher + each student); documented 2 added assumptions
  (no caching — no scale/latency spec; demo-UX defaults via build + UAT)
- [x] Final review pass of `decision-rationale.md` + README before sign-off
- [x] Security hardening (post-launch review): H2 console gated OFF in prod (`H2_CONSOLE_ENABLED=false`);
  X-Frame-Options restored; strict allow-list CSP + Permissions-Policy + Referrer-Policy; CORS
  deny-by-default (no wildcard). **Deployed live (`5c4a17a`)** — all 6 headers verified on SPA + API,
  SPA renders + full login works under the policy.

## Workflow lesson (recorded for future sessions)
- **Always validate locally BEFORE anything that triggers a deploy.** For this stack:
  local backend (jar on :8080) + Vite dev frontend (:5173, serves source + proxies /api) is a fast
  validate-in-a-minute loop; run the browser E2E there first, then commit/push to trigger Render.
- **Never deploy to the public URL without the user's explicit go-ahead** (learned the hard way twice).
  Local validation + commit are mine to do; firing the Render deploy is a user-approved action.

## Open items user can still pick up

1. Optional: CSRF-mechanism automated test (missing X-XSRF-TOKEN → 403), currently browser-E2E only.
2. Deployment credential (the only hard blocker).

## Decisions/doc deltas worth remembering for the write-up

- Versions: Java 17 / Spring Boot 3.5.16 / React 19 / Node 22 LTS (rationale in README "Why these versions").
- Locking: pessimistic for the status PUT (workload: ~1 write/s, same-user idempotent rows; both approaches valid — rationale D7).
- Version-column discussion (byte vs int vs bigint, indexing) — relevant interview talking point, not applied (no optimistic lock in code).