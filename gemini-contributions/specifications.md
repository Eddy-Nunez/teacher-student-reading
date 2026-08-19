# System Architecture & Technical Specification

**Project Overview:** Reading Assignment Platform  
**Target Timeline:** 4-hour implementation sprint  
**Target Execution Environment:** CLI AI Coding Agent  

---

## 1. Core Architecture & Tech Stack

* **Backend Framework:** Java 17+, Spring Boot 3.x
* **Data Access Layer:** Spring Data JPA (PostgreSQL / H2 for fast local dev)
* **Authentication:** Spring Security with Stateless JWT (Username/Password authentication)
* **Frontend Framework:** React 18+ (Vite or Create React App)
* **HTTP Client:** Axios (configured with request/response interceptors for JWT injection)
* **State & Local Persistence:** React Context API + browser `localStorage` (for offline/in-flight timer tracking)

---

## 2. Backend Specification (Java / Spring Boot)

### 2.1 Domain Models & Entities

**User Entity (`app_user`)**
* `id` (Long, Primary Key, Auto-generated)
* `username` (String, Unique, Non-null)
* `password` (String, BCrypt hashed, Non-null)
* `role` (Enum: `ROLE_TEACHER`, `ROLE_STUDENT`)

**Assignment Entity (`assignment`)**
* `id` (Long, Primary Key, Auto-generated)
* `title` (String, Non-null)
* `description` (Text)
* `referenceUrl` (String, Non-null) — Link to external reading resource
* `assignedByTeacherId` (Long, Non-null) — Foreign key reference to `app_user.id`
* `createdAt` (Timestamp, Auto-generated)

**StudentAssignmentProgress Entity (`student_assignment_progress`)**
* `id` (Long, Primary Key, Auto-generated)
* `studentId` (Long, Non-null) — Foreign key reference to `app_user.id`
* `assignmentId` (Long, Non-null) — Foreign key reference to `assignment.id`
* `status` (Enum: `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`)
* `elapsedMinutes` (Long, Default: 0) — Synced accumulated reading time
* **Constraints:** Unique constraint on `(studentId, assignmentId)`

### 2.2 API Endpoints

#### Authentication
* **`POST /api/auth/login`**
  * **Payload:** `{ "username": "string", "password": "string" }`
  * **Response:** `{ "token": "jwt_string", "role": "ROLE_TEACHER|ROLE_STUDENT", "userId": 123 }`

#### Teacher Endpoints (`@PreAuthorize("hasRole('TEACHER')")`)
* **`POST /api/teacher/assignments`**
  * **Payload:** `{ "title": "string", "description": "string", "referenceUrl": "string" }`
  * **Behavior:** Creates assignment and automatically initializes `StudentAssignmentProgress` records (`NOT_STARTED`) for all enrolled students.
* **`GET /api/teacher/assignments`**
  * **Response:** List of assignments created by the authenticated teacher.
* **`GET /api/teacher/assignments/{id}/progress`**
  * **Response:** List of all students assigned to this reading, including `studentName`, `status`, and `elapsedMinutes`.

#### Student Endpoints (`@PreAuthorize("hasRole('STUDENT')")`)
* **`GET /api/student/assignments`**
  * **Response:** List of assigned readings for the logged-in student along with their current `status` and `elapsedMinutes`.
* **`PUT /api/student/assignments/{id}/status`**
  * **Payload:** `{ "status": "IN_PROGRESS|COMPLETED", "elapsedMinutes": 15 }`
  * **Behavior:** Updates progress record in DB. Called periodically or when marking as completed.

---

## 3. Frontend Specification (React)

### 3.1 Architecture & Utilities

* **`api/axiosInstance.js`:** Custom Axios instance configured with an `onRequest` interceptor that reads the JWT from `localStorage` and injects `Authorization: Bearer <token>`. Includes a response interceptor to catch `401 Unauthorized` and trigger logout.
* **`context/AuthContext.jsx`:** React Context handling `user`, `token`, `login()`, and `logout()` state.
* **`components/ProtectedRoute.jsx`:** Wrapper component checking JWT existence and verifying user roles against required route permissions.

### 3.2 Component Hierarchy

```text
src/
├── api/
│   └── axiosInstance.js
├── context/
│   └── AuthContext.jsx
├── components/
│   ├── Navbar.jsx
│   ├── ProtectedRoute.jsx
│   └── ReadingTimer.jsx
└── pages/
    ├── LoginPage.jsx
    ├── teacher/
    │   ├── TeacherDashboardPage.jsx
    │   ├── AssignmentTable.jsx
    │   ├── CreateAssignmentModal.jsx
    │   └── StudentProgressModal.jsx
    └── student/
        ├── StudentDashboardPage.jsx
        ├── AssignmentCardGrid.jsx
        └── ReadingViewPage.jsx

3.3 Functional Behavior Specifications
1. Authentication & Session Handling
Login Flow: User submits credentials at LoginPage. Upon receipt of JWT, the token, role, and userId are stored in localStorage and synchronized with AuthContext.

Route Protection: ProtectedRoute checks the current token and role. Unauthenticated requests redirect to /login. Teachers navigating to student routes (or vice versa) are redirected to their respective dashboards.

Token Injection: axiosInstance.js automatically attaches the token as Bearer <token> in the Authorization header for all outgoing API requests.

2. Local Storage Timer & Sync (ReadingTimer.jsx)
Time Tracking: On mount in ReadingViewPage, the component initializes a timer reading from localStorage.getItem('assignment_${id}_timer') or defaults to 0.

Active Ticking: Uses an setInterval loop (1-second tick) while the tab/view is active, updating both React state and localStorage every second.

Backend Sync Trigger:

Auto-syncs to PUT /api/student/assignments/{id}/status with status: "IN_PROGRESS" and current calculated elapsedMinutes at interval (e.g., every 60 seconds).

Upon clicking "Mark as Completed", immediately stops the interval, dispatches PUT /api/student/assignments/{id}/status with status: "COMPLETED", updates the local state, and clears assignment_${id}_timer from localStorage.

3. Teacher Dashboard Operations
Assignment Creation: CreateAssignmentModal posts form data (title, description, referenceUrl) to POST /api/teacher/assignments. Upon success, closes the modal and refetches the assignment list.

Student Progress Tracking: Clicking "View Progress" on an assignment opens StudentProgressModal, fetching /api/teacher/assignments/{id}/progress to render a table displaying all assigned students, their current status (NOT_STARTED, IN_PROGRESS, COMPLETED), and total elapsedMinutes.

4. Student Dashboard & Reader Operations
Assignment Filtering: StudentDashboardPage fetches /api/student/assignments on mount and categorizes assignments in AssignmentCardGrid by status.

Reading Engagement: Clicking "Start Reading" transitions the student to ReadingViewPage, embedding the reading material (referenceUrl) and mounting ReadingTimer.