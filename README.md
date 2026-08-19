# Teacher Reading Assignment Portal

A lightweight end-to-end web app for the Scholastic coding challenge: teachers assign books to
students, students read and track their minutes, and teachers monitor progress.

**Reading guide** (graded-requirement map):
- *What was implemented* → **What was built**
- *Key architectural decisions* → **Key architectural decisions** (+ **Why these versions**)
- *Tradeoffs and assumptions* → **Tradeoffs & assumptions (explicit)**
- *What you'd improve with more time* → **What would improve with more time**
- *How the design evolved with review input* → **Reviewer feedback & resolutions (UAT log)**

**Live URL:** **https://teacher-student-reading.onrender.com**
**Demo credentials:**

| Role | Username | Password |
|------|----------|----------|
| Teacher | `teacher` | `password` |
| Student | `student1` / `student2` / `student3` | `password` |

---

## What was built

- **Teacher flow:** browse a book catalog, create a reading assignment (book + due date) that is
  automatically assigned to every student, and view per-student status (`NOT_STARTED`,
  `IN_PROGRESS`, `COMPLETED`) plus minutes read for any assignment.
- **Student flow:** see assigned readings, open the reader view, track reading time with a
  session timer (localStorage-backed, synced to the server), update status, and mark readings
  completed.
- **Auth:** username/password with stateless **JWT** (BCrypt-hashed passwords) delivered in an
  **HttpOnly, SameSite=Lax cookie** (never exposed to JS/XSS), with **double-submit CSRF**
  protection for state-changing requests, and role-based access control separating teacher and
  student endpoints.
- **Persistence:** SQLite-compatible **H2** database (file-backed locally), auto-seeded with 3
  demo books and 4 users on first boot.
- **UX refinements from review:** opening the reader immediately marks it *In progress*; the
  teacher dashboard refreshes student data on interaction and auto-polls every 15 s.

## Tech stack

| Layer | Choice | Why |
|-------|--------|-----|
| Backend | **Java 17, Spring Boot 3.5**, Spring Data JPA | Scholastic's primary stack; fast, opinionated scaffolding |
| Auth | Spring Security + JWT in HttpOnly cookie + CSRF | Stateless, XSS-resistant, CSRF-protected |
| DB | H2 (file mode) | Zero-config local persistence, matches "all local" constraint |
| Frontend | **React 19** (Vite), React Router, Axios | Scholastic's frontend stack; Vite for fast dev/build |
| Styling | **Bootstrap 5** + small brand CSS | Industry-standard React styling; fast and consistent |
| Tests | JUnit 5 + MockMvc integration tests | Real HTTP + cookie session + role-guard coverage through the full pipeline |

### Why these versions (not "latest" everywhere)

- **Java 17** (not 21/25): current enterprise baseline — most Java shops, including the likely
  baseline at Scholastic, run 17; fully supported by Spring Boot 3.5; records/enums (already used)
  are 17 features. Newer LTS features (virtual threads, pattern matching) buy little at this scale.
- **Spring Boot 3.5** (not 4.x): the local toolchain's start.spring.io default (`4.0.7.RELEASE`)
  does not exist on Maven Central (it switched to calendar versions); 3.5.x is a maintained stable
  line with the Spring Security 6 DSL used here. A 4.x upgrade is a reasonable follow-up.
- **React 19 / Vite 8** — current majors (no downgrade). **Node 18+** is the minimum floor; the
  build was verified on Node 22 LTS.

## Repository layout

```
.
├── backend/                 # Spring Boot application (Maven)
│   ├── src/main/java/com/scholastic/portal/
│   │   ├── config/          # Security config, data seeder
│   │   ├── controller/      # Auth, Teacher, Student REST controllers
│   │   ├── dto/             # Request/response records
│   │   ├── model/           # User, Book, Assignment, StudentAssignment
│   │   ├── repository/      # Spring Data JPA repositories
│   │   └── security/        # JWT service + filter, principal
│   └── src/test/            # API integration tests (MockMvc)
├── frontend/                # React (Vite) SPA
│   └── src/
│       ├── api/client.js    # Axios: cookie session, CSRF header, 401 handling
│       ├── auth/            # AuthContext (session state)
│       ├── components/      # Navbar, ProtectedRoute, StatusBadge
│       └── pages/           # Login, TeacherDashboard, StudentDashboard, Reader
├── scripts/start-backend.sh # Local backend start/restart helper
├── decision-rationale.md    # THE write-up: decisions, tradeoffs, assumptions
└── requirements.md          # Verbatim assessment prompt
```

