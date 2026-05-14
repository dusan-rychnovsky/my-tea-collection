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
      test("fails when args is empty") {
        Main.parseUrlArg(Chunk.empty).flip.map { err =>
          assertTrue(err.getMessage.contains("Usage"))
        }
      },
      test("fails when the arg isn't a valid URL") {
        Main.parseUrlArg(Chunk("not a url")).flip.map { err =>
          assertTrue(err != null)
        }
      }
    ),
    suite("program")(
      test("fails with UnsupportedVendor for an unknown host") {
        Main
          .program(Chunk("https://example.com/foo"))
          .provide(Client.default)
          .flip
          .map { err =>
            assertTrue(err.isInstanceOf[UnsupportedVendor], err.getMessage.contains("example.com"))
          }
      },
      test("propagates the Usage error for empty args") {
        Main
          .program(Chunk.empty)
          .provide(Client.default)
          .flip
          .map { err =>
            assertTrue(err.getMessage.contains("Usage"))
          }
      }
    )
  )
