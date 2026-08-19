# Status — Teacher Reading Assignment Portal

> Owned by the build session. Update this as work progresses.

## Milestones

- [x] Design/assumptions captured (design from `gemini-contributions/` + new decisions recorded during build)
- [x] Project scaffold (backend + frontend)
- [x] Data model / persistence (H2)
- [x] Core flows implemented (books, assignments, status updates, minutes read)
- [x] Auth / role approach (JWT username/password; teacher vs student)
- [x] Tests (8/8 backend integration tests + browser E2E)
- [x] README (setup, run, arch, tradeoffs, improvements)
- [x] `decision-rationale.md` finalized
- [ ] GitHub repo + initial commit (in progress)
- [ ] Public deployment URL + credentials (blocked on credentials from user)

## Current focus
Git init + commit, then deployment. Deployment requires user input: no hosting credentials/tokens
exist on this box (no Render/Railway/Fly/GitHub auth). Options: (a) user provides a token/account,
(b) ephemeral cloudflared tunnel (works now, but requires this machine to stay on).
