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
- **Auth:** username/password with stateless **JWT** (BCrypt-hashed passwords), role-based access
  control separating teacher and student endpoints.
- **Persistence:** SQLite-compatible **H2** database (file-backed locally), auto-seeded with 3
  demo books and 4 users on first boot.

## Tech stack

| Layer | Choice | Why |
|-------|--------|-----|
| Backend | **Java 17, Spring Boot 3.5**, Spring Data JPA | Scholastic's primary stack; fast, opinionated scaffolding |
| Auth | Spring Security + JWT (jjwt) | Stateless, zero server-side session state |
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
│       ├── api/client.js    # Axios instance (JWT interceptor, 401 handling)
│       ├── auth/            # AuthContext (session state)
│       ├── components/      # Navbar, ProtectedRoute, StatusBadge
│       └── pages/           # Login, TeacherDashboard, StudentDashboard, Reader
├── scripts/start-backend.sh # Local backend start/restart helper
├── decision-rationale.md    # THE write-up: decisions, tradeoffs, assumptions
└── requirements.md          # Verbatim assessment prompt
```

## Run locally

Prerequisites: Java 17+, Maven 3.9+, Node 18+.

```bash
# 1. Backend (http://localhost:8080)
cd backend
mvn spring-boot:run

# 2. Frontend (http://localhost:5173) — proxies /api to :8080 in dev
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 and sign in with any demo account above.

### Tests & build

```bash
cd backend && mvn test        # 8 integration tests: auth, roles, CRUD, monotonic minutes
cd frontend && npm run build  # production bundle in frontend/dist
```

## API summary

| Method | Endpoint | Role | Purpose |
|--------|----------|------|---------|
| POST | `/api/auth/login` | public | Exchange credentials for a JWT |
| GET | `/api/teacher/books` | TEACHER | Book catalog |
| GET | `/api/teacher/students` | TEACHER | Student roster |
| POST | `/api/teacher/assignments` | TEACHER | Create assignment (auto-assigns to all students) |
| GET | `/api/teacher/assignments` | TEACHER | My assignments + aggregate status |
| GET | `/api/teacher/assignments/{id}/progress` | TEACHER | Per-student status + minutes |
| GET | `/api/student/assignments` | STUDENT | My assigned readings |
| GET | `/api/student/assignments/{id}` | STUDENT | Reading detail incl. content |
| PUT | `/api/student/assignments/{id}/status` | STUDENT | Update status / minutes read |

## Key architectural decisions (summary — full reasoning in `decision-rationale.md`)

1. **JWT + method security** instead of server-side sessions — stateless, matches a future
   multi-service split; tradeoff: no server-side revocation (acceptable for the prototype).
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
- Refresh-token rotation, token revocation, and secure (httpOnly) token storage.
- Server-side session timers (students sometimes read on multiple devices).
- TypeScript, component library (MUI), Vitest + React Testing Library for frontend unit tests.
- Pagination, search/filtering, admin role, audit logs, and deployment via CI/CD.
