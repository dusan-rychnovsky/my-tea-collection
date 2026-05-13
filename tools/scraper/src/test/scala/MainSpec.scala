import zio.*
import zio.test.*

object MainSpec extends ZIOSpecDefault:

  private val sampleHtml =
    """
      |<h1 class="product-info__title">Jade Star 9</h1>
      |<h2 class="product-info__subtitle">2008 Bai Mu Dan and Shou Mei</h2>
      |<dl class="product-detail">
      |  <dd><meta itemprop="name" content="Season"><span itemprop="value">Spring 2008</span></dd>
      |  <dd><meta itemprop="name" content="Cultivar"><span itemprop="value">Da Bai</span></dd>
      |  <dd><meta itemprop="name" content="Origin"><span itemprop="value">Fuding, Fujian, China</span></dd>
      |  <dd><meta itemprop="name" content="Elevation"><span itemprop="value">900m approx</span></dd>
      |</dl>
      |""".stripMargin

  private def fetchReturning(html: String): String => UIO[String] =
    _ => ZIO.succeed(html)

  private def fetchFailing(err: Throwable): String => Task[String] =
    _ => ZIO.fail(err)

  def spec = suite("Main")(
    test("prints parsed tea info from downloaded HTML") {
      val expected =
        """title: Jade Star 9
          |name: 2008 Bai Mu Dan and Shou Mei
          |season: Spring 2008
          |cultivar: Da Bai
          |origin: Fuding, Fujian, China
          |elevation: 900m approx
          |""".stripMargin
      for
        _      <- Main.program("http://example.com", fetchReturning(sampleHtml))
        output <- TestConsole.output
      yield assertTrue(output == Vector(expected))
    },
    test("propagates fetcher errors") {
      val err = RuntimeException("network down")
      Main.program("http://example.com", fetchFailing(err)).flip.map { e =>
        assertTrue(e.getMessage == "network down")
      }
    },
    test("propagates parser errors") {
      Main.program("http://example.com", fetchReturning("<html></html>")).flip.map { e =>
        assertTrue(e.isInstanceOf[ParseError])
      }
    }
  )
