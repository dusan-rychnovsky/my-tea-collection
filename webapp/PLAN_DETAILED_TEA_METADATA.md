# Detailed Tea Metadata Implementation Plan

## Agreed behavior

Open Graph and Twitter cards use the same values.

- Title format: `[title] ([vendor], [year])`.
- Omit the year and its comma when the season has no exact year.
- Omit the year when that same year already appears as a standalone year in the title.
- Examples:
  - `Luminary Misfit (Mei Leaf, 2022)`
  - `Shou Mei 2017 (Meetea)`
  - `Jade Star 8 (Mei Leaf)`
- Description format: `[technical name]. [description]`.
- Omit the technical name and separator when the name is null or blank.
- Abbreviate the complete combined description, not only the original description.

A tea may have no season. A present season contains meaningful nonblank descriptive text and may contain zero or one distinct exact year. Exact years retain the current `TeaSlug` semantics: standalone values from 1900 through 2099. Approximate values such as `Early 2000s`, `1990s`, and `1980s` are valid seasons with no exact year. Attached values such as `Spring2022` and `2022abc` also do not count as exact years.

## 1. Correct the existing blend data [Completed]

Before enforcing the new invariant, change Jade Star 8's season from `Spring 2013, 2014 and 2018` to `N/A` in both data sources:

- `../teas/23/info.json`
- `src/test/resources/teas/5/info.json`

Keep its technical name unchanged because it remains useful descriptive information about the blend components. Verify the production tea-data audit then reports no season with more than one distinct exact year.

Completed: both records now use `N/A`, the technical name is unchanged, and all 144 production tea JSON files parse with zero seasons containing multiple distinct exact years.

## 2. Introduce the `Season` domain value object [Completed]

Add `src/main/java/cz/dusanrychnovsky/myteacollection/domain/Season.java` as a record that preserves meaningful season text for display and persistence.

Its responsibilities are:

- Parse standalone years using the existing 1900-2099 boundary rule from `TeaSlug`.
- Reject null or blank season text; absence is represented by `Optional<Season>` outside this value object.
- Reject values containing more than one distinct exact year with an `IllegalArgumentException` that includes the season value and discovered years.
- Expose `Optional<Integer> year()`.
- Keep low-level exact-year extraction internal and package-private in `domain`; only `TeaTitleYear` uses it to analyze titles, avoiding a public general-purpose text-parsing API.

The record component is `String value`, so its generated `value()` accessor returns the original meaningful text for persistence. The domain does not interpret the JSON sentinel `N/A`; the ingest adapter translates that sentinel to an empty optional before constructing `Season`. Approximate season text such as `Early 2000s` remains a present `Season` whose `year()` is empty.

Add `SeasonTests` covering:

- One exact year.
- Constructor rejection for null and blank text.
- No exact year for approximate decades, out-of-range years, and years attached to letters/digits.
- Repeated occurrences of the same year, which still represent one distinct year.
- Rejection of two or more distinct exact years.
- `Spring and Autumn 2022` as one season value with one exact year.
- `Early 2000s` as a present valid season with no exact year.

Completed: added `Season` and its focused unit tests. `SeasonTests` passes all 5 tests with no failures, errors, or skips.

## 3. Promote season inside the write-side domain [Completed]

Change `domain/TeaScope` from a `String season` component to an `Optional<Season> season` component. Require the `Optional` itself to be non-null. Keep `tea/query/TeaScope` unchanged because it is a display-oriented read model and should continue exposing nullable persisted text.

Update write-side construction and mapping boundaries:

- `tea/ingest/TeaRecordMapper` maps the exact JSON sentinel `N/A` to `Optional.empty()` and wraps every other value in `Optional.of(new Season(...))`. Add mapper tests proving both the sentinel conversion and preservation of `Early 2000s` as a present season.
- `tea/web/TeaController` maps a null or blank optional form field to `Optional.empty()` and wraps nonblank text in `Optional.of(new Season(...))`.
- `tea/application/TeaMapper` persists `scope.season().map(Season::value).orElse(null)`.
- Update affected test builders and assertions to construct `Season` and `Optional<Season>` explicitly.

No database or DDL shape change is required: `TeaScopeEntity.season` remains a nullable string column containing meaningful season text. A rebuilt import stores SQL `NULL`, rather than `N/A`, for an absent season.

The existing adapter behavior handles validation failures: `TeaController` catches `IllegalArgumentException` and re-renders the form error, while `UploadNewTeas` logs the rejected tea and aborts the import. Add focused assertions only where existing tests do not already prove these paths.

Completed: `domain/TeaScope` now carries a non-null `Optional<Season>`; ingest maps `N/A` to absence, the add form maps null/blank input to absence, and persistence stores absence as SQL `NULL`. Focused unit and integration suites pass with no failures, errors, or skips.

## 4. Extract the shared tea title/year policy

Add `src/main/java/cz/dusanrychnovsky/myteacollection/domain/TeaTitleYear.java` as a stateless domain policy. This cross-field rule belongs neither to `Season` nor to either output formatter because it compares a tea title with its optional season.

Expose:

```java
public static Optional<Integer> suffixFor(String title, Optional<Season> season)
```

The policy uses `Season`'s package-private exact-year extraction and:

