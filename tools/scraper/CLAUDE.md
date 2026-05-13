# Scraper

Scala 3 + ZIO console app. Downloads a tea product page from meileaf.com, parses it, and prints the key fields (title, name, season, cultivar, origin, elevation).

## Stack

Scala 3.8.3 · ZIO 2.1.14 · zio-http 3.0.1 · JSoup 1.18.1 · sbt 1.11.5

## Running

URLs need a scheme and must point at a meileaf.com tea product page. Quote the whole sbt command so your shell doesn't split args:

```
sbt "run https://meileaf.com/tea/tea-jtic/"
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

- **Functional style fitting ZIO philosophy**: helpers like `fetch` and `parseTea` are top-level functions, not methods on traits/objects. For testability, `Main.program` takes the fetch function as a parameter — fake fetchers in tests are plain function values. Prefer ZIO idioms (`ZIO`, `Layer`, `zio-http`) over OOP abstractions.
- **Native ZIO HTTP** (`zio-http`): use `ZClient.batched` + `Request.get` + `URL.decode`. Don't bridge from JDK's `HttpClient` with `fromCompletableFuture`.
- **HTML parsing via JSoup wrapped in `ZIO.attempt`**. The ZIO ecosystem has no native HTML parser; JSoup is the standard JVM choice. Wrap its (possibly throwing) calls in `ZIO.attempt` and `refineOrDie` to a typed error (`ParseError`). Don't expose JSoup types in signatures — keep them inside the parser.
- **Integration tests need `@@ withLiveClock`** — otherwise ZIO Test's default `TestClock` warns about not advancing time during real HTTP calls.

## Gotchas

- **`sbt clean` after renaming top-level defs.** Scala 3 compiles top-level functions into synthetic `<file>$package` classes; sbt's incremental compiler can leave stale ones behind, causing `NoClassDefFoundError: <file>$package$` at runtime. A clean rebuild fixes it.
