# Scraper

Scala 3 + ZIO console app. Will grow into a scraper that downloads tea info from the internet. Currently fetches a URL and prints its HTML.

## Stack

Scala 3.8.3 · ZIO 2.1.14 · zio-http 3.0.1 · sbt 1.11.5

## Running

URLs need a scheme. Quote the whole sbt command so your shell doesn't split args:

```
sbt "run https://example.com"
```

Or use the sbt shell, where no quoting is needed:

```
sbt
> run https://example.com
```

## Testing

Integration tests (those hitting real URLs) live in `src/test/scala/` alongside unit tests and are tagged `@@ tag("integration")`.

- `sbt test` — all tests (~6s, requires network)
- `sbt "testOnly -- -ignore-tags integration"` — units only (~1s, offline)
- `sbt "testOnly -- -tags integration"` — integration only

## Conventions

- **Functional style fitting ZIO philosophy**: helpers like `fetch` are top-level functions, not methods on traits/objects. For testability, `Main.program` takes the fetch function as a parameter — fake fetchers in tests are plain function values. Prefer ZIO idioms (`ZIO`, `Layer`, `zio-http`) over OOP abstractions.
- **Native ZIO HTTP** (`zio-http`): use `ZClient.batched` + `Request.get` + `URL.decode`. Don't bridge from JDK's `HttpClient` with `fromCompletableFuture`.
- **Integration tests need `@@ withLiveClock`** — otherwise ZIO Test's default `TestClock` warns about not advancing time during real HTTP calls.

## Gotchas

- **`sbt clean` after renaming top-level defs.** Scala 3 compiles top-level functions into synthetic `<file>$package` classes; sbt's incremental compiler can leave stale ones behind, causing `NoClassDefFoundError: <file>$package$` at runtime. A clean rebuild fixes it.
