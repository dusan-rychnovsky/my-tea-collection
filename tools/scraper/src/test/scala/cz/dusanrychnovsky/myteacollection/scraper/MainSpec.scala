package cz.dusanrychnovsky.myteacollection.scraper

import cz.dusanrychnovsky.myteacollection.scraper.parser.*

import zio.*
import zio.http.{Client, URL}
import zio.test.*

object MainSpec extends ZIOSpecDefault:

  def spec = suite("Main")(
    suite("parseUrlArg")(
      test("decodes the first arg as a URL") {
        Main.parseUrlArg(Chunk("https://meileaf.com/tea/tea-jtic/")).map { url =>
          assertTrue(url.host.contains("meileaf.com"))
        }
      },
      test("fails with MissingArg when args is empty") {
        Main.parseUrlArg(Chunk.empty).flip.map { err =>
          assertTrue(err == ArgError.MissingArg("Usage: scraper <url>"))
        }
      },
      test("fails with BadUrl when the arg isn't a valid URL") {
        Main.parseUrlArg(Chunk("not a url")).flip.map { err =>
          assertTrue(err match
            case ArgError.BadUrl("not a url", _) => true
            case _                               => false
          )
        }
      }
    ),
    suite("program")(
      test("fails with UnsupportedVendorError for an unknown host") {
        Main
          .program(Chunk("https://example.com/foo"))
          .provide(Client.default.orDie)
          .flip
          .map { err =>
            assertTrue(err == UnsupportedVendorError("example.com"))
          }
      },
      test("propagates MissingArg for empty args") {
        Main
          .program(Chunk.empty)
          .provide(Client.default.orDie)
          .flip
          .map { err =>
            assertTrue(err == ArgError.MissingArg("Usage: scraper <url>"))
          }
      }
    )
  )