---

## Live deployment (Render)

The full stack (React SPA + Spring API in ONE Docker process, single origin) is deployed and can be
reached at **`https://teacher-student-reading.onrender.com`**. Any browser tab there exercises the
same auth, CSRF, and RBAC behavior as local — no CORS, single origin.

**How it was built:** `render.yaml` Blueprint (Docker) → the `Dockerfile` builds the SPA, bakes it into
the Spring fat jar, and runs one JVM on Render's injected `$PORT`.

**Validated end-to-end on the live URL (smoke test):**

| Check | Result |
|-------|--------|
| Health probe `GET /api/auth/csrf` | `204` ✅ |
| Teacher login (`teacher/password`) | `200`, JWT cookie set ✅ |
| Wrong password rejected | `401` ✅ |
| Create assignment (book + due date → all students) | `200`, persisted ✅ |
| Student opens reader → auto `IN_PROGRESS` | confirmed back in teacher view ✅ |
| Status transitions `NOT_STARTED → IN_PROGRESS → COMPLETED` + monotonic minutes | ✅ |
| Teacher dashboard reflects per-student progress | ✅ |
| RBAC: unauthenticated `/api/student/assignments` | `401` ✅ |
| SPA deep links (`/login`, `/student/assignments/1`) | serve the app ✅ |

**Known production caveats (free tier)** — the tradeoff for a zero-cost always-public URL:
1. **Ephemeral data on restart.** Render's free containers have no persistent disk, so `./data/portal`
   (H2 file DB) starts fresh after every recycle/cold start (free tier also sleeps after ~15 min idle).
   The **idempotent seeder restores the demo users + books**, and any data created in the *current*
   running instance is served correctly — but data from a prior instance is not carried over. For
   durable data you'd point the datasource at hosted Postgres/SQLite or Render's paid persistent disk.
2. **Cold start is slow (~2–3 min on free CPU)** the first time after sleep: Docker + JVM + Hibernate
   on a free core, not an app error. Click, wait ~15 s, then refresh once it's up.

The local Quick Start below is unaffected by either caveat.

## Quick Start — local testing on a fresh environment

Everything runs locally; no accounts or external services needed. Linux/macOS/Windows-WSL2 all work.

### 1. Prerequisites (install once)

| Tool | Version | Install hint |
|------|---------|--------------|
| Java | 17+ | `sdk install java`, `brew install openjdk@17`, or `apt install openjdk-17-jdk` |
| Maven | 3.9+ | `sdk install maven` or `brew install maven` |
| Node.js | 18+ | `nvm install 18` or official installer |

> **Windows / WSL2:** install the tools inside WSL. The Vite dev server proxies `/api` to the backend, so
> the browser only ever talks to one origin — no CORS setup and cookie/CSRF auth behaves normally. From a
> Windows browser, open `http://localhost:5173` (WSL2 forwards localhost automatically).

### 2. Start the backend (http://localhost:8080)

```bash
cd backend
mvn spring-boot:run
```

On first boot the app creates its H2 database at `backend/data/` and **auto-seeds** a book catalog and the
demo users (idempotent — nothing to configure).

### 3. Start the frontend (http://localhost:5173)

```bash
cd frontend
npm install
npm run dev
```

### 4. Verify the app works

1. Open http://localhost:5173 and sign in as **`teacher` / `password`**.
2. Create a reading assignment (pick a book + due date) — it is assigned to all students.
3. Sign out, sign in as **`student1` / `password`** — open the reader → status flips to *In progress*, minutes
   tick up → **Mark as completed**.
