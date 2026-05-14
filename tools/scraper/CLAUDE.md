# Scraper

Scala 3 + ZIO console app. Downloads a tea product page from a supported e-shop, parses it, and prints the key fields (`title`, `name`, `description`, `types`, `vendor`, `url`, `origin`, `cultivar`, `season`, `elevation`, `price`, `brewingInstructions`, `inStock`). Optional fields (`origin`/`cultivar`/`season`/`elevation`) are omitted when missing; other fields are always present (`price`/`brewingInstructions`/`description` may be `"N/A"` while parsing isn't implemented yet).

## Stack

Scala 3.8.3 · ZIO 2.1.14 · zio-http 3.0.1 · JSoup 1.18.1 · sbt 1.11.5

## Supported vendors

| Enum case        | Display name | Host              | Source file     |
| ---------------- | ------------ | ----------------- | --------------- |
| `Vendor.MeiLeaf` | `Mei Leaf`   | `meileaf.com`     | `Meileaf.scala` |
| `Vendor.Meetea`  | `Meetea`     | `store.meetea.cz` | `Meetea.scala`  |

Vendors are a Scala 3 `enum` in `Vendor.scala`, each carrying `displayName`, `host`, and a `scrape(url)` method that dispatches to the right parser. `selectVendor` matches by exact URL host.

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

- **Functional style fitting ZIO philosophy**: helpers like `fetch` and the per-vendor parsers are top-level functions, not methods on traits/objects. For testability, `Main.program` takes a `scrape: URL => ZIO[R, Throwable, TeaInfo]` function as a parameter — fake scrapers in tests are plain function values. Prefer ZIO idioms (`ZIO`, `Layer`, `zio-http`) over OOP abstractions.
- **Vendor + TeaType are Scala 3 enums** (`Vendor.scala`, `TeaType.scala`). Each `Vendor` case carries `displayName`, `host`, and a `scrape` method that composes `fetch` + the per-vendor parser. Adding a vendor: add a `<Vendor>.scala` exposing `parse<Vendor>Tea(html, url)`, then add an enum case to `Vendor` and a branch in `scrape`. `TeaType` is a small enum with a `displayName` (e.g. `"White Tea"`); extend it when a new vendor surfaces a new type. No traits, no inheritance.
- **One parser per file, distinct top-level names** (`parseMeileafTea`, `parseMeeteaTea`). Scala 3 compiles top-level defs into one synthetic `<file>$package` class per source file, so distinct names per vendor file avoid clashes and keep the source tree flat. Each parser takes `(html: String, url: URL)` and returns the full `TeaInfo` (setting `vendor` to the right enum case and `url` to `url.encode`).
- **Native ZIO HTTP** (`zio-http`): use `ZClient.batched` + `Request.get` + `URL.decode`. Don't bridge from JDK's `HttpClient` with `fromCompletableFuture`.
- **HTML parsing via JSoup wrapped in `ZIO.attempt`**. The ZIO ecosystem has no native HTML parser; JSoup is the standard JVM choice. Wrap its (possibly throwing) calls in `ZIO.attempt` and `refineOrDie` to a typed error (`ParseError`). Don't expose JSoup types in signatures — keep them inside the parser.
- **Optional fields**: `TeaInfo.origin/cultivar/season/elevation` are `Option[String]`. Different vendors surface different fields (e.g. meetea has no elevation). `renderTeaInfo` skips `None` lines. Other fields are required; `description`/`price`/`brewingInstructions` may be the literal string `"N/A"` until parsing is implemented (meileaf description, both vendors' price/brewing).
- **Integration tests need `@@ withLiveClock`** — otherwise ZIO Test's default `TestClock` warns about not advancing time during real HTTP calls.

## Gotchas

- **`sbt clean` after renaming top-level defs.** Scala 3 compiles top-level functions into synthetic `<file>$package` classes; sbt's incremental compiler can leave stale ones behind, causing `NoClassDefFoundError: <file>$package$` at runtime. A clean rebuild fixes it.
