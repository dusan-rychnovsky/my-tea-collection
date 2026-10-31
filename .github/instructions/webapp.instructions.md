---
applyTo: "webapp/**"
---

# Webapp

The main user-facing tea collection app: Spring Boot + Java + Maven, Thymeleaf views, Spring Data JPA/Hibernate over PostgreSQL (H2 in tests), Spring Security.

## Stack

Spring Boot 3.4.0 · Java 17 · Maven (wrapper `./mvnw`, `mvnw.cmd` on Windows) · Thymeleaf (+ Spring Security 6 extras) · Spring Data JPA / Hibernate · PostgreSQL (prod) / H2 (tests) · JUnit 5 + Spring Test + MockMvc.

## Build & test

Run everything from the `webapp/` module root, using the Maven wrapper.

- `./mvnw test` — compile + **unit** tests only (surefire; `*Tests.java`).
- `./mvnw verify` — unit **and integration** tests (failsafe runs `*IT.java`). This is the full green gate.
- `./mvnw clean package` — build the deployable jar (CI uses `mvn clean package -DskipTests`).

Integration tests run against in-memory **H2**, so no Docker/database is needed for `verify`.

**Always `clean` before trusting a full run** (`./mvnw clean verify`) — see Gotchas.

Verified green baseline (2026-10-07, `./mvnw clean verify`): **170 unit tests (26 classes) + 82 integration tests (12 classes), 0 failures/errors/skipped.**

## Schema evolution and production rebuilds

- Git-held tea and tasting-note files are the source of truth. Backward-incompatible DDL and brief
  maintenance downtime are accepted; do not add Flyway, Liquibase, incremental migrations, or a
  backfill CLI unless this policy changes.
- Before dropping production, create and test a restorable database backup. Rehearse the complete
  rebuild against a disposable PostgreSQL database using the Git fixtures; slug validation and the
  full tea/tasting-note import must succeed there before production work begins. Keep the backup
  until production smoke checks pass.
- Rebuild in this order: drop and recreate the database/schema; generate and apply JPA DDL; load
  `src/test/resources/data.sql`; run `CreateUser`; run `UploadNewTeas`; then run
  `UploadTastingNotes`. The latter is the runnable adapter around `ReplaceTeaTastingNotes`.
- After importing, verify the tea count, tasting-note count, non-null and unique slugs, one known
  tea's materialized average rating against its imported notes, one canonical tea page, and one
  legacy numeric redirect. Restore the backup if import or smoke checks fail rather than leaving
  production on a partial dataset.

## Green mainline = deployable to PROD (IMPORTANT)

- **On the mainline branch (`main`), every change must leave the full suite green — all unit AND integration tests passing** — so `main` can be deployed to PROD at any moment. Run `./mvnw clean verify` and confirm BUILD SUCCESS before committing to `main`.
- **Feature branches may be WIP / temporarily red.** The always-green bar applies to the mainline, not to in-progress feature branches.
- Why this discipline matters: CI (`.github/workflows/deploy.yml`) builds with `-DskipTests` and pushes the image to ACR on every push to `main` — **CI does not run the tests**. Nothing but this local discipline stops a red commit from being deployed.

## Test coverage expectations (IMPORTANT)

- **Every change must be fully covered by tests — both unit and integration.** For any change: review which existing tests need updating, and add new tests for the new behavior. A change isn't done until it's covered at both levels.
  - **Unit tests** (`*Tests.java`, surefire): plain JUnit 5, no Spring context — for pure logic (`domain/` aggregates + value objects, read models, mappers, `util/`). Fast.
  - **Integration tests** (`integration/*IT.java`, failsafe): `@SpringBootTest` + `@AutoConfigureMockMvc` + `@TestPropertySource("classpath:application-test.properties")`, driving controllers/HTTP and persistence end-to-end via `MockMvc`. Seed data through `CreateUser` + `UploadNewTeas` in `@BeforeEach`; assert page content with the `ITUtils.containsStrings` / `doesNotContainStrings` helpers (see `TeaViewIT`).

