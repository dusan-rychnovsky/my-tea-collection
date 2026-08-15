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

Verified green baseline on `main` (2026-08-11, `./mvnw clean verify`): **48 unit tests (7 classes) + 16 integration tests (6 classes), 0 failures/errors/skipped.**

## Green mainline = deployable to PROD (IMPORTANT)

- **On the mainline branch (`main`), every change must leave the full suite green — all unit AND integration tests passing** — so `main` can be deployed to PROD at any moment. Run `./mvnw clean verify` and confirm BUILD SUCCESS before committing to `main`.
- **Feature branches may be WIP / temporarily red.** The always-green bar applies to the mainline, not to in-progress feature branches.
- Why this discipline matters: CI (`.github/workflows/deploy.yml`) builds with `-DskipTests` and pushes the image to ACR on every push to `main` — **CI does not run the tests**. Nothing but this local discipline stops a red commit from being deployed.

## Test coverage expectations (IMPORTANT)

- **Every change must be fully covered by tests — both unit and integration.** For any change: review which existing tests need updating, and add new tests for the new behavior. A change isn't done until it's covered at both levels.
  - **Unit tests** (`*Tests.java`, surefire): plain JUnit 5, no Spring context — for pure logic (entities, `model/`, `util/`). Fast.
  - **Integration tests** (`integration/*IT.java`, failsafe): `@SpringBootTest` + `@AutoConfigureMockMvc` + `@TestPropertySource("classpath:application-test.properties")`, driving controllers/HTTP and persistence end-to-end via `MockMvc`. Seed data through `CreateUser` + `UploadNewTeas` in `@BeforeEach`; assert page content with the `ITUtils.containsStrings` / `doesNotContainStrings` helpers (see `TeaViewIT`).

## Layout (package `cz.dusanrychnovsky.myteacollection`)

- (root) `MyTeaCollectionApplication` — just `@SpringBootApplication` + `main`. Also `AuthController`, `SecurityConfig`.
- `web/` — MVC controllers (inbound HTTP adapter): `TeaQueryController` (reads: `/`, `/index`, `/filter`, `/search`, `/teas/{id}`), `TeaController` (writes: `/teas/add`), `ImageController` (`/images/{id}`).
- `ingest/` — JSON/filesystem inbound adapter (sibling of `web/`, not a util): the standalone CLI Spring Boot batch apps `UploadNewTeas` / `UpdateTeasAvailability` (each its own `@SpringBootApplication` `main`; `UploadNewTeas` builds an `AddTeaCommand` per tea and delegates to `application.AddTea`), the `TeaRecord` JSON contract, `TeaRecordMapper` (static `TeaRecord` → `AddTeaCommand`: resolves vendor/type/tag names → ids, parses price), and `CannotLoadTea*Exception`.
- `application/` — write use cases (the shared core the inbound adapters call): `AddTea` (`@Service`; resolves + validates the command's reference ids, then maps and saves, returning the new id), `AddTeaCommand` (its input DTO, reference data by id), and `TeaMapper` (pure static domain `Tea` + resolved reference entities → `TeaEntity`, incl. images).
- `domain/` — write-side domain model: the `Tea` aggregate (a class — identity, not value, semantics; the seam where write invariants will live) plus the `Price` and `TeaScope` value objects (records).
- `persistence/` — JPA entities (`TeaEntity`, `TeaImageEntity`, `TeaImageDataEntity`, `TagEntity`, `TeaTypeEntity`, `VendorEntity`, embeddable `TeaScopeEntity`, `persistence/users/UserEntity`) + Spring Data repositories.
- `query/` — read side (CQRS): per-view read models (`TeaSummary`, `TeaTag` for the index; `TeaDetail`, `TeaScope` for the detail page) and `TeaQueryRepository`, the Criteria-API paging / filter / search projection that feeds the index.
- `model/` — request/view helpers (`FilterCriteria`, `SearchCriteria`, `PageInfo`, `Availability`).
- `security/` — `EmailBasedUserDetailsService`.
- `util/` — stateless shared helpers: `MapUtils`, `ClassLoaderUtils`, and `JpgCompression` (JPG compression used by both `ingest` and `web`), plus the `util/users/CreateUser` CLI `@SpringBootApplication`.
- `src/main/resources/templates/` — Thymeleaf views (`index`, `tea-view`, `tea-add`, `login`); `static/` holds CSS/JS.
- Tests mirror this under `src/test/java`; integration tests live in the `integration/` package. H2 config in `src/test/resources/application-test.properties`, seed rows in `data.sql`.

## Domain terminology

- **SCOPE** is a MeiLeaf terminus technicus for a tea's provenance characteristics: **S**eason, **C**ultivar, **O**rigin, **P**icking, **E**levation. This app uses all but Picking. It's a deliberate, meaningful name — don't "fix" it or reorder its fields (the `TeaScopeEntity` constructor and the `TeaScope` records follow SCOPE order: season, cultivar, origin, elevation). Persistence type: `persistence/TeaScopeEntity` (`@Embeddable`); read-side view: `query/TeaScope` (record); write-side value object: `domain/TeaScope` (record).
- **Tea write invariants**: `domain/Tea` enforces **every invariant checkable from its own state** (throwing `IllegalArgumentException`): `title`/`description`/`url` mandatory (`url` well-formed), `scope` and `vendorId` non-null, at least one type, at least one image, `tagIds` non-null (may be empty). **`name` is optional** — it may be blank (some teas have only a technical title). Price is guarded by the `Price` VO (non-negative; `null` = absent). `application/AddTea` adds only what genuinely needs the DB — **existence**: that the referenced vendor / types / tags rows exist, plus resolving the **owner** (actor context supplied by the app/adapter, deliberately not a `Tea` property). This is the intrinsic-vs-existence split: the aggregate owns presence + cardinality of its references; existence is a boundary concern (app service, optionally a DB FK). A future "default image" workstream may relax the ≥1-image rule. Domain/service layers **throw**; each inbound adapter reports rejections its own way: `web/TeaController` catches and re-renders the form with an `error` model attribute (Bootstrap alert banner in `tea-add.html`); `ingest/UploadNewTeas` logs the rejected tea (`logger.error`) and aborts the batch (leaving already-uploaded teas committed, keeping the resume id-alignment intact).

## Conventions

- Idiomatic Spring / Java. Constructor injection. Match the existing **2-space indentation**.
- **Naming.** Infrastructure/adapter classes carry a role suffix — `…Controller`, `…Repository`, `…Entity`, `…Mapper`. Application **use-case** classes are named as bare imperative verb phrases — `AddTea` (handling `AddTeaCommand`), mirroring the CLI apps `CreateUser` / `UploadNewTeas` — **not** `AddTeaService`; a `…Service` suffix is reserved for framework-contract impls (e.g. `EmailBasedUserDetailsService` implements `UserDetailsService`). Value objects / read models / DTOs use plain domain names (`Price`, `TeaDetail`, `AddTeaCommand`).
- Test naming drives the runner: `*Tests.java` → surefire (unit), `integration/*IT.java` → failsafe (integration). Keep new tests to this pattern so they run in the right phase.

## Gotchas

- **`clean` before trusting Maven test results.** `target/` is not wiped on branch switches, so stale compiled `*.class` files and old surefire/failsafe report `*.txt` from another branch (e.g. a feature branch's tests) linger and can pollute report parsing or appear to run. `./mvnw clean verify` avoids this.
- **CI builds with `-DskipTests`** — a green CI run does *not* mean the tests pass. Verify locally.
