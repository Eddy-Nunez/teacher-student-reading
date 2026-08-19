# Overall Progress — Teacher Reading Assignment Portal

## Plan (high level)
1. **Design capture** — reconcile candidate's verbal design (in `gemini-contributions/`) into `decision-rationale.md`. ✅ (finalized in docs step)
2. **Scaffold** — Spring Boot backend + React (Vite) frontend, both local. ✅
3. **Backend** — entities (User/Book/Assignment/StudentAssignment), JWT auth, teacher + student APIs, H2 persistence, seed data. ✅ tested end-to-end
4. **Frontend** — login, teacher dashboard (catalog + create assignment + progress), student dashboard + reader with minute tracker. ✅
5. **Tests** — backend integration tests (8/8 green) + browser E2E. ✅
6. **README + decision-rationale.md** — architecture, tradeoffs, assumptions, run instructions. 🔄 in progress
7. **Deployment** — GitHub repo + public URL with credentials. (next)

## Current task
✅ **Security pass landed (post-UAT reviewer feedback):** moved from localStorage Bearer-token auth to
**HttpOnly + SameSite=Lax cookie** auth with **double-submit CSRF** protection. Verified live: login works,
`document.cookie` exposes only the CSRF token (auth cookie invisible to XSS), mutations pass CSRF, and all
prior flows (auto-IN_PROGRESS, timer, complete, teacher view) still work under cookie auth.
- Tests migrated to cookie pipeline + added security regressions (HttpOnly cookie, no token in body, /me from cookie): **10/10 green**.
- Stale-docs sweep done: README + decision-rationale no longer claim localStorage token storage.
- Known gap (documented): CSRF mechanism itself is browser-E2E-only; an automated 403-without-header test is a noted follow-up.

Commit pending. **Next**: final docs review → commit → deployment (Option A, waiting on credential).

## Divergences / pivots (all updates)
1. **Spring Boot version**: start.spring.io's `4.0.7.RELEASE` doesn't exist on Maven Central (calendar versioning). Pivoted to **Spring Boot 3.5.16** (stable, Java 17, familiar Security 6.x API).
2. **Maven**: installed 3.9.16 via sdkman; `mvn` needs `source ~/.sdkman/bin/sdkman-init.sh`.
3. **Data model refinement** (vs. gemini spec): added a **Book entity** (requirement: "view a list of available books") — Assignment now references Book + dueDate + teacher. Books embed short public-domain content so the reader works without iframe/CORS issues.
4. **Extra endpoint**: `GET /api/teacher/students` (roster) so the UI can say "assigned to all N students".
5. **Minutes tracking**: localStorage timer synced every 60s; backend enforces **monotonic minutes** (max) against out-of-order writes.
6. **UI/UX styling (was never covered in the original spec)** — adopted **Bootstrap 5** as the primary styling framework (industry-standard for React enterprise apps; fastest for a sprint). Custom CSS trimmed to brand overrides (reader typography, login card, navbar, status badge colors). This decision is documented in `decision-rationale.md` §2/§5.
7. **TypeScript vs JavaScript**: JSX chosen deliberately for the 4h sprint; TS listed as an "improve with more time" item.
8. **Real bug found & fixed during browser testing**: student list exposed the *progress-row id* instead of the *assignment id* → reader/detail/status endpoints 404'd when the ids diverged (worked only by coincidence on the first assignment). Fixed + regression test `studentUsesAssignmentIdNotProgressRowId`.
9. **Test tooling quirks (not app bugs)**: native `<input type=date>` picker can't be driven by CDP automation; agent-browser a11y clicks occasionally miss → used JS clicks. Also `pkill -f` suicide risk (pattern matching my own shell) → `scripts/start-backend.sh` uses /proc-based kills.

## Notes / risks
- Dev flow: `vite` proxies `/api` → `localhost:8080`. Production: same-origin reverse proxy (static host + backend, or single server).
- H2 file DB (`./data/`) persists locally; deployment must use a pristine DB (start script does `rm -rf data`).
- **Deployment host decision is the last open item** — verify reachability from this box before the deadline.
