## Plan: Friendly Tea URLs

Introduce a persisted, globally unique slug for each tea, generated automatically as `{normalized-vendor}-{normalized-title}[-{year}]`. Make `/teas/{slug}` canonical, permanently redirect legacy numeric links, and rely on the repository's accepted full-database rebuild workflow instead of a migration/backfill. The agreed example is `/teas/mei-leaf-luminary-misfit-2022`.

**Steps**

### Phase 1: Slug Contract and Write Path

1. Correct the one known source inconsistency in `teas/43/info.json`: change season `Q3 2020` to `Q3 2022`, matching its title, description, and vendor URL. This is required before strict title/season year validation can be enabled.
2. Add a pure `tea/application/TeaSlug` value/factory and focused `TeaSlugTests` using only the Java standard library. Its contract must be deterministic:
   - Normalize vendor name and mandatory tea title independently in this order: apply Unicode NFKD; lowercase with `Locale.ROOT`; remove characters in the Unicode combining-mark categories `Mn`, `Mc`, and `Me`; remove both ASCII (`'`) and right-curly (`’`) apostrophes; replace each remaining run outside ASCII `a-z0-9` with one hyphen; trim leading/trailing hyphens. Apostrophe removal joins the surrounding text (`Shan's` -> `shans`); it does not introduce a separator.
   - Compose vendor and full title with exactly one hyphen between the normalized components: `{normalized-vendor}-{normalized-title}[-{year}]`. Never fall back to optional tea `name`.
   - Recognize a year only when it is a standalone ASCII token in the range 1900-2099, bounded on both sides by the start/end of input or a character outside `[A-Za-z0-9]`. Thus `2022`, `Q3 2022`, and `2020-2022` contain recognized years, while `8582`, `1990s`, `Spring2022`, and `2022abc` do not.
   - Compare sets of distinct recognized years. If title and season each contain exactly one distinct year and they differ, throw an `IllegalArgumentException` naming both fields and values so both add adapters reject the tea. Repeated occurrences such as `2022/2022` count as one distinct year.
   - If the title contains any recognized year, append no year. Otherwise append the season year only when the season contains exactly one distinct recognized year. Omit a suffix for `N/A`, decade labels, and seasons containing multiple distinct years such as `2020-2022`.
   - Reject a vendor or title component that normalizes empty, a final slug that is all-numeric or exactly the reserved route segment `add`, or a final slug longer than 255 ASCII characters. An individually numeric component is allowed because the required vendor-title hyphen keeps the final slug distinct from a numeric ID route (for example, `2024-tea`). Do not truncate or add an external slugification dependency.
3. Persist and enforce the slug in the write path (*depends on step 2*):
   - Add a non-null `slug` column to `TeaEntity`, its constructor, and getter, mapped explicitly with `@Column(name = "slug", length = 255, nullable = false)`. Enforce global uniqueness with one explicitly named table-level database unique constraint; do not add a redundant ordinary index because PostgreSQL backs the unique constraint with an index.
   - Add `Optional<TeaEntity> findBySlug(String slug)` and `boolean existsBySlug(String slug)` repository methods. The database unique constraint is the final integrity guarantee; the application pre-check provides a clear duplicate error for ordinary sequential writes. Concurrent-collision behavior is an explicit decision below.
   - In `AddTea`, generate the slug after resolving the vendor, reject an existing slug with an `IllegalArgumentException` naming it, pass the slug through `TeaMapper`, and return an `AddedTea` result containing both database ID and slug.
   - Update `TeaController` to use Spring's existing `redirect:` convention, returning the current PRG-style HTTP 302 redirect after a successful POST directly to the friendly URL. `UploadNewTeas` may ignore the result; its existing catch/log/rethrow behavior will abort on a mismatch or collision.