4. Sign back in as **`teacher`** — the assignment now shows that student's updated status/minutes (auto-refresh).

### 5. Run the tests + build

```bash
cd backend  && mvn test         # 10 API integration tests (auth/JWT/RBAC, CSRF session, monotonic + concurrency)
cd frontend && npm run build    # production bundle → frontend/dist
```

### Troubleshooting (local)

- **Port in use** — start the backend with `--server.port=8081` and update the Vite proxy target in
  `frontend/vite.config.js` to match.
- **Cookies / CSRF on localhost** — dev uses an HttpOnly cookie with the `Secure` flag **off** (production
  turns it on via `COOKIE_SECURE`). SameSite=Lax and the double-submit CSRF cookie behave normally locally.
- **WSL2 browser can't connect** — if localhost forwarding is off, run Vite with `--host 0.0.0.0` and open the
  WSL IP directly.
- **Stale demo data** — delete `backend/data/` and restart to reseed a pristine state.

## API reference (OpenAPI style)

Base URL: `/api` (dev: proxied by Vite at `:5173`; backend at `:8080`). All paths below are relative to it.

**Auth model.** `POST /api/auth/login` issues the JWT in an **HttpOnly, SameSite=Lax** cookie (`portal_token`).
Every state-changing request (POST, PUT, DELETE) must also send the double-submit **`X-XSRF-TOKEN`** header
matching the `XSRF-TOKEN` cookie (Axios injects it automatically; the SPA bootstraps the cookie via
`GET /api/auth/csrf`). Endpoints are guarded by role — the `TEACHER`/`STUDENT` columns below are requirements.

**Common error codes:** `401` unauthenticated / invalid session · `403` wrong role or missing CSRF token ·
`400` malformed request · `404` resource not found.

---

### Authentication (no role)

#### `POST /api/auth/login`
- **Request:** `{ "username": string, "password": string }`
- **200** → `{ "userId": number, "username": string, "displayName": string, "role": "TEACHER" \| "STUDENT" }`
  plus `Set-Cookie: portal_token=<jwt>; HttpOnly; SameSite=Lax; Path=/`.
- `401` invalid credentials · `400` missing username/password.

#### `GET /api/auth/me`
- Resolves the current session from the cookie (used by the SPA on boot).
- **200** → same shape as login. `401` no session.

#### `POST /api/auth/logout`
- Clears the session (sets an expired cookie). **200** empty body.

#### `GET /api/auth/csrf`
- Bootstraps the `XSRF-TOKEN` cookie (readable by JS). **204** empty body.

---

### Teacher endpoints (`TEACHER`)

#### `GET /api/teacher/books`
- **200** → `[ { "id": number, "title": string, "author": string, "description": string\|null, "referenceUrl": string\|null } ]`

#### `GET /api/teacher/students`
- **200** → `[ { "id": number, "name": string } ]` (enrolled roster).

#### `POST /api/teacher/assignments` *(CSRF)*
- Creates an assignment and auto-assigns to every student (each progress row starts `NOT_STARTED`).
- **Request:** `{ "bookId": number, "dueDate": "YYYY-MM-DD" }`
- **200** → `AssignmentSummary` (shape below).
- `404` unknown book · `400` missing/invalid `dueDate`.

#### `GET /api/teacher/assignments`
- **200** → `[ AssignmentSummary ]` for the authenticated teacher, newest first.

#### `GET /api/teacher/assignments/{id}/progress`
- Per-student progress for one of the teacher's assignments.
- **200** → `AssignmentSummary`. `404` not found / not owned by this teacher.

**`AssignmentSummary` shape** (used above):
```jsonc
{
  "id": 1,
  "bookTitle": "The Great Gatsby",
  "bookAuthor": "F. Scott Fitzgerald",
  "dueDate": "2026-09-30",
  "assignedStudentsCount": 3,
  "completedCount": 1,
  "inProgressCount": 1,
  "notStartedCount": 1,
  "studentProgress": [
    { "studentId": 2, "studentName": "Ava", "status": "IN_PROGRESS", "elapsedMinutes": 7 }
  ]
}
// status ∈ NOT_STARTED | IN_PROGRESS | COMPLETED
```

---

### Student endpoints (`STUDENT`)

#### `GET /api/student/assignments`
- Assignments assigned to the logged-in student, newest first.
- **200** → `[ { "id": number, "bookId": number, "bookTitle": string, "bookAuthor": string, "dueDate": string, "status": enum, "elapsedMinutes": number } ]`
  (`id` is the **assignment** id and is safe to use in the detail/status URLs below.)

#### `GET /api/student/assignments/{id}`
- Reading detail for the reader view (uses the assignment id).
- **200** → `{ "id", "bookId", "bookTitle", "bookAuthor", "description", "content", "referenceUrl", "dueDate", "status", "elapsedMinutes" }`
- `404` not assigned to this student.

#### `PUT /api/student/assignments/{id}/status` *(CSRF)*
- Updates the student's own progress. Minutes are **monotonic** (`max(stored, incoming)`), merged
  under a **pessimistic row lock** so concurrent writers cannot lose an update.
- **Request:** `{ "status"?: enum, "elapsedMinutes"?: number }` — either field optional.
- **200** → updated student-assignment object (same shape as the list item). `404` not assigned.

---

**Behavioral notes**
- `elapsedMinutes` is a maximum merge, so a stale/out-of-order client write can never reduce a student's total.
- Opening the reader (client-side) immediately promotes `NOT_STARTED → IN_PROGRESS`; the server's `GET .../{id}`
  itself performs **no** state change (mutations are explicit PUTs only).

## Key architectural decisions (summary — full reasoning in `decision-rationale.md`)

> Decisions marked **👤** were driven by reviewer feedback during the review conversation —
> a record of how the design evolved with input, not just a static spec.

1. **JWT in an HttpOnly cookie + method security** instead of server-side sessions — stateless,
   XSS-resistant (JS cannot read the token), SameSite=Lax + double-submit CSRF protection;
   tradeoff: no server-side revocation (acceptable for the prototype; refresh rotation listed
   below). 👤 *(reviewer: "localStorage has a vulnerability; cookie should be used" — replaced
   the initial Bearer-token-in-localStorage approach; CSRF token follows naturally, see
   decision-rationale §3 D2.)*
2. **Assignments auto-assign to every student** — no classroom/roster management; the prompt's
   "for student(s)" was interpreted as "all enrolled students" to keep scope lean.
   👤 *(reviewer: "assign to all students is reasonable" — validated and kept.)*
3. **Book catalog as first-class entity** — the prompt requires "a list of books to assign";
   assignments reference a book + due date rather than inlining a URL.
4. **Client-side reading timer with periodic server sync** — avoids spamming the API every
   second, survives tab closes via localStorage, and the server keeps minutes **monotonic** to
   be safe against out-of-order writes.
