# My Tea Collection

Ecosystem for a personal tea collection app.

## Layout

- `webapp/` — Spring Boot web app (Java, Maven). The main user-facing application.
- `tools/scraper/` — Scala 3 + ZIO scraper for tea info. See `tools/scraper/CLAUDE.md`.
- `teas/` — tea data assets.

The two apps have separate toolchains and each owns its own `.gitignore`, `.gitattributes`, etc.

## CI

`.github/workflows/deploy.yml` builds the webapp and pushes its Docker image to ACR (Maven build runs with `working-directory: webapp`). Scraper has no CI yet.

## Git

- Short imperative commit messages.
- Structural / repo-wide changes (moves, restructures) commit directly to `main`.

## Collaboration

- **Always cover changes with tests.** After any change, make sure it's tested at the appropriate level — update existing tests or add new ones (unit and/or integration as fits).
- **Always run the affected app's full test suite after a change** to catch regressions. Scope the run to the app you changed (e.g. only the Scala tests in `tools/scraper/` for a scraper-only change, only the Maven tests in `webapp/` for a webapp-only change). Don't run the other app's tests.
- **Follow platform idioms.** Stay within the conventions of each app's stack — idiomatic Spring/Java in `webapp/`, idiomatic Scala/ZIO in `tools/scraper/`.
