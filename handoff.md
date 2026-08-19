# Handoff — from Job-Search Session (orchestration contract)

## Why this exists
The job-search workspace holds logistics: requirements capture, workspace scaffold, and
deliverable receipt. It intentionally does **NOT** hold assignment design/implementation
details — that work belongs to this dedicated workspace scoped to build the challenge.

## What is already in this workspace
| File | Purpose |
|------|---------|
| `requirements.md` | Full verbatim assessment prompt (extracted from `Assessment-prompt.pdf`). |
| `CLAUDE.md` | Auto-loading context for a fresh pi session started in this folder. |
| `status.md` | Progress tracker (empty; owned by the build session). |
| `decision-rationale.md` | Placeholder for the written rationale — THE key deliverable. |

## Decisions already made (from the candidate, verbally)
- **Frontend:** React
- **Backend:** Java
- **Supporting services:** all local / SQLite
- **Deployment:** public URL + local-runnable README
- **Orchestration:** dedicated workspace (this folder)

## Known metadata
- Due: **Fri Aug 22 EOD**
- Candidate's strengths for this role (JD): Python primary, Java secondary (self-rating 5/10,
  treat as competent-not-differentiator), React 5/10, AWS strong, agentic-AI-tooling is a direct
  match (daily Claude Code / agent pipelines).
- Role is a **Sr/Lead SWE** at Scholastic: they hire Java+React and value architectural
  decision-making + leading systems with a team. The whole point is the thinking, not the code.

## Requirements still missing from this workspace
- Candidate verbal design/implementation decisions discussed outside this workspace were NOT
  transcribed here (by design — this folder doesn't track that). Capture them now, before coding
  starts, so they're not lost.

## Boundary (orchestration)
- **In scope:** everything needed to design, build, deploy, document, and rationalize.
- **Out of scope:** job-search pipeline/tracker/notes. Keep them out of this folder.

## Return handoff
When build is complete, a fresh job-search session will consume: full repo path, deployed URL
+ creds, and `decision-rationale.md`. Nothing else needed.