5. **Open = started + live teacher view** — reviewer-backed UX: opening the reader immediately
   promotes `NOT_STARTED → IN_PROGRESS`, and the teacher dashboard refreshes on interaction +
   auto-poll. 👤 *(UAT log #2, #3.)*
6. **Bootstrap 5 for UI** — enterprise-standard styling framework; fast, consistent, familiar to
   graders; custom CSS limited to brand accents.

## Tradeoffs & assumptions (explicit)

Graded requirement: state assumptions and tradeoffs openly. Full prose in
`decision-rationale.md` §1 / §5 — highlights:

| Area | Choice | Tradeoff accepted |
|------|--------|-------------------|
| Auth | Seeded users, no registration | No self-service accounts; auth flow only |
| Auth | 24 h JWT, no revocation | Stolen token valid until expiry; refresh rotation is a follow-up |
| Auth storage | HttpOnly cookie + CSRF | More moving parts than localStorage; token invisible to JS (the point) |
| Assignment target | Auto-assign to all students | No per-student selection yet (schema supports adding rosters later) |
| Books | Embedded curated excerpts | Not full novels; no rich-text authoring; reader works offline/iframe-free |
| Timer | Client tick → 60 s server sync | Server value can lag ~1 min; multi-device sessions would need server-side timing |
| Persistence | H2 file mode | Single-writer DB; demo scale only — SQLite/PG swap documented |
| Atomicity | `@Transactional` create + pessimistic-lock status update | Per-row lock is a tiny serialization point; fine at this write volume |
| UX | Interaction refresh + 15 s poll | Polling instead of websockets/SSE (simpler, fine at this scale) |
| UI | Bootstrap 5 | Larger CSS bundle; limited custom brand identity |
| Language | JavaScript (JSX) | No static typing during the sprint; TS is the natural next step |
| Versions | Java 17 / Boot 3.5 / Node 18+ | Not the newest LTS in every slot — see "Why these versions" |

## Reviewer feedback & resolutions (UAT log)

Callouts made during the review conversation, with dispositions. This log is also useful as the
"how did this evolve" narrative in a follow-up interview.

| # | Reviewer callout | Disposition | Where |
|---|------------------|-------------|-------|
| 1 | "Assign to all students" is a reasonable model | ✅ Accepted as-is | Data model, auto-enrollment |
| 2 | Opening a reading should immediately show *In progress*, not *Not started* | ✅ Fixed — reader mount promotes `NOT_STARTED → IN_PROGRESS` (explicit PUT, idempotent, no GET side effect) | `ReaderPage`; verified E2E |
| 3 | Teacher dashboard should refresh student data on interaction (no manual reload) | ✅ Fixed — refetch on expand toggle + 15 s auto-poll | `TeacherDashboard` |
| 4 | localStorage JWT is an XSS-exfiltration vector; cookie auth should be used | ✅ Fixed — JWT moved to `HttpOnly, SameSite=Lax` cookie; `Secure` in prod | `AuthCookie`, `SecurityConfig`, `client.js` |
| 5 | A security pass should be an explicit part of the plan | ✅ Landed + tracked as a milestone; remaining hardening (refresh rotation, revocation) listed in "With more time" | `decision-rationale.md` §3 D2 / §6, `status.md` |
| 6 | Confirm tests were updated for the cookie refactor | ✅ Suite migrated to cookie pipeline + new security regressions (`HttpOnly`, no token in body, `/me` from cookie) — 10/10 green | `ApiIntegrationTest` |
| 7 | Role-guard coverage in tests | ✅ Covered — 403 both directions (student↔teacher) + 401 anonymous | `ApiIntegrationTest#roleGuardsAreEnforced` |
| 8 | Session/cookie lifetime? | ✅ Documented — JWT expiry default 24 h (`app.jwt.expiration-ms`), no server-side revocation (tradeoff) | `decision-rationale.md` §3 D2 |
| 9 | Why weren't latest Java/Spring Boot/Node used? | ✅ Documented — see "Why these versions" above | Tech stack section |
| 10 | Is the CSRF token natural for cookie auth? | ✅ Documented — yes; double-submit is the SPA-standard complement to cookie sessions (defense-in-depth beyond `SameSite=Lax`) | `decision-rationale.md` §3 D2, API reference |
| 11 | Windows/WSL2 local testing | ✅ Documented — WSL2 localhost forwarding + troubleshooting in Quick Start | Quick Start §1/§Troubleshooting |

## What would improve with more time

- Real registration + classroom/roster management (assign to selected students).
- SQLite via JDBC for the "SQLite" constraint / or Postgres with Flyway migrations.
- Refresh-token rotation + server-side revocation/deny-listing (httpOnly cookie storage and CSRF
  protection already landed).
- Automated test for the CSRF mechanism itself (missing X-XSRF-TOKEN header → 403) — currently
  covered by browser E2E only.
- Server-side session timers (students sometimes read on multiple devices).
- TypeScript, component library (MUI), Vitest + React Testing Library for frontend unit tests.
- Pagination, search/filtering, admin role, audit logs, and deployment via CI/CD.
