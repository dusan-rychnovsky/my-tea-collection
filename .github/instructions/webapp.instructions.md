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

- (root) `MyTeaCollectionApplication` — `@SpringBootApplication` + `@Controller`; the web entry point and, currently, all tea / image / index / filter / search MVC endpoints. Also `AuthController`, `SecurityConfig`.
- `db/` — JPA entities (`TeaEntity`, `TeaImageEntity`, `TeaImageDataEntity`, `TagEntity`, `TeaTypeEntity`, `VendorEntity`, embeddable `TeaScope`, `db/users/UserEntity`) + Spring Data repositories. `TeaSearchRepository` holds the Criteria-API paging / filter / search query.
- `model/` — request/view helpers (`FilterCriteria`, `SearchCriteria`, `PageInfo`, `Availability`).
- `security/` — `EmailBasedUserDetailsService`.
- `util/` — stateless helpers plus **standalone CLI Spring Boot apps** used as batch tools (not part of the web server): `util/upload/` (`UploadNewTeas`, `UpdateTeasAvailability`, `TeaRecord`, `JpgCompression`) and `util/users/CreateUser`. Each is its own `@SpringBootApplication` `main`.
- `src/main/resources/templates/` — Thymeleaf views (`index`, `tea-view`, `tea-add`, `login`); `static/` holds CSS/JS.
- Tests mirror this under `src/test/java`; integration tests live in the `integration/` package. H2 config in `src/test/resources/application-test.properties`, seed rows in `data.sql`.

## Conventions

- Idiomatic Spring / Java. Constructor injection. Match the existing **2-space indentation**.
- Test naming drives the runner: `*Tests.java` → surefire (unit), `integration/*IT.java` → failsafe (integration). Keep new tests to this pattern so they run in the right phase.

## Gotchas

- **`clean` before trusting Maven test results.** `target/` is not wiped on branch switches, so stale compiled `*.class` files and old surefire/failsafe report `*.txt` from another branch (e.g. a feature branch's tests) linger and can pollute report parsing or appear to run. `./mvnw clean verify` avoids this.
- **CI builds with `-DskipTests`** — a green CI run does *not* mean the tests pass. Verify locally.
