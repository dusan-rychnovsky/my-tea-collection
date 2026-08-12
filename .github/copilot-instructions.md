# My Tea Collection

Ecosystem for a personal tea collection app.

> **About these instructions:** This file holds the **repository-wide** custom instructions and
> applies to all work in this repo. Project-specific guidance lives in path-specific files under
> `.github/instructions/` (currently `scraper.instructions.md`, scoped via an `applyTo` glob). When
> you work on files a path-specific file matches, Copilot combines that file with this one; when you
> work repo-wide, only this file applies.

## Layout

- `webapp/` — Spring Boot web app (Java, Maven). The main user-facing application.
- `tools/scraper/` — Scala 3 + ZIO scraper for tea info. See `.github/instructions/scraper.instructions.md`.
- `teas/` — tea data assets.

The two apps have separate toolchains and each owns its own `.gitignore`, `.gitattributes`, etc.

## CI

`.github/workflows/deploy.yml` builds the webapp and pushes its Docker image to ACR (Maven build runs with `working-directory: webapp`). Scraper has no CI yet.

## Git

- Short imperative commit messages.
- Commit messages describe **what changed**, not which tool produced the change. Never add tool attribution or co-authored-by trailers (e.g. "Created by Copilot", "Co-authored-by: Copilot").
- **Never commit without approval.** When changes are ready to commit, show them (diff / summary) and ask whether they're OK; only commit after explicit approval.
- Structural / repo-wide changes (moves, restructures) commit directly to `main`.

## Line endings

- **Always use LF line endings** (Unix `\n`) for all files, never CRLF — when creating, editing, or normalizing files.
- Enforced repo-wide by the root `.gitattributes` (`* text=auto eol=lf`, binaries excluded) plus local `core.autocrlf=false`. After creating a file on Windows, verify it's LF (some editors/tools default to CRLF) and normalize if needed.

## Collaboration

- **Question requests; don't just comply.** When a request seems suboptimal — a weak name, questionable design, a hidden pitfall, or a better alternative exists — push back before doing it: explain the concern, offer alternatives, and ask whether to proceed as originally asked. (E.g. if asked to create a `TeaSnippet` class, note that "Snippet" is a poor name for a list-item model, suggest better options like `TeaSummary`, and confirm before proceeding.)
- **Always cover changes with tests.** After any change, make sure it's tested at the appropriate level — update existing tests or add new ones (unit and/or integration as fits).
- **Always run the affected app's full test suite after a change** to catch regressions. Scope the run to the app you changed (e.g. only the Scala tests in `tools/scraper/` for a scraper-only change, only the Maven tests in `webapp/` for a webapp-only change). Don't run the other app's tests.
- **Always review every finished change with an independent sub-agent.** Once a change is complete and green, spin up a separate review sub-agent to inspect the diff for introduced bugs, security vulnerabilities, test coverage (both unit and integration — every changed code path should be exercised), and overall code quality. The reviewer must actively question code style — **including naming decisions** — as well as design and whether the code is idiomatic for the stack, not just check correctness. Surface its findings and address them before considering the change done.
- **Follow platform idioms.** Stay within the conventions of each app's stack — idiomatic Spring/Java in `webapp/`, idiomatic Scala/ZIO in `tools/scraper/`.
- **Always keep these instruction files up to date.** After every change, check whether any Copilot instruction file needs updating to reflect new structure, tooling, conventions, or workflows, and update it in the same change. This is a multi-project repo:
  - Info that applies to the whole repo (shared conventions, layout, cross-cutting workflows) goes in this file, `.github/copilot-instructions.md`.
  - Info specific to a single project goes in that project's path-specific file, e.g. `.github/instructions/scraper.instructions.md` for scraper-only details (scoped via its `applyTo` glob).