4. Extend write-side coverage (*depends on step 3*): update `TeaMapperTests` and `AddTeaServiceIT` for persistence/result, year mismatch, and duplicate rejection; update `AddTeaIT` to assert the canonical post-add redirect and form-level duplicate error; update `UploadNewTeasIT` to assert generated slugs and that a duplicate slug is logged and aborts the batch. Keep the existing mandatory-title invariant unchanged and verify no second row is stored on collision.
   - `TeaSlugTests` must exercise decomposed/accented text, ASCII and right-curly apostrophes, consecutive separators including an emoji/non-ASCII-symbol run, normalized-empty components, final numeric/reserved rejection, an individually numeric component, exactly 255 characters, and 256-character rejection.
   - Year tests must explicitly cover inclusive boundaries `1900` and `2099`; out-of-range/product values `1899`, `2100`, and `8582`; decade labels `1990s` and `2020s`; attached text `Spring2022` and `2022abc`; a multi-year range `2020-2022`; and a repeated single distinct year `2022/2022`.
   - Concurrent constraint-race behavior is intentionally not covered in this workstream; duplicate tests exercise the ordinary sequential pre-check and database uniqueness separately.

### Phase 2: Canonical Read Path

5. Carry the persisted slug through both read models (*depends on step 3; can proceed in parallel with step 4*): add it to `TeaSummary` and its `TeaQueryRepository` projection, and to `TeaDetail.from`. Update their focused tests, including `TeaDetailTests` and repository integration assertions.
6. Split tea-detail routing by identifier (*depends on step 5*):
   - `GET /teas/{friendly-slug}` performs `findBySlug`, renders the existing detail view, and uses the resolved tea ID for tasting-note lookup.
   - `GET /teas/{numeric-id}` finds the tea and returns HTTP 301 with `Location: /teas/{slug}`.
   - Missing slugs and missing numeric IDs return 404 rather than the current `NoSuchElementException`/500 behavior.
   - Use genuinely non-overlapping Spring path constraints: numeric IDs match `[0-9]+`; friendly slugs match `[a-z0-9]+(?:-[a-z0-9]+)+` and therefore require at least one hyphen. Every generated slug satisfies this because it joins separately validated, nonempty vendor and title components. Preserve the more-specific authenticated `/teas/add` route and its security behavior. Uppercase and malformed slug paths do not resolve and return 404; they are not normalized or redirected.
   - Return an intentionally relative, same-origin `Location: /teas/{slug}` for the numeric 301 redirect. Relative `Location` values are permitted by HTTP and avoid coupling backward-compatible redirects to canonical-origin configuration.
7. Make generated links canonical (*depends on steps 5-6*): use `tea.slug` for all image/title/View links in `index.html`; use it in `tea-view.html`'s absolute Open Graph URL and add a matching canonical link element. Keep numeric URLs only as backward-compatible entry points, not generated links.
   - Both `og:url` and `<link rel="canonical">` must contain the same absolute URL. Obtain the trusted origin from the required `app.public-base-url` property, supplied through `APP_PUBLIC_BASE_URL` in production and set to `http://localhost` in integration tests; do not derive SEO metadata from request `Host` or forwarded headers.
   - Add a small immutable `tea/web/PublicBaseUrl` Spring component whose constructor receives `@Value("${app.public-base-url}")`, parses it with `java.net.URI`, and throws `IllegalArgumentException` during context creation unless it is an absolute `http` or `https` URI containing an authority and no path beyond optional `/`, query, or fragment. Normalize away a trailing `/` and expose URL composition without accepting arbitrary paths. Cover its validation and composition with pure `PublicBaseUrlTests`; inject it into `TeaQueryController` instead of constructing an origin from the request.
8. Extend read/web integration coverage (*depends on steps 6-7*):
   - Update `TeaCollectionIT` to expect friendly links for all link surfaces.
   - Move normal `TeaViewIT` rendering/tasting-note assertions to friendly URLs; add explicit tests for the exact Luminary URL, numeric 301 redirect with a relative same-origin `Location`, missing slug 404, missing ID 404, uppercase/malformed slug 404, friendly absolute canonical/OG metadata, and unchanged detail content.
   - Retain the existing `/teas/add` authentication test as the route-specificity regression guard.

