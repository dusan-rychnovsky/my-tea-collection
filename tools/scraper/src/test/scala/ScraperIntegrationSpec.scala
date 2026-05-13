import zio.*
import zio.http.*
import zio.test.*
import zio.test.TestAspect.*

object ScraperIntegrationSpec extends ZIOSpecDefault:

  private def scrapeUrl(raw: String): ZIO[Client, Throwable, TeaInfo] =
    for
      url    <- ZIO.fromEither(URL.decode(raw))
      vendor <- selectVendor(url)
      info   <- vendor.scrape(url)
    yield info

  def spec = suite("Scraper integration")(
    test("parses Jade Star 9 from meileaf.com") {
      scrapeUrl("https://meileaf.com/tea/tea-jtic/").map { info =>
        assertTrue(
          info == TeaInfo(
            title     = "Jade Star 9",
            name      = "2008 Bai Mu Dan and Shou Mei",
            season    = Some("Spring 2008"),
            cultivar  = Some("Da Bai"),
            origin    = Some("Fuding, Fujian, China"),
            elevation = Some("900m approx")
          )
        )
      }
    },
    test("parses Heritage Green 2026 from store.meetea.cz") {
      scrapeUrl("https://store.meetea.cz/zeleny-caj/heritage-green-2026/").map { info =>
        assertTrue(
          info == TeaInfo(
            title     = "Heritage Green 2026",
            name      = "Móc Câu Thái Nguyên 2026",
            season    = Some("Březen 2026"),
            cultivar  = Some("Trung Du – vypěstováno ze semínek"),
            origin    = Some("Thái Nguyên, Vietnam"),
            elevation = None
          )
        )
      }
    }
  ).provide(Client.default) @@ withLiveClock @@ tag("integration")
