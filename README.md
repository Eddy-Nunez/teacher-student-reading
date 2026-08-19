# Teacher Reading Assignment Portal

A lightweight end-to-end web app for the Scholastic coding challenge: teachers assign books to
students, students read and track their minutes, and teachers monitor progress.

**Live URL:** _(to be filled after deployment)_
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
  protection for state-changing requests, and role-based access control on every endpoint.
  control separating teacher and student endpoints.
- **Persistence:** SQLite-compatible **H2** database (file-backed locally), auto-seeded with 3
  demo books and 4 users on first boot.

## Tech stack

| Layer | Choice | Why |
|-------|--------|-----|
| Backend | **Java 17, Spring Boot 3.5**, Spring Data JPA | Scholastic's primary stack; fast, opinionated scaffolding |
| Auth | Spring Security + JWT in HttpOnly cookie + CSRF | Stateless, XSS-resistant, CSRF-protected |
| DB | H2 (file mode) | Zero-config local persistence, matches "all local" constraint |
| Frontend | **React 19** (Vite), React Router, Axios | Scholastic's frontend stack; Vite for fast dev/build |
| Styling | **Bootstrap 5** + small brand CSS | Industry-standard React styling; fast and consistent |
| Tests | JUnit 5 + MockMvc integration tests | Real HTTP + JWT + role-guard coverage through the full pipeline |

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
cd backend  && mvn test         # 9 API integration tests (auth/JWT/RBAC, CSRF session, monotonic minutes)
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
- Updates the student's own progress. Minutes are **monotonic** (`max(stored, incoming)`).
- **Request:** `{ "status"?: enum, "elapsedMinutes"?: number }` — either field optional.
- **200** → updated student-assignment object (same shape as the list item). `404` not assigned.

---

**Behavioral notes**
- `elapsedMinutes` is a maximum merge, so a stale/out-of-order client write can never reduce a student's total.
- Opening the reader (client-side) immediately promotes `NOT_STARTED → IN_PROGRESS`; the server's `GET .../{id}`
  itself performs **no** state change (mutations are explicit PUTs only).

## Key architectural decisions (summary — full reasoning in `decision-rationale.md`)

1. **JWT in an HttpOnly cookie + method security** instead of server-side sessions — stateless,
   XSS-resistant (JS cannot read the token), SameSite=Lax + double-submit CSRF protection;
   tradeoff: no server-side revocation (acceptable for the prototype; refresh rotation listed below).
2. **Assignments auto-assign to every student** — no classroom/roster management; the prompt's
   "for student(s)" was interpreted as "all enrolled students" to keep scope lean.
3. **Book catalog as first-class entity** — the prompt requires "a list of books to assign";
   assignments reference a book + due date rather than inlining a URL.
4. **Client-side reading timer with periodic server sync** — avoids spamming the API every
   second, survives tab closes via localStorage, and the server keeps minutes **monotonic** to
   be safe against out-of-order writes.
5. **Bootstrap 5 for UI** — enterprise-standard styling framework; fast, consistent, familiar to
   graders; custom CSS limited to brand accents.

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
