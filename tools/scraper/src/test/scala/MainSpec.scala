import zio.*
import zio.http.URL
import zio.test.*

object MainSpec extends ZIOSpecDefault:

  private val sampleInfo = TeaInfo(
    title = "Jade Star 9",
    name = "2008 Bai Mu Dan and Shou Mei",
    description = "N/A",
    types = Set(TeaType.White),
    vendor = Vendor.MeiLeaf,
    url = "https://example.com/x",
    season = Some("Spring 2008"),
    cultivar = Some("Da Bai"),
    origin = Some("Fuding, Fujian, China"),
    elevation = Some("900m approx"),
    price = "N/A",
    brewingInstructions = "N/A",
    inStock = true
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
          |description: N/A
          |types: White Tea
          |vendor: Mei Leaf
          |url: https://example.com/x
          |origin: Fuding, Fujian, China
          |cultivar: Da Bai
          |season: Spring 2008
          |elevation: 900m approx
          |price: N/A
          |brewingInstructions: N/A
          |inStock: true
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
    test("renders multiple types comma-separated, sorted") {
      val info = sampleInfo.copy(types = Set(TeaType.Green, TeaType.White))
      for
        _      <- Main.program(sampleUrl, scrapeReturning(info))
        output <- TestConsole.output
      yield assertTrue(output.headOption.exists(_.contains("types: Green Tea, White Tea")))
    },
    test("propagates scrape errors") {
      val err = RuntimeException("boom")
      Main.program(sampleUrl, scrapeFailing(err)).flip.map { e =>
        assertTrue(e.getMessage == "boom")
      }
    }
  )
