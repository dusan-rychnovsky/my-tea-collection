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

- `domain/` — `TeaInfo`, `TeaType`, `Vendor`. Pure data. No dependency on `parser`. Rendering helpers (`renderTeaInfo`, `renderTeaType`, `renderVendor`) live in `domain/TeaInfo.scala` for now; they'll move to a dedicated `render` subpackage when a second rendering format is added.
- `parser/` — `Fetcher` (co-locates `HttpError`), `ParseError`, `Meileaf`, `Meetea`, `Scrape` (co-locates `UnsupportedVendorError`), `TeaTypeVocabulary`. HTTP fetch, HTML parsing, URL → parser routing. Depends on `domain`. Each error type is its own small case class living next to the function that produces it (single-producer errors are file-co-located; `ParseError` is standalone because both parsers produce it).
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
- **TeaType label parsing** lives in `parser/TeaTypeVocabulary.scala` (`lookupTeaType(label): Option[TeaType]`, case-insensitive against a single shared `teaTypeVocabulary: Map[String, TeaType]` with lowercase keys). Each parser uses `lookupTeaType` regardless of vendor — labels like `"Green Tea"`, `"green tea"`, `"Zelený čaj"` all live side-by-side in the vocabulary. Don't add per-vendor label maps and don't lean on `renderTeaType`'s output for parsing.
- **One parser per file** (`parser/Meileaf.scala`, `parser/Meetea.scala`) with distinct top-level names (`parseMeileafTea`, `parseMeeteaTea`). Each parser takes `(html: String, url: URL)` and returns the full `TeaInfo` (setting `vendor` to the right enum case and `url` to `url.encode`).
- **Native ZIO HTTP** (`zio-http`): use `ZClient.batched` + `Request.get` + `URL.decode`. Don't bridge from JDK's `HttpClient` with `fromCompletableFuture`.
- **HTML parsing via JSoup wrapped in `ZIO.attempt`**. The ZIO ecosystem has no native HTML parser; JSoup is the standard JVM choice. Wrap its (possibly throwing) calls in `ZIO.attempt`, then `refineToOrDie[ParseError]` so the public signature returns `IO[ParseError, TeaInfo]`. `ParseError` extends `RuntimeException(message)` so the parser can `throw ParseError(...)` directly inside `ZIO.attempt` as an early-exit; the case-class shape gives callers equality and pattern matching. Don't expose JSoup types in signatures — keep them inside the parser. Note the asymmetry with `HttpError`, which wraps an external `Throwable` cause via composition and doesn't itself extend `Throwable`: that's deliberate — only `ParseError` is constructed-then-thrown by our own code, the others are only ever propagated through ZIO's error channel.
- **Per-component typed errors, narrow per function, composed via union types.** Each component owns one error type that names its own failure modes — never a catch-all enum for the whole app. Concretely: `ParseError` (parsers), `HttpError` (Fetcher), `UnsupportedVendorError` (Scrape's vendor routing), `ArgError` (Main's CLI args). Each function returns only what it can legitimately produce: `fetch: ZIO[Client, HttpError, String]`; `parse<Vendor>Tea: IO[ParseError, TeaInfo]`; `scrape: ZIO[Client, HttpError | ParseError | UnsupportedVendorError, TeaInfo]`; `parseUrlArg: IO[ArgError, URL]`; `Main.program: ZIO[Client, ArgError | HttpError | ParseError | UnsupportedVendorError, Unit]`. Scala 3 union types compose them at the boundaries without dragging unrelated cases into narrower signatures. `Main.run` pattern-matches the full union exhaustively to render messages — adding a new error case to any component fails compilation there until you handle it, which is the whole point. Boundaries (zio-http throwables, JSoup throwables) translate with `.mapError` / `.refineToOrDie`; `Throwable` never appears in public signatures. `Client.default.orDie` lifts the layer's startup failure to a defect so it doesn't bleed into the error channel.
- **Optional fields**: `TeaInfo.origin/cultivar/season/elevation` are `Option[String]`. Different vendors surface different fields (e.g. meetea has no elevation). `renderTeaInfo` skips `None` lines. Other fields are required; `description`/`price`/`brewingInstructions` may be the literal string `"N/A"` until parsing is implemented (meileaf description, both vendors' price/brewing).
- **Integration tests need `@@ withLiveClock`** — otherwise ZIO Test's default `TestClock` warns about not advancing time during real HTTP calls.

## Gotchas

- **`sbt clean` after renaming top-level defs.** Scala 3 compiles top-level functions into synthetic `<file>$package` classes; sbt's incremental compiler can leave stale ones behind, causing `NoClassDefFoundError: <file>$package$` at runtime. A clean rebuild fixes it.
- **Don't put code back in the unnamed (default) package.** With sbt's default `ScalaLibrary` classloader layering, test classloaders can't see `<file>$package$` synthetics for top-level defs in the unnamed package from a cold cache, throwing `NoClassDefFoundError` on every test that calls a top-level function. Keeping everything under `cz.dusanrychnovsky.myteacollection.scraper.{domain,parser}` (or root for `Main`) avoids this entirely — that's why no source file omits its `package` line.