- Returns the season year only when the title contains zero exact years and the season contains one exact year.
- Returns empty when there is no season, the season has no exact year, or the title contains the same single exact year.
- Rejects a single exact title year that conflicts with the season's single exact year, preserving the current `TeaSlug` validation behavior.
- Returns empty when the title contains multiple distinct exact years, preserving the current `TeaSlug` behavior rather than appending or reporting a single-year conflict.

Add `TeaTitleYearTests` covering all four outcomes plus an equal single title/season year, standalone boundaries, non-year numeric values, and repeated occurrences of the same title year.

Refactor `TeaSlug` to remove its year regex, extraction, and title/season comparison. Use `TeaTitleYear.suffixFor(tea.getTitle(), tea.getScope().season())` solely to decide whether to append a year before slug normalization. Preserve normalization, reserved-slug, all-numeric, and length-limit behavior unchanged.

Update `TeaSlugTests` to use `Season` and retain slug-specific regression cases without duplicating the complete `TeaTitleYear` test matrix. Add or adjust a case proving that constructing a tea with a multi-year season is rejected before slug generation.

## 5. Add a social metadata read model

Add a small read-side type such as `tea/query/TeaSocialMetadata` with `title` and `description` fields and a factory from `TeaDetail`.

Title construction:

1. Start with `tea.title()`.
2. Always append the mandatory vendor in parentheses.
3. Convert nullable persisted season text to `Optional<Season>`; null means no season, while any present persisted text constructs a `Season`.
4. Call `TeaTitleYear.suffixFor(tea.title(), season)` and append `, YEAR` only when it returns a value.

This read path deliberately trusts the persisted season invariant and fails fast on invalid non-null data instead of silently hiding corruption. The corrected source data must therefore be fully re-imported before deploying this read path; follow the repository's documented destructive rebuild procedure. This removes the old multi-year Jade Star value and converts imported `N/A` sentinels to SQL `NULL`.

Description construction:

1. If `tea.name()` is null or blank, return `tea.description()` unchanged.
2. Otherwise return `tea.name() + ". " + tea.description()`.

Return the full combined description from `TeaSocialMetadata`. Keep abbreviation in Thymeleaf with `#strings.abbreviate(socialMetadata.description, 300)`, preserving the current library's exact truncation and ellipsis behavior while applying it to the complete combined value.

Add unit tests covering:

- Vendor and non-duplicated year.
- A year already present in the title.
- Absent and approximate seasons.
- Present, blank, and null technical names.
- Combined-description punctuation.

## 6. Wire metadata into the tea page

In `TeaQueryController.viewTeaBySlug`, create `TeaSocialMetadata` from the loaded `TeaDetail` and add it to the model.

Update `templates/tea-view.html` to use the computed values for:

- `og:title`
- `og:description`
- `twitter:title`
- `twitter:description`

Keep the existing `og:url`, `og:image`, `twitter:card`, canonical URL, trusted `PublicBaseUrl`, and browser `<title>` behavior unchanged unless explicitly deciding to align the browser title in a separate change.

Emit the description tags only when the computed description is nonblank. Explicit Twitter title and description tags avoid relying on Open Graph fallback behavior.

Use `${socialMetadata.title}` for both title tags. Guard both description tags with `!#strings.isEmpty(socialMetadata.description)` and use `${#strings.abbreviate(socialMetadata.description, 300)}` for both values; do not retain the old guard or abbreviation against `tea.description`.

## 7. Extend integration coverage

Update `integration/TeaViewIT` to assert the complete metadata values for representative teas:

- Luminary Misfit includes vendor, season year, and technical name.
- Shou Mei 2017 does not repeat `2017` in the card title.
- A tea with no season omits the year cleanly.
- A present approximate season such as `Early 2000s` also omits the exact year cleanly.
- Doubleshot's `Spring and Autumn 2022` contributes the single year `2022`.
- Simple Dreams 2's `15th April 2021` contributes `2021`.
- A tea with a blank technical name starts its card description directly with the normal description.
- Open Graph and Twitter tags carry identical title and description values.
- Existing canonical URL, image URL, hostile-host rejection, and card-type assertions remain green.

Use HTML-aware assertions or exact escaped fragments where practical so punctuation and conditional separators are covered.

## 8. Validate the complete change

Run checks in this order:

1. Focused `SeasonTests`, `TeaTitleYearTests`, `TeaSlugTests`, and `TeaSocialMetadataTests` while iterating.
2. Focused `TeaViewIT` integration test.
3. Re-run the same read-only PowerShell `ConvertFrom-Json` audit used during planning over all `teas/*/info.json` files with the exact standalone-year regex. Confirm zero multi-year seasons and zero JSON parse errors; this is a one-off validation command, not a new repository script.
4. Run the required full webapp gate from `webapp/`: `./mvnw.cmd clean verify`.
5. Verify edited and added files use LF line endings.
6. Review the final diff for accidental schema/template changes. Update `.github/instructions/webapp.instructions.md` to document `Season` alongside the other domain value objects, including meaningful-text preservation, `Optional<Season>` absence, approximate seasons without exact years, and the at-most-one-exact-year invariant.
7. Run the repository-required independent review sub-agent against the finished diff, asking specifically about correctness, security, unit and integration coverage, naming, CQRS boundaries, and idiomatic Java/Spring design. Address findings and rerun affected tests before considering the work complete.
8. Before production deployment, execute the documented backup and destructive rebuild workflow so all persisted season values satisfy the new invariant; smoke-test Jade Star 8 and representative exact-year and approximate-season tea pages afterward.
