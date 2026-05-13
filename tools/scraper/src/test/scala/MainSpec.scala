import zio.*
import zio.http.URL
import zio.test.*

object MainSpec extends ZIOSpecDefault:

  private val sampleInfo = TeaInfo(
    title = "Jade Star 9",
    name = "2008 Bai Mu Dan and Shou Mei",
    season = Some("Spring 2008"),
    cultivar = Some("Da Bai"),
    origin = Some("Fuding, Fujian, China"),
    elevation = Some("900m approx")
  )

  private val sampleUrl: URL = URL.decode("https://example.com/x").toOption.get

  private def scrapeReturning(info: TeaInfo): URL => UIO[TeaInfo] =
    _ => ZIO.succeed(info)

  private def scrapeFailing(err: Throwable): URL => Task[TeaInfo] =
    _ => ZIO.fail(err)

  def spec = suite("Main")(
    test("prints rendered tea info from scrape result") {
      val expected =
        """title: Jade Star 9
          |name: 2008 Bai Mu Dan and Shou Mei
          |season: Spring 2008
          |cultivar: Da Bai
          |origin: Fuding, Fujian, China
          |elevation: 900m approx
          |""".stripMargin
      for
        _      <- Main.program(sampleUrl, scrapeReturning(sampleInfo))
        output <- TestConsole.output
      yield assertTrue(output == Vector(expected))
    },
    test("omits missing optional fields") {
      val info = sampleInfo.copy(elevation = None)
      for
        _      <- Main.program(sampleUrl, scrapeReturning(info))
        output <- TestConsole.output
      yield assertTrue(
        output.headOption.exists(_.contains("origin: Fuding, Fujian, China")),
        !output.headOption.exists(_.contains("elevation"))
      )
    },
    test("propagates scrape errors") {
      val err = RuntimeException("boom")
      Main.program(sampleUrl, scrapeFailing(err)).flip.map { e =>
        assertTrue(e.getMessage == "boom")
      }
    }
  )
