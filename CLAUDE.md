# Scholastic Coding Challenge — Teacher Reading Assignment Portal

This is a dedicated, isolated workspace for building the Scholastic take-home assessment.
The job-search context lives elsewhere and is **NOT** tracked here. Work in this folder,
build the solution, and deliver the final artifacts back to the job-search session.

## Mission
Design and build a lightweight, end-to-end web application that lets teachers assign reading
to students and track assignment status. Target effort ~4 hours. Due **Fri Aug 22 EOD**.

## Implementer-Decided Constraints (candidate decisions — treat as fixed)
- **Frontend:** React
- **Backend:** Java
- **Supporting services:** all local (e.g. SQLite for persistence)
- **Deployment:** must be runnable locally AND deployed to a public URL with credentials
- **Deliverables:** GitHub repo + live URL + write-up (README) covering what was built,
  key architectural decisions, tradeoffs/assumptions, and what would be improved with more time

## What the Grader Wants (read carefully)
The prompt is graded on:
1. Interpreting requirements
2. Architectural and implementation tradeoffs
3. Balancing quality with time constraints
4. Communicating decisions clearly

The **decision-rationale write-up is the real test** — the interviewer will discuss this live.
Do not treat this as just "make it run."

## Read First
- `requirements.md` — the full verbatim assessment prompt. Use as the spec anchor.
- Missing: candidate verbal design decisions (the user discussed design verbally, outside this
  file). If not present in this workspace, the work session must capture assumptions itself
  and document them — that is expected and rewarded.

## Defines
- A recommendation: log each architectural decision in `decision-rationale.md` the moment it's
  made (not after the fact), so nothing is lost.

## Boundary (orchestration contract)
- **In:** can work on all coding, architecture, tests, README, deployment, and rationale docs.
- **Out:** do not pull in job-search context. Do not add job-search tracker/notes here.
- **Deliverable back:** the finished repo path, live URL + creds, and the rationale doc.