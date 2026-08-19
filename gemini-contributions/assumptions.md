# Architectural & Design Assumptions

**Project Overview:** Reading Assignment Platform  
**Target Timeline:** 4-hour implementation sprint  

---

## 1. Domain & Scope Assumptions

* **Classroom & Scale:** 
  * Small class sizes are assumed.
  * Complex pagination, infinite scrolling, and advanced server-side filtering are explicitly out of scope for both the teacher and student dashboards.
* **Document Content Storage:** 
  * Reading assignments link directly to external resources via a `referenceUrl` or display embedded raw text/binary content. 
  * The platform does not require a custom rich-text editor or complex PDF document parsing backend.
* **Enrollment Model:** 
  * Creating a new assignment automatically assigns it to all existing students in the system. 
  * Complex classroom grouping, course enrollment rosters, and multi-tenancy are omitted to keep the database schema lean.

---

## 2. Authentication & Security Assumptions

* **Authentication Protocol:** 
  * Simple, stateless Username/Password authentication issuing a JSON Web Token (JWT) is sufficient.
  * Modern alternatives (such as WebAuthn/Passkeys, OAuth2, or SSO) were explicitly bypassed due to implementation complexity and setup overhead.
* **Role-Based Access Control:** 
  * The system strictly distinguishes between two roles: `ROLE_TEACHER` and `ROLE_STUDENT`.
  * Multi-role capabilities per user or administrator-level global access are unnecessary for the prototype.
* **Token Storage:** 
  * The JWT will be stored in client-side `localStorage` and injected via an Axios request interceptor for rapid prototyping.

---

## 3. Technology Stack & Data Access Assumptions

* **Backend Persistency Layer:** 
  * **Spring Data JPA** was selected over jOOQ to save ~30–45 minutes of pipeline setup, eliminating custom code-generation plugins and DTO record mapping boilerplate.
* **Database Engine:** 
  * H2 (in-memory) or PostgreSQL for simple local development, relying on standard JPA entity mappings for automatic schema generation.
* **Frontend State & Navigation:** 
  * Built using React 18+ (Vite/CRA) with standard React Context API for global state management and standard React Router for role-guarded views.
  * Axios is used as the primary HTTP client due to clean, centralized interceptor support for JWT injection and unified `401 Unauthorized` handling.

---

## 4. Progress Tracking & Timer Assumptions

* **Local Storage Dominance:** 
  * Student active reading time is continuously calculated on the client side and saved in `localStorage` under `assignment_{id}_timer`.
* **Backend Synchronization:** 
  * Active time accumulates locally and is synced back to the server periodically or upon status state changes (e.g., explicitly clicking "Mark as Completed").
* **Offline / Mid-Session Resilience:** 
  * `localStorage` acts as the single source of truth for in-flight reading sessions to prevent loss of tracked minutes across unexpected tab closures or page reloads.