### Phase 3: Workflow and Completion

9. Document the accepted schema policy (*can proceed in parallel after step 2*): add a schema-evolution section to `.github/instructions/webapp.instructions.md` stating that Git data is authoritative, backward-incompatible DDL and short downtime are accepted, and schema changes use a full rebuild: drop schema/database, generate/apply JPA DDL, load `data.sql`, run `CreateUser`, run `UploadNewTeas`, then run the `UploadTastingNotes` CLI (the runnable adapter around `ReplaceTeaTastingNotes`). Align the existing README database section with that policy and correct stale command/class naming if encountered. Do not introduce Flyway, Liquibase, an incremental SQL migration, or a backfill CLI. After verification, refresh the instruction file's dated test-count baseline.
   - Refresh both the verified-test date and unit/integration counts from the final `clean verify` reports.
   - Document a pre-drop production gate: take a restorable database backup, generate and validate all slugs from the Git fixtures in a disposable database, complete the full tea/tasting-note import there, and only then begin the destructive production rebuild. Retain the backup until production smoke checks pass.
10. Run focused tests after each implementation slice, then run the webapp's full clean gate with `webapp/mvnw.cmd clean verify`. Inspect the generated schema/test database to confirm the non-null unique slug constraint and audit the imported fixtures for uniqueness. Once green, run the required independent review sub-agent over the final diff for correctness, security, naming, idiomatic Spring/Java design, and unit/integration coverage; address every finding and rerun the affected tests plus `clean verify`.

**Relevant files**

- `teas/43/info.json` — correct the sole title/season year mismatch.
- `webapp/src/main/java/cz/dusanrychnovsky/myteacollection/tea/application/TeaSlug.java` — new deterministic slug and year policy.
- `webapp/src/main/java/cz/dusanrychnovsky/myteacollection/tea/application/AddedTea.java` — new AddTea result carrying ID and slug.
- `webapp/src/main/java/cz/dusanrychnovsky/myteacollection/tea/application/AddTea.java` — generate, check, persist, and return the slug.
- `webapp/src/main/java/cz/dusanrychnovsky/myteacollection/tea/application/TeaMapper.java` — map slug into persistence.
- `webapp/src/main/java/cz/dusanrychnovsky/myteacollection/persistence/TeaEntity.java` — non-null unique slug column.
- `webapp/src/main/java/cz/dusanrychnovsky/myteacollection/persistence/TeaRepository.java` — slug lookup and collision check.
- `webapp/src/main/java/cz/dusanrychnovsky/myteacollection/tea/query/TeaSummary.java` — expose slug to index links.
- `webapp/src/main/java/cz/dusanrychnovsky/myteacollection/tea/query/TeaDetail.java` — expose slug to canonical detail metadata.
- `webapp/src/main/java/cz/dusanrychnovsky/myteacollection/tea/query/TeaQueryRepository.java` — project persisted slug.
- `webapp/src/main/java/cz/dusanrychnovsky/myteacollection/tea/web/TeaQueryController.java` — friendly lookup, legacy 301, and 404 handling.
- `webapp/src/main/java/cz/dusanrychnovsky/myteacollection/tea/web/TeaController.java` — redirect new teas directly to their slug.
- `webapp/src/main/java/cz/dusanrychnovsky/myteacollection/tea/web/PublicBaseUrl.java` — validate the configured public origin and compose absolute canonical URLs.
- `webapp/src/main/resources/templates/index.html` — generate friendly detail links.
- `webapp/src/main/resources/templates/tea-view.html` — friendly Open Graph and canonical URL.
- `webapp/src/test/java/cz/dusanrychnovsky/myteacollection/tea/application/TeaSlugTests.java` — slug algorithm unit coverage.
- `webapp/src/test/java/cz/dusanrychnovsky/myteacollection/tea/web/PublicBaseUrlTests.java` — public-origin validation and URL composition coverage.
- `webapp/src/test/java/cz/dusanrychnovsky/myteacollection/tea/application/TeaMapperTests.java` and `webapp/src/test/java/cz/dusanrychnovsky/myteacollection/tea/query/TeaDetailTests.java` — mapping/read-model unit coverage.
- `webapp/src/test/java/cz/dusanrychnovsky/myteacollection/integration/AddTeaServiceIT.java`, `AddTeaIT.java`, `UploadNewTeasIT.java`, `TeaQueryRepositoryIT.java`, `TeaCollectionIT.java`, and `TeaViewIT.java` — application, adapter, query, routing, link, redirect, metadata, collision, and 404 coverage.
- `.github/instructions/webapp.instructions.md` — persist the database rebuild convention and refresh verified test counts.
- `README.md` — align the already-documented database setup/rebuild wording and runnable class names.