## Layout (package `cz.dusanrychnovsky.myteacollection`)

**Packaging convention: shared model, feature-sliced behavior.** The two *model* layers stay flat and
shared because the model is one coherent whole (the ubiquitous language / the DB schema): `domain/`
and `persistence/` hold **all** features' types together. The *behavioral* layers are sliced **by
feature** — each of `tea/` and `tastingnotes/` owns its own `application` / `ingest` / `query` (and
`tea/` its `web`) — so a feature's use cases sit together. Cross-cutting infrastructure
(`security/`, `util/`, and the root app/auth classes) stays flat/shared. This means the top level
deliberately mixes **layer-named** packages (`domain`, `persistence`, `security`, `util`) with
**feature-named** ones (`tea`, `tastingnotes`); that split (model vs behavior) is intentional, not an
oversight. `tastingnotes` has no `web`/`domain`/`persistence` of its own: notes render inside the tea
detail page (`tea/web/TeaQueryController`) and their model lives in the shared `domain`/`persistence`.

- (root) `MyTeaCollectionApplication` — just `@SpringBootApplication` + `main`. Also `AuthController`,
  `SecurityConfig`, and the cross-cutting `RequestLoggingFilter` (one method/URL/status/duration completion
  log for every HTTP request; 5xx completions are ERROR, while embedded Tomcat owns the single stack-trace
  log for exceptions that escape request processing).
