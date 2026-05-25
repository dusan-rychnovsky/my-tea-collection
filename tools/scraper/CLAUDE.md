# Scraper

Scala 3 + ZIO console app. Downloads a tea product page from a supported e-shop, parses it, and prints the key fields (`title`, `name`, `description`, `types`, `vendor`, `url`, `origin`, `cultivar`, `season`, `elevation`, `price`, `brewingInstructions`, `inStock`). Optional fields (`origin`/`cultivar`/`season`/`elevation`) are omitted when missing; other fields are always present (`price`/`brewingInstructions`/`description` may be `"N/A"` while parsing isn't implemented yet).

## Stack

Scala 3.8.3 · ZIO 2.1.14 · zio-http 3.0.1 · JSoup 1.18.1 · sbt 1.11.5

## Supported vendors

| Enum case        | Display name | Host              | Parser source          |
| ---------------- | ------------ | ----------------- | ---------------------- |
| `Vendor.MeiLeaf` | `Mei Leaf`   | `meileaf.com`     | `parser/Meileaf.scala` |
| `Vendor.Meetea`  | `Meetea`     | `store.meetea.cz` | `parser/Meetea.scala`  |

`Vendor` itself (in `domain/Vendor.scala`) is a pure tag enum — no `displayName`, no `host`, no methods. Display names live in `renderVendor` (in `domain/TeaInfo.scala`). The full URL-to-`TeaInfo` pipeline lives behind one public entry point: `scrape(url): ZIO[Client, HttpError | ParseError | UnsupportedVendorError, TeaInfo]` in `parser/Scrape.scala`. Internally `scrape` literal-matches on `url.host` and dispatches straight to the right parser — there's no separate `selectVendor` step, because callers (e.g. `Main`) don't need the intermediate `Vendor` tag.

## Package layout

Everything lives under `cz.dusanrychnovsky.myteacollection.scraper`, split into three groups:

- `domain/` — `TeaInfo`, `TeaType`, `Vendor`, plus opaque-type wrappers `Title`, `Name`, `Description`, `Elevation` (each in its own file; `Elevation` wraps `Int`, the others wrap `String`). Pure data. No dependency on `parser`. Rendering helpers (`renderTeaInfo`, `renderTeaType`, `renderVendor`) live in `domain/TeaInfo.scala` for now; they'll move to a dedicated `render` subpackage when a second rendering format is added.
- `parser/` — `Fetcher` (co-locates `HttpError`), `HtmlParsing` (co-locates `ParseError` + shared JSoup helpers `parseElementText`, `parseAttributeText`, `parseElement`, `cleanText`), `Meileaf`, `Meetea`, `Scrape` (co-locates `UnsupportedVendorError`), `TeaTypeVocabulary` (co-locates `lookupTeaType` + its ZIO wrapper `resolveTeaType`). HTTP fetch, HTML parsing, URL → parser routing. Depends on `domain`. Each error type is its own small case class living next to the function(s) that produce it; `ParseError` lives in `HtmlParsing` because the shared element-extraction helpers are its primary producers (the vendor parsers and `resolveTeaType` produce it too).
- (root) — `Main` (co-locates `ArgError`). Imports both `domain.*` and `parser.*`. Its `program` is now just `parseUrlArg` → `scrape` → `renderTeaInfo`; it never sees a `Vendor` value. `ArgError` covers CLI-arg parsing failures (`MissingArg`, `BadUrl`); other error types live with their producers in `parser/`.

## Running

URLs need a scheme and must point at a product page of a supported vendor. Quote the whole sbt command so your shell doesn't split args:

```
sbt "run https://meileaf.com/tea/tea-jtic/"
sbt "run https://store.meetea.cz/zeleny-caj/heritage-green-2026/"
```

Or use the sbt shell, where no quoting is needed:

```
sbt
> run https://meileaf.com/tea/tea-jtic/
```

## Testing

Integration tests (those hitting real URLs) live in `src/test/scala/` alongside unit tests and are tagged `@@ tag("integration")`.

- `sbt test` — all tests (~6s, requires network)
- `sbt "testOnly -- -ignore-tags integration"` — units only (~1s, offline)
- `sbt "testOnly -- -tags integration"` — integration only

## Formatting

scalafmt (via `sbt-scalafmt`) is the canonical formatter. Config lives at `.scalafmt.conf`.

- `sbt scalafmtAll scalafmtSbt` — format all sources (incl. `build.sbt`)
- `sbt scalafmtCheckAll` — verify formatting without modifying files (use in CI)

Format on save in IntelliJ/Metals also reads `.scalafmt.conf`.

## Conventions

- **Functional style fitting ZIO philosophy**: helpers like `fetch` and the per-vendor parsers are top-level functions, not methods on traits/objects. `Main.program(args)` owns the full business flow (arg parsing → URL decode → `scrape` → print) so it's fully exercisable from tests; `run` is a thin shell that provides `Client.default.orDie` and pretty-prints failures via `tapError`. `Main` never sees a `Vendor` — that's an internal concept of `scrape`. Parser-level fakes aren't needed because `parse<Vendor>Tea` and `renderTeaInfo` are pure functions with their own direct specs. Prefer ZIO idioms (`ZIO`, `Layer`, `zio-http`) over OOP abstractions.
- **Vendor + TeaType are pure tag enums** in `domain/`. Neither carries any property — no `displayName`, no `host`, no taxonomy beyond what each enum literally needs (`TeaType` has a `parent: Option[TeaType]` for its hierarchy; `Vendor` has nothing). Everything around them — display strings, host routing, parser dispatch, label parsing — lives outside the enum in pure functions. Adding a vendor: add a `parser/<Vendor>.scala` exposing `parse<Vendor>Tea(html, url)`, add an enum case to `Vendor`, then add two branches (one in `renderVendor` in `domain/TeaInfo.scala`, one in `scrape` in `parser/Scrape.scala` — the latter handles both the host literal and the parser dispatch in a single match). Adding a tea type: add the enum case, add a `renderTeaType` branch, and add at least one entry in `teaTypeVocabulary`. Match exhaustiveness catches missing render branches at compile time; the `scrape` host branches and the vocabulary entry are the pieces the compiler can't enforce.
- **Why pure tag enums:** rendering, parsing, and routing are all separate concerns and we want them to stay separable so that future ones (JSON output, more vendors, fuzzy host matching) don't have to touch `domain`. (Scala 3 enums don't support per-case method bodies anyway, so co-locating these concerns on the enum was never an option.) Don't put `displayName`, `host`, or `scrape` back on `Vendor`; don't put `displayName` back on `TeaType`. If you need a new piece of per-case data, add a new top-level function with an exhaustive match.
- **TeaType label parsing** lives in `parser/TeaTypeVocabulary.scala` (`lookupTeaType(label): Option[TeaType]`, case-insensitive against a single shared `teaTypeVocabulary: Map[String, TeaType]` with lowercase keys, plus `resolveTeaType(label): IO[ParseError, TeaType]` which is the ZIO-channel wrapper both parsers use). Each parser uses `lookupTeaType`/`resolveTeaType` regardless of vendor — labels like `"Green Tea"`, `"green tea"`, `"Zelený čaj"` all live side-by-side in the vocabulary. Don't add per-vendor label maps and don't lean on `renderTeaType`'s output for parsing.
- **One parser per file** (`parser/Meileaf.scala`, `parser/Meetea.scala`) with distinct top-level names (`parseMeileafTea`, `parseMeeteaTea`). Each parser takes `(html: String, url: URL)` and returns the full `TeaInfo` (setting `vendor` to the right enum case and `url` to the `URL` it was handed — no `.encode` at the parser layer; `renderTeaInfo` calls `.encode` when printing). Both parsers reuse the shared `parseElementText`/`parseAttributeText` helpers from `HtmlParsing` for single-element JSoup extraction; multi-element patterns (e.g. building a `Map` from a list of `dd` elements, or pulling the second item from a breadcrumb list) stay inline because they don't fit the `selectFirst`-based helper shape, and inventing a multi-element helper for a single site would be premature.
- **Native ZIO HTTP** (`zio-http`): use `ZClient.batched` + `Request.get` + `URL.decode`. Don't bridge from JDK's `HttpClient` with `fromCompletableFuture`.
- **Functional error handling — no exceptions for domain errors.** Domain errors are values returned through ZIO's error channel (or `Either`), never thrown. Compose with `ZIO.fromOption(...).orElseFail(SomeError(...))` (or `Either` + for-comprehension) instead of `throw`. Domain error types like `ParseError`, `HttpError`, `UnsupportedVendorError`, `ArgError` are plain case classes and do **not** extend `Throwable` — they're data, not exceptions. The only allowed `throw` is something the JVM/library does on its own behind a boundary we wrap (e.g. JSoup throwables caught by `ZIO.attempt(...).orDie`); we never throw our own error types. This is the general preferred style for Scala/ZIO code in this repo.
- **HTML parsing via JSoup wrapped in `ZIO.attempt(...).orDie`**. The ZIO ecosystem has no native HTML parser; JSoup is the standard JVM choice. Wrap `Jsoup.parse(html)` in `ZIO.attempt(...).orDie` so any unexpected JVM throwable becomes a defect, then thread the parsed `Document` through a ZIO for-comprehension that uses `ZIO.fromOption(...).orElseFail(ParseError(...))` at each potential failure point. Don't expose JSoup types in signatures — keep them inside the parser.
- **Per-component typed errors, narrow per function, composed via union types.** Each component owns one error type that names its own failure modes — never a catch-all enum for the whole app. Concretely: `ParseError` (parsers), `HttpError` (Fetcher), `UnsupportedVendorError` (Scrape's vendor routing), `ArgError` (Main's CLI args). Each function returns only what it can legitimately produce: `fetch: ZIO[Client, HttpError, String]`; `parse<Vendor>Tea: IO[ParseError, TeaInfo]`; `scrape: ZIO[Client, HttpError | ParseError | UnsupportedVendorError, TeaInfo]`; `parseUrlArg: IO[ArgError, URL]`; `Main.program: ZIO[Client, ArgError | HttpError | ParseError | UnsupportedVendorError, Unit]`. Scala 3 union types compose them at the boundaries without dragging unrelated cases into narrower signatures. Boundaries translate Throwables out at the edges: zio-http throwables via `.mapError(HttpError(_))`, JSoup throwables via `.orDie` (defect, since they should never happen with hardcoded selectors). `Throwable` never appears in public signatures, and our own error types never extend `Throwable`. `Client.default.orDie` lifts the layer's startup failure to a defect so it doesn't bleed into the error channel.
- **Common `ScraperError` trait carries user-facing rendering.** All app error case classes/enums extend `ScraperError` (in `scraper/ScraperError.scala`) and implement `def message: String` — each error knows how to render itself for the user. `Main.run` doesn't pattern-match the union; it just calls `err.message` and prints it via `Console.printLineError`. Defects (anything outside the error channel) crash the app as ZIO defaults — that's the desired behavior. The trait is for *rendering only*, not for replacing narrow per-function error types: functions still return their specific error (e.g. `IO[ParseError, TeaInfo]`), never `IO[ScraperError, TeaInfo]`. When you add a new error type, extending `ScraperError` is what the compiler enforces (abstract `message` must be implemented) — and union-typed call sites automatically pick up the new member without a pattern-match update. Note: for `ParseError(message: String)` the case-class field accessor auto-implements the trait; for cases without a literal `message` field (`BadUrl`, `HttpError`, `UnsupportedVendorError`), provide an explicit `def message: String`. Don't name a constructor param `message` on a type that needs a derived render (e.g. `ArgError.MissingArg` uses `usage: String` to avoid clashing with the inherited method).
- **Optional fields**: `TeaInfo.origin/cultivar/season` are `Option[String]`; `elevation` is `Option[Elevation]`. Different vendors surface different fields (e.g. meetea has no elevation). `renderTeaInfo` skips `None` lines and formats `Elevation` as `"{n}m"`. Other fields are required; `description` (a `Description`), `price`, `brewingInstructions` may be the literal string `"N/A"` (wrapped in `Description` where the type demands it) until parsing is implemented.
- **Opaque-type wrappers in `domain/`** for fields that would otherwise be primitives: `Title`, `Name`, `Description` (over `String`), `Elevation` (over `Int`), plus the structured `URL` from `zio.http` for `TeaInfo.url`. Each opaque type lives in its own file under `domain/`, has an `apply(value): Self` smart constructor and a `value` extension method (returning the underlying type). They currently perform no validation — they're nominal wrappers that stop you from passing a title where a name is expected. Constructors are explicit (`Title("…")`, `Elevation(900)`), no implicit conversions. To add a new wrapper, copy `domain/Title.scala`. Don't put validation into these until there's a real second case forcing it. `price` and `brewingInstructions` stay as `String` for now because they're placeholders; promote them when parsing lands.
- **String → typed-value parsing lives next to its only caller for now.** Cleaning a raw HTML value into a typed domain value (e.g. `"900m approx"` → `Elevation(900)`) is a parser-layer concern, so the helper sits in the vendor parser that uses it (`parseElevation` in `parser/Meileaf.scala` — top-of-file `private`, regex-extracts the first run of digits). When a second vendor needs the same shape, lift the helper to a shared parser file. Don't preemptively put `parse` factories on the domain opaque types — keep them as nominal wrappers; parsing concerns can grow on the parser side.
- **Integration tests need `@@ withLiveClock`** — otherwise ZIO Test's default `TestClock` warns about not advancing time during real HTTP calls.

## Gotchas

- **`sbt clean` after renaming top-level defs.** Scala 3 compiles top-level functions into synthetic `<file>$package` classes; sbt's incremental compiler can leave stale ones behind, causing `NoClassDefFoundError: <file>$package$` at runtime. A clean rebuild fixes it.
- **Don't put code back in the unnamed (default) package.** With sbt's default `ScalaLibrary` classloader layering, test classloaders can't see `<file>$package$` synthetics for top-level defs in the unnamed package from a cold cache, throwing `NoClassDefFoundError` on every test that calls a top-level function. Keeping everything under `cz.dusanrychnovsky.myteacollection.scraper.{domain,parser}` (or root for `Main`) avoids this entirely — that's why no source file omits its `package` line.