**Verification**

1. From `webapp/`, run `mvnw.cmd "-Dtest=TeaSlugTests,TeaMapperTests,TeaDetailTests" test` after the pure/model changes.
2. Run targeted integration coverage with `mvnw.cmd "-Dit.test=AddTeaServiceIT,AddTeaIT,UploadNewTeasIT,TeaQueryRepositoryIT,TeaCollectionIT,TeaViewIT" verify` after persistence and routing changes.
3. Run `mvnw.cmd clean verify` and record the final unit/integration counts in the webapp instruction file.
4. Verify via tests/smoke checks that `/teas/mei-leaf-luminary-misfit-2022` returns 200 with matching canonical and `og:url`, `/teas/2` returns 301 to it, unknown friendly/numeric references return 404, and `/teas/add` retains its authentication behavior.
5. For production rollout, follow the documented destructive rebuild sequence and confirm all Git-backed teas and tasting notes reimport successfully; slug collision/year validation makes the rebuild fail loudly before deployment if source data violates the contract.
   - Before dropping production, complete the same rebuild successfully in a disposable database and verify that a restorable production backup exists. If the production import or smoke checks fail, restore that backup rather than leaving the application on a partial dataset.
6. Run an independent review sub-agent, address findings, and rerun focused tests plus `mvnw.cmd clean verify`.

**Decisions**

- Canonical format: `{vendor}-{title}[-{year}]`, e.g. `mei-leaf-luminary-misfit-2022`; vendor comes from the resolved `Vendors.name` and title is mandatory.
- Slugs are generated and persisted automatically, not entered in JSON. They remain stable within a database; a deliberate vendor/title/year metadata change can change the slug on the next full rebuild.
- Duplicate or invalid generated slugs reject the add/import. There is no database-ID fallback and no custom-slug field.
- Legacy numeric detail URLs remain supported via HTTP 301 only; friendly URLs return 200 and are used everywhere internally.
- Schema rollout is a destructive rebuild from Git-held data. No migration framework, compatibility migration, or backfill is in scope.
- Historical aliases after metadata changes, vendor-specific slug fields, Unicode URLs/transliteration libraries, edit-flow slug behavior, share buttons, and sitemap work are out of scope.

**Decisions confirmed**

1. **Trusted canonical origin: explicit configuration.** Add the required `app.public-base-url` application property, supplied through `APP_PUBLIC_BASE_URL` in production (`https://mytea.dusanrychnovsky.cz`) and set to `http://localhost` in integration tests. Validate it at startup and build canonical/OG URLs from it. This is deterministic behind proxies and avoids trusting arbitrary request headers.
2. **Concurrent duplicate UX: integrity guarantee only.** Keep the friendly `existsBySlug` rejection for normal sequential adds and rely on the named unique constraint for the rare concurrent race. Do not add database-specific `DataIntegrityViolationException` translation in this workstream; a racing loser may receive a generic server error, while the database still prevents duplicate data. Improved concurrent-collision error handling may be added later.
3. **Historical friendly aliases: out of scope.** A vendor, title, or selected year change may generate a different slug on the next full rebuild; invalidating previously shared friendly links in this rare case is acceptable. The old friendly URL returns 404. Numeric legacy URLs still redirect when their IDs remain present, but no alias persistence or Git-backed alias source will be added.
