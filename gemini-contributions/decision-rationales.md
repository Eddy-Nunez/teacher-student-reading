# Architectural Decision Records (ADR) & Decision Rationales

**Project Overview:** Reading Assignment Platform  
**Target Timeline:** 4-hour implementation sprint  

---

## 1. Data Access Strategy: Spring Data JPA over jOOQ

* **Decision:** Adopt **Spring Data JPA** (`JpaRepository`) for database persistency instead of **jOOQ**.
* **Rationale:** 
  * **Time Savings:** Eliminates 30–45 minutes of upfront configuration, including code generation plugins, live schema setup, and custom `Record` to DTO mappers.
  * **Sprint Fit:** For standard CRUD endpoints and simple single-table updates, JPA entities with automated schema generation let the developer spin up the entire data layer in minutes.
  * **Trade-Off Acknowledgment:** While jOOQ provides better SQL control and avoids ORM traps (like N+1 queries or detached entity states), these benefits are secondary given the strict 4-hour build timeline and basic query requirements.

---

## 2. Authentication Strategy: Username/Password with JWT over Passkeys (WebAuthn)

* **Decision:** Implement standard **Username/Password authentication issuing a stateless JSON Web Token (JWT)**.
* **Rationale:** 
  * **Implementation Complexity:** WebAuthn/Passkeys require managing browser cryptographic challenge-response APIs, credential registration routines, and public key verification on the backend.
  * **Sprint Fit:** Spring Security provides turnkey patterns for BCrypt password hashing and JWT issuance. A basic React login form paired with local storage requires minimal code and zero external setup.

---

## 3. HTTP Client Selection: Axios over Fetch API

* **Decision:** Use **Axios** as the primary frontend HTTP client.
* **Rationale:** 
  * **Centralized Interceptors:** Axios natively supports request and response interceptors (`axios.interceptors.request.use`), making it trivial to inject `Authorization: Bearer <token>` into every outbound request and intercept `401 Unauthorized` responses globally.
  * **Boilerplate Reduction:** Achieving the same interceptor pattern with the native `fetch` API requires writing custom wrapper functions or proxy utilities, adding unnecessary boilerplate during a timed sprint.

---

## 4. State Management & Progress Tracking: Client-Side `localStorage` Timer

* **Decision:** Track active student reading time locally in browser **`localStorage`** and synchronize with the backend on status updates or periodic intervals.
* **Rationale:** 
  * **Server Uncoupling:** Avoids spamming the backend with high-frequency heartbeat requests (e.g., every second), keeping the API stateless and lightweight.
  * **Session Resilience:** Preserves tracked reading time across accidental tab closes, page refreshes, or network drops without requiring complex server-side session recovery mechanisms.

---

## 5. Scope Management: Omitting Pagination & Complex Enrollment Features

* **Decision:** Omit server-side pagination, advanced filtering, and multi-class enrollment management.
* **Rationale:** 
  * **Target Domain:** The core requirement assumes small class sizes and small report volumes.
  * **Automation:** Automatically assigning newly created readings to all existing student records reduces schema complexity (avoiding join tables for rosters/courses) and accelerates both backend logic and frontend UI delivery.