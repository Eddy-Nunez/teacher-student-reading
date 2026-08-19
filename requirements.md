# Teacher Reading Assignment Portal — Assessment Prompt

> Source: `Assessment-prompt.pdf` (Scholastic coding challenge). This is the verbatim
> requirements from the prompt the candidate received. This workspace is dedicated to
> designing and building the solution.

## Overview
Design and build a lightweight, end-to-end web application that allows teachers to assign
reading to students and track assignment status.

The exercise is intentionally high-level and partially incomplete. It is graded on how the
candidate:
- Interprets requirements
- Makes architectural and implementation tradeoffs
- Balances quality with time constraints
- Communicates decisions clearly

Target effort: **~4 hours**.

## Product Concept
A lightweight portal where teachers can assign books to students and track assignment progress.

## User Roles
Candidate may choose their own approach to user identity and roles. Real authentication is
**preferred** but scope should be kept reasonable.

### Teacher
- View a list of available books that can be assigned to students
- Create a reading assignment (book + due date) for student(s)
- View created assignments, their status, and minutes read
- View assignment status for students

### Student
- View assigned reading
- Open/view the assigned book
- Track minutes read
- Update assignment status
- See and update current status:
  - Not Started
  - In Progress
  - Completed

## Functional Requirements
- **Books:** A list of books should be available to assign.
- **Assignments:** Teachers create assignments by selecting a book and a due date.
- **Assignment Status:** Assignments track student progress using the defined statuses.
  - Students can update their assignment status.
  - Teachers can view assignment progress.

## Intentionally Incomplete Requirements
Some details are open. The candidate is expected to:
- Make reasonable assumptions
- Document those assumptions
- Design for clarity and extensibility

There is no single "correct" solution.

## Technical Expectations
- **Tech stack:** Any language/framework. Scholastic primarily uses **Java (backend)** and
  **React (frontend)** — free to use if comfortable, but not required.
- **Architecture:** Implement both frontend AND backend.
- **AI Tooling:** AI-assisted tools (Claude, Cline, etc.) may be used during the challenge.
- **Deployment:** App must be:
  - Runnable locally (README must allow local run)
  - Deployed to a public URL (e.g. Vercel, Netlify, Render, Fly.io, GB etc.)

## Deliverables
1. **GitHub repo** — clear README with setup instructions.
2. **Live deployed URL** with credentials.
3. **Short written explanation** (README is fine) covering:
   - What was implemented
   - Key architectural decisions
   - Tradeoffs + assumptions
   - What would be improved with more time

## Time Expectation
Approximately **4 hours**.

## Follow-Up Interview
The submission is used as the basis for a potential follow-up interview, including:
- Architectural decisions and tradeoffs
- Code quality and testing approach
- How the candidate would lead and evolve this system with a team

> "There are no trick questions, we want to understand how you think."