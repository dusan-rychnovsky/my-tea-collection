# Scraper

Scala 3 + ZIO console app. Downloads a tea product page from a supported e-shop, parses it, and prints the key fields (title, name, season, cultivar, origin, elevation; missing fields are omitted).

## Stack

Scala 3.8.3 · ZIO 2.1.14 · zio-http 3.0.1 · JSoup 1.18.1 · sbt 1.11.5

## Supported vendors

| Vendor  | Host                 | Source file       |
| ------- | -------------------- | ----------------- |
| meileaf | `meileaf.com`        | `Meileaf.scala`   |
| meetea  | `store.meetea.cz`    | `Meetea.scala`    |

The registry lives in `Vendor.scala` as `val vendors: List[Vendor]`. Vendor selection is by exact URL host match.

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

## Conventions

- **Functional style fitting ZIO philosophy**: helpers like `fetch` and the per-vendor parsers are top-level functions, not methods on traits/objects. For testability, `Main.program` takes a `scrape: URL => ZIO[R, Throwable, TeaInfo]` function as a parameter — fake scrapers in tests are plain function values. Prefer ZIO idioms (`ZIO`, `Layer`, `zio-http`) over OOP abstractions.
- **Vendors are values, not classes**: each vendor is a `case class Vendor(name, host, scrape)` value composed via function composition (`fetch` + parser via `flatMap`). New vendors add a Scala file (`<Vendor>.scala` exposing `parse<Vendor>Tea` + `val <vendor>Vendor`) and register the value in the `vendors` list in `Vendor.scala`. No traits, no inheritance. Multi-page vendors chain more `fetch`es inside their `scrape` — no special abstraction needed.
- **One parser per file, distinct top-level names** (`parseMeileafTea`, `parseMeeteaTea`). Scala 3 compiles top-level defs into one synthetic `<file>$package` class per source file, so distinct names per vendor file avoid clashes and keep the source tree flat.
- **Native ZIO HTTP** (`zio-http`): use `ZClient.batched` + `Request.get` + `URL.decode`. Don't bridge from JDK's `HttpClient` with `fromCompletableFuture`.
- **HTML parsing via JSoup wrapped in `ZIO.attempt`**. The ZIO ecosystem has no native HTML parser; JSoup is the standard JVM choice. Wrap its (possibly throwing) calls in `ZIO.attempt` and `refineOrDie` to a typed error (`ParseError`). Don't expose JSoup types in signatures — keep them inside the parser.
- **Optional fields**: `TeaInfo.season/cultivar/origin/elevation` are `Option[String]`. Different vendors surface different fields (e.g. meetea has no elevation). `renderTeaInfo` skips `None` lines.
- **Integration tests need `@@ withLiveClock`** — otherwise ZIO Test's default `TestClock` warns about not advancing time during real HTTP calls.

## Gotchas

- **`sbt clean` after renaming top-level defs.** Scala 3 compiles top-level functions into synthetic `<file>$package` classes; sbt's incremental compiler can leave stale ones behind, causing `NoClassDefFoundError: <file>$package$` at runtime. A clean rebuild fixes it.