- `domain/` — domain model (flat, shared): the `Tea` and `TastingNote` aggregates (classes — identity, not value, semantics; the seam where write invariants live; `TastingNote` holds its tea/taster ids and rating/date/body, without a note id) plus the `Price`, `Season`, `TeaScope`, `Rating`, `TeaSlug`, and `LatestTasterRatings` value objects/policies (records; `Rating` owns the 0–10 half-star ↔ 0.0–5.0 conversion; `LatestTasterRatings` selects one current opinion per taster from a single tea's `TastingNote` objects by tasting date, arbitrarily keeping the first encountered note when dates match — the write path uses its average for the materialized sorting score, while the detail read uses its ratings for taster count and distribution; `Season` preserves meaningful season text and permits at most one distinct exact 1900–2099 year; `TeaSlug` owns deterministic friendly-URL generation and validation) and the `TeaTitleYear` policy shared by title-derived labels.
- `persistence/` — JPA entities and mappings (flat, shared): `TeaEntity` (including its non-null, globally unique friendly-URL slug and nullable materialized average-rating projection), `TeaImageEntity`, `TeaImageDataEntity`, `TagEntity`, `TeaTypeEntity`, `VendorEntity`, embeddable `TeaScopeEntity`, `TastingNoteEntity` (table `TastingNotes`, `body` TEXT, `@ManyToOne` tea+user, deliberately not mapped as a collection on `TeaEntity`), `TastingNoteMapper` (shared domain/JPA mapping for writes and rating-summary reads; verifies resolved tea/taster references without exposing database note identity to the domain), `persistence/users/UserEntity` + Spring Data repositories (`TeaRepository` supports slug lookup/collision checks; `TastingNoteRepository` fetches a tea's notes newest-first with a `join fetch` on the owner).
- `tea/` — the tea feature's behavior:
  - `tea/web/` — MVC controllers (inbound HTTP adapter): `TeaQueryController` (reads: `/`, `/index`, `/filter`, `/search`, canonical `/teas/{slug}`, plus permanent redirects from legacy numeric `/teas/{id}` URLs — also renders the tea's tasting notes via the `tastingnotes/query` read models), `PublicBaseUrl` (validates the configured public HTTP(S) origin and composes trusted absolute canonical tea URLs), `TeaController` (writes: `/teas/add`), `ImageController` (`/images/{id}`).
  - `tea/ingest/` — JSON/filesystem inbound adapter: the CLI Spring Boot batch apps `UploadNewTeas` / `UpdateTeasAvailability` (`UploadNewTeas` builds an `AddTeaCommand` per tea and delegates to `tea/application.AddTea`), the `TeaRecord` JSON contract, `TeaRecordMapper`, and `CannotLoadTea*Exception`.
  - `tea/application/` — write use cases: `AddTea` (`@Service`; resolves + validates reference ids, generates and checks the slug, then maps and saves), `AddTeaCommand`, `AddedTea` (new id + slug result), `TeaMapper`.
  - `tea/query/` — read side (CQRS): per-view read models (`TeaSummary`, `TeaTag` for the index; `TeaDetail`, `TeaScope`, and `TeaSocialMetadata` for the detail page; both tea models expose the persisted slug), `TeaQueryRepository` (Criteria-API paging/filter/search/sort projection), and the query-input/paging types `FilterCriteria`, `SearchCriteria`, `TeaSort`, `PageInfo`, `Availability` (consumed by `TeaQueryRepository`, bound by the controller — that's why they live with the read side, not under `web`).
- `tastingnotes/` — the tasting-notes feature's behavior:
  - `tastingnotes/ingest/` — `UploadTastingNotes` (CLI `@SpringBootApplication`; reads each tea folder's `tasting-notes.json` and delegates a `ReplaceTeaTastingNotesCommand` to `tastingnotes/application.ReplaceTeaTastingNotes` — idempotent replace-per-tea, absent file = skip, `[]` = clear), the `TastingNoteRecord` JSON contract, `TastingNoteRecordMapper`, `CannotLoadTastingNotesException`. NB: `UploadTastingNotes` (like `CreateUser`) declares no `@EnableJpaRepositories`/`@EntityScan` — those come from `UploadNewTeas` via component scanning; re-declaring them duplicates repository beans and breaks the test context.
  - `tastingnotes/application/` — `ReplaceTeaTastingNotes` (`@Service`; builds+validates each `TastingNote` from the command's tea/taster ids and `NoteData`, resolves those references, then atomically replaces a tea's notes and updates its materialized `averageRatingHalfStars` projection in one transaction; reloads the saved history through the same repository query used by the detail page so arbitrary same-date choices remain consistent), `ReplaceTeaTastingNotesCommand` (carries `List<NoteData>`; `userId` identifies the taster, not a separate import actor).
  - `tastingnotes/query/` — `TastingNoteItem` + `RatingSummary` (historical note count, materialized average label, current taster count, and six-row 5★–0★ distribution using only each taster's latest rating for the detail page; author name/avatar derived from the owning user).
- `security/` — `EmailBasedUserDetailsService` (shared).
- `util/` — stateless shared helpers: `MapUtils`, `ClassLoaderUtils`, and `JpgCompression` (JPG compression used by both ingest and web), plus the `util/users/CreateUser` CLI `@SpringBootApplication`.
- `src/main/resources/templates/` — Thymeleaf views (`index`, `tea-view`, `tea-add`, `login`); `static/` holds CSS/JS.
- Tests mirror the production packages under `src/test/java` (unit tests move with their feature; e.g. `tea/ingest/TeaRecordTests`, `tastingnotes/query/RatingSummaryTests`, `domain/RatingTests`). Integration tests stay in one flat `integration/` package (system-level, often cross-feature — e.g. `TeaViewIT` exercises tea + tasting notes together). H2 config in `src/test/resources/application-test.properties`, seed rows in `data.sql`.


## Domain terminology

- **Tasting note invariants**: tea and taster references, rating and tasting date are mandatory; body must not be blank. Notes have no domain id or persisted-only state. `LatestTasterRatings` requires notes belonging to one tea and selects the latest tasting date per taster; same-date ties may choose any note (the implementation retains the first encountered). Write averages and read distributions must consume the same history order so their arbitrary choices agree. Reference existence is checked by `ReplaceTeaTastingNotes`, not the domain.
- **SCOPE** is a MeiLeaf terminus technicus for a tea's provenance characteristics: **S**eason, **C**ultivar, **O**rigin, **P**icking, **E**levation. This app uses all but Picking. It's a deliberate, meaningful name — don't "fix" it or reorder its fields (the `TeaScopeEntity` constructor and the `TeaScope` records follow SCOPE order: season, cultivar, origin, elevation). Persistence type: `persistence/TeaScopeEntity` (`@Embeddable`); read-side view: `tea/query/TeaScope` (record); write-side value object: `domain/TeaScope` (record). The write side represents absence as `Optional<Season>`; ingest translates the JSON `N/A` sentinel to absence, and persistence stores it as a nullable season string.
- **Tea write invariants**: `domain/Tea` enforces **every invariant checkable from its own state** (throwing `IllegalArgumentException`): `title`/`description`/`url` mandatory (`url` well-formed), `scope` and `vendorId` non-null, at least one type, at least one image, `tagIds` non-null (may be empty). **`name` is optional** — it may be blank (some teas have only a technical title). Price is guarded by the `Price` VO (non-negative; `null` = absent), while `TeaSlug` owns slug format/generation and title-vs-season year consistency. `tea/application/AddTea` adds what genuinely needs the DB: resolving the **owner** (actor context supplied by the app/adapter, deliberately not a `Tea` property), validating that referenced vendor/types/tags exist, supplying the resolved vendor name to `TeaSlug`, and rejecting an existing slug; a named DB unique constraint is the final slug integrity guarantee. This is the intrinsic-vs-existence split: the domain owns value invariants and reference presence/cardinality; existence and global uniqueness are boundary concerns. A future "default image" workstream may relax the ≥1-image rule. Domain/service layers **throw**; each inbound adapter reports rejections its own way: `tea/web/TeaController` catches and re-renders the form with an `error` model attribute (Bootstrap alert banner in `tea-add.html`); `tea/ingest/UploadNewTeas` logs the rejected tea (`logger.error`) and aborts the batch (leaving already-uploaded teas committed, keeping the resume id-alignment intact).

## Conventions

- Idiomatic Spring / Java. Constructor injection. Match the existing **2-space indentation**.
- **Entity test fixtures.** Use real JPA entities for mapper/read-model unit tests; set generated IDs
  with `ReflectionTestUtils.setField` when needed. Reserve Mockito for behavioral collaborators,
  not data-only entity mocks or spies.
- Console logs use Spring Boot's human-readable text format at INFO by default. Opt into DEBUG with the
  standard `logging.level.*` properties / `LOGGING_LEVEL_*` environment variables; do not enable SQL DEBUG
  globally.
- `app.public-base-url` is required and must be supplied through `APP_PUBLIC_BASE_URL` in production; integration tests set it to `http://localhost`. Canonical and Open Graph URLs must use `PublicBaseUrl`, never request host or forwarded headers.
- **Naming.** Infrastructure/adapter classes carry a role suffix — `…Controller`, `…Repository`, `…Entity`, `…Mapper`. Application **use-case** classes are named as bare imperative verb phrases — `AddTea` (handling `AddTeaCommand`), mirroring the CLI apps `CreateUser` / `UploadNewTeas` — **not** `AddTeaService`; a `…Service` suffix is reserved for framework-contract impls (e.g. `EmailBasedUserDetailsService` implements `UserDetailsService`). Value objects / read models / DTOs use plain domain names (`Price`, `TeaDetail`, `AddTeaCommand`).
- Test naming drives the runner: `*Tests.java` → surefire (unit), `integration/*IT.java` → failsafe (integration). Keep new tests to this pattern so they run in the right phase.

## Gotchas

- **Compare the runtime JVM for cross-platform failures.** `./mvnw -v` (`.\mvnw.cmd -v` on Windows)
  reports the JVM actually running Maven/tests; `<java.version>17</java.version>` sets the compilation
  target, not that runtime.
- **`clean` before trusting Maven test results.** `target/` is not wiped on branch switches, so stale compiled `*.class` files and old surefire/failsafe report `*.txt` from another branch (e.g. a feature branch's tests) linger and can pollute report parsing or appear to run. `./mvnw clean verify` avoids this.
- **CI builds with `-DskipTests`** — a green CI run does *not* mean the tests pass. Verify locally.
