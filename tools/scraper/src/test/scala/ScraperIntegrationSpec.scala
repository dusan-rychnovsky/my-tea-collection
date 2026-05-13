import zio.*
import zio.http.*
import zio.test.*
import zio.test.TestAspect.*

object ScraperIntegrationSpec extends ZIOSpecDefault:
  def spec = suite("Scraper integration")(
    test("parses Jade Star 9 from meileaf.com") {
      for
        html <- fetch("https://meileaf.com/tea/tea-jtic/")
        info <- parseTea(html)
      yield assertTrue(
        info == TeaInfo(
          title    = "Jade Star 9",
          name     = "2008 Bai Mu Dan and Shou Mei",
          season   = "Spring 2008",
          cultivar = "Da Bai",
          origin   = "Fuding, Fujian, China",
          elevation = "900m approx"
        )
      )
    }
  ).provide(Client.default) @@ withLiveClock @@ tag("integration")
