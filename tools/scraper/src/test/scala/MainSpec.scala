import zio.*
import zio.test.*

object MainSpec extends ZIOSpecDefault:

  private def fetchReturning(html: String): String => UIO[String] =
    _ => ZIO.succeed(html)

  private def fetchFailing(err: Throwable): String => Task[String] =
    _ => ZIO.fail(err)

  def spec = suite("Main")(
    test("prints HTML downloaded from URL") {
      for
        _      <- Main.program("http://example.com", fetchReturning("<html>hello</html>"))
        output <- TestConsole.output
      yield assertTrue(output == Vector("<html>hello</html>\n"))
    },
    test("propagates fetcher errors") {
      val err = RuntimeException("network down")
      Main.program("http://example.com", fetchFailing(err)).flip.map { e =>
        assertTrue(e.getMessage == "network down")
      }
    }
  )
