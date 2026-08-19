# Decision Rationale — Teacher Reading Assignment Portal

> The written test of the challenge. This document records what was decided, **why**, what was
> rejected, and what would be improved with more time. It was updated **as decisions were made**
> during the build session, not reconstructed afterward.

---

## 1. Requirements interpretation & assumptions

The prompt is intentionally high-level: "a lightweight, end-to-end web application that lets teachers
assign reading to students and track assignment status." Grading is on interpretation, tradeoffs,
and communication — not on feature count.

**Assumptions made:**

1. **Identity/auth** — "Real authentication is preferred but scope should be kept reasonable."
   → Username/password + **stateless JWT** (BCrypt-hashed passwords). Registration was **cut**:
   users are seeded (1 teacher, 3 students) because user-management is not a stated requirement and
   would consume a meaningful slice of a 4-hour budget. Real OAuth/SSO was rejected (§3).
2. **"for student(s)"** — interpreted as **assign to all enrolled students**. Rejected: classroom
   grouping / roster selection / multi-tenancy. The prompt describes only one role (student) and no
   classroom concept; a Roster entity + join tables + selection UI would add scope with no
   requirement pointing at it. The teacher UI communicates this explicitly ("Assigned to all 3
   students") so the behavior is a feature, not a surprise.
3. **Books** — "a list of books should be available to assign" → modeled a **first-class Book
   catalog** (title, author, description, embedded content, optional source URL); assignments
   reference a book + due date. Rejected: inlining a `referenceUrl` directly on the assignment
   (the original design sketch) — that fails the "list of available books" requirement. Books embed
   short public-domain excerpts so the reader works without depending on external sites (many
   publishers set `X-Frame-Options`/CSP that would have silently broken an iframe demo).
4. **Minutes read** — tracked **client-side** during a session (localStorage) and **synced to the
   server** every 60 s and on status change. Server stores `MAX(stored, incoming)` so out-of-order
   or stale writes can never regress a student's minutes. Rationale: per-second HTTP sync is
   needlessly chatty; localStorage survives accidental tab closes; monotonic server-side storage
   keeps the server authoritative without requiring real-time streams.
5. **Statuses** — exactly the three the prompt defines: `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`.
   No extra states (e.g., OVERDUE is derived from due date in the UI layer, not stored).
6. **UI styling** — the prompt says nothing about a design system, so the decision had to be made
   from scratch. Chosen: **Bootstrap 5** as the base framework with a small brand-override
   stylesheet. Bootstrap is the de-facto standard for enterprise React apps (Scholastic context),
   is fast to apply in a sprint, and makes the UI readable to any grader. Custom hand-rolled CSS
   was rejected: slower, less consistent, and a worse signal for a Java/React senior-role screen.

## 2. Architecture

```
                 ┌────────────────┐          ┌──────────────────────┐
   Browser ─────▶ │  React 19 SPA  │   /api   │  Spring Boot 3.5 API  │ ──▶ H2 (file)
   (public URL)  │   (Vite)       │ ───────▶ │  Java 17, Security    │        │
                 └────────────────┘          └──────────────────────┘     seeded
                      dev: Vite proxy /api → :8080                        demo data
```

**Why this structure:**

- **Two clear seams** (SPA ↔ JSON API ↔ DB) match how Scholastic runs Java+React and make the
  follow-up conversation easy: where would services split, how does auth scale, what breaks.
- **Stateless API (JWT)** — any instance can serve any request; no session stickiness; easy to
  scale or move behind a load balancer later.
- **DTOs as records, controllers thin, logic in the controller layer for CRUD-scale endpoints** —
  a service layer was judged unnecessary at this size; the code stays readable and honest about
  complexity (no fake "enterprise" layering).

### Backend packages

```
com.scholastic.portal
├── PortalApplication.java
├── config/       SecurityConfig, DataSeeder
├── security/     JwtService, JwtAuthFilter, AppPrincipal
├── model/        User, Role, Book, Assignment, StudentAssignment, AssignmentStatus
├── repository/   Spring Data JPA repositories
├── dto/          request/response records
└── controller/   AuthController, TeacherController, StudentController
```

### Data model

- **app_user** (id, username unique, password BCrypt, display_name, role)
- **book** (id, title, author, description, content, reference_url)
- **assignment** (id, book_id FK, teacher_id FK, due_date, created_at)
- **student_assignment_progress** (id, student_id FK, assignment_id FK,
  `UNIQUE(student_id, assignment_id)`, status enum, elapsed_minutes)

One progress row per (student, assignment) pair; created eagerly when the teacher assigns.

## 3. Key decisions & tradeoffs

### D1. Spring Boot version pinning
The local toolchain's start.spring.io now defaults to Spring Boot 4.x, whose generated
`4.0.7.RELEASE` artifact **does not exist on Maven Central** (it moved to calendar versions
`4.0.x`). Pinned **Spring Boot 3.5.16** (stable LTS-track) + Java 17 — matches the team's likely
baseline, avoids bleeding-edge migration churn, and works with the familiar Spring Security 6 DSL.

### D2. JWT auth (stateless) over sessions / OAuth / WebAuthn
- **Accepted tradeoff:** no server-side revocation; a leaked token lives until expiry (24 h). Fine
  for a prototype; listed in §4 for production hardening.
- **Rejected:** server-side sessions (adds state, breaks horizontal scaling, more moving parts),
  OAuth2/SSO and WebAuthn/Passkeys (disproportionate setup — provider accounts, challenge-response
  flows — for a demo with no org context).
- **Token storage** is localStorage + Axios interceptor for speed. Known XSS risk, mitigated by
  React's default escaping; httpOnly-cookie storage noted as the production upgrade.

### D3. Auto-assign to all students
Covered in §1.2. The flip side is worth stating: there is **no way to assign a book to a subset of
students yet**. That is a deliberate scope cut, not an oversight, and the schema supports adding a
roster later without migration pain (progress rows are per student/assignment).

### D4. Reading timer: client-local + periodic sync, monotonic server storage
- **Why not server-side ticking:** doubles API traffic, needs heartbeat/keepalive handling, and
  provides no real benefit at demo scale; students also read while offline-ish (flaky school Wi-Fi).
- **Why not pure client-side (never sync):** the teacher requirement is *visibility into progress*,
  so minutes must reach the server.
- **Monotonic guard** (`MAX(stored, incoming)`): cheap, deterministic, and prevents a stale device
  or retry from "un-reading" a student. The sync is a PUT of the full accumulated value, idempotent.

### D5. Bootstrap 5 + brand CSS
Chosen over custom CSS (slow, inconsistent) and over Tailwind (excellent but more tooling; Bootstrap
is the more common enterprise default and reads as "boring and solid" to an interviewer). Custom CSS
is limited to brand accents: reader typography, login card, navbar, status-badge colors. Verified the
Bootstrap bundle actually loads (computed styles check) after wiring it in.

### D6. Also considered and rejected

| Option | Decision | Why rejected |
|--------|----------|--------------|
| OAuth2 / SSO / WebAuthn | reject | Setup cost >> value for a demo with no external identity provider |
| Server-side sessions | reject | Statelessness chosen; see D2 |
| PostgreSQL / JDBC (SQLite) | reject for now | "All supporting services local" constraint → H2 file mode; swap documented as easy (§4) |
| TypeScript | reject for now | JSX chosen to keep the sprint tight; TS is the natural next step |
| Tailwind | reject | Bootstrap chosen as the more standard enterprise default (see D5) |
| Admin role / user registration UI | reject | Not in the prompt; seed data covers demo needs |
| Pagination / search / filters | reject | Explicitly out of scope in the design notes (small class sizes) |
| PWA / offline reader | reject | Service-worker complexity without a requirement |

## 4. Testing approach & quality

- **8 MockMvc integration tests** run the *full stack* against a seeded in-memory H2: login success
  and failure, catalog listing, assignment creation with auto-enrollment, student list/detail,
  status + minutes update, **monotonic minutes** behavior, teacher progress view, and role guards
  (403s both directions, 401 for anonymous).
- A **regression test** (`studentUsesAssignmentIdNotProgressRowId`) pins a real bug found during
  browser E2E: the student API exposed the *progress-row id* instead of the *assignment id*, which
  404'd the reader when the ids diverged (they only coincided for the first assignment). This is
  exactly the kind of data-model/API-contract mismatch integration tests exist to catch.
- **Frontend E2E (manual, agent-browser):** teacher login → create assignment → student login →
  open reader → timer ticked 66 s → server persisted IN_PROGRESS/1 min via the 60 s sync → mark
  complete → teacher dashboard shows the completed student with minutes. Also verified the built
  bundle (`npm run build`) and that Bootstrap CSS actually loaded.
- **Honest gaps:** no frontend unit tests (timebox), no load testing, no contract tests beyond
  MockMvc. Frontend testing is a §4 improvement.

## 5. Tradeoffs & assumptions (summary)

| Area | Chosen | Tradeoff accepted |
|------|--------|-------------------|
| Auth | JWT + seeded users | No registration/revocation; localStorage token storage |
| Assignment target | All students | No subset selection (documented UX copy) |
| Books | Embedded excerpts + catalog | Content is curated excerpts, not full novels |
| Timer | Client tick → 60 s server sync | Server value can lag up to a minute; offline minutes merge on next sync |
| Styling | Bootstrap 5 | Larger CSS bundle; limited custom brand identity |
| Persistence | H2 file | Single-writer file DB; fine at demo scale |
| Frontend | JavaScript (JSX) | No static typing during the sprint |

## 6. With more time…

1. **Auth hardening:** registration, OAuth/SSO option, refresh-token rotation, httpOnly-cookie
   storage, logout/revocation.
2. **Rosters/classes:** assign books to selected students; teacher-managed student accounts.
3. **Real database:** SQLite (matches the stated constraint) or Postgres + Flyway migrations, both
   via a `DataSource` swap; H2's dialect already keeps SQL generic.
4. **Frontend quality:** TypeScript, component tests (Vitest + RTL), E2E (Playwright), error
   boundaries, skeletons/loading states.
5. **Operational:** CI/CD (GitHub Actions: build → test → deploy), structured logging + metrics,
   container image, health/liveness endpoints, DB backup, rate limiting on login.
6. **Product:** pagination/search, book covers + blurbs, due-date reminders, multi-device reading
   sessions (server-authoritative minutes), teacher analytics (avg minutes, completion rates).

## 7. Follow-up conversation starters

- How I'd split this into a team-sized codebase (contracts, service layer, events for progress
  updates, observability).
- How minutes tracking should work at scale (server-authoritative sessions vs. client sync).
- Where the security model needs to evolve for real classrooms (multi-tenant schools, teacher roles
  beyond one, compliance basics).
