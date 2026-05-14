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
      val raw = "https://meileaf.com/tea/tea-jtic/"
      scrapeUrl(raw).map { info =>
        assertTrue(
          info == TeaInfo(
            title = "Jade Star 9",
            name = "2008 Bai Mu Dan and Shou Mei",
            description = "N/A",
            types = Set(TeaType.White),
            vendor = Vendor.MeiLeaf,
            url = raw,
            season = Some("Spring 2008"),
            cultivar = Some("Da Bai"),
            origin = Some("Fuding, Fujian, China"),
            elevation = Some("900m approx"),
            price = "N/A",
            brewingInstructions = "N/A",
            inStock = true
          )
        )
      }
    },
    test("parses Heritage Green 2026 from store.meetea.cz") {
      val raw = "https://store.meetea.cz/zeleny-caj/heritage-green-2026/"
      scrapeUrl(raw).map { info =>
        assertTrue(
          info == TeaInfo(
            title = "Heritage Green 2026",
            name = "Móc Câu Thái Nguyên 2026",
            description =
              "Robustní, ale elegantní zelený čaj s příjemně hořko-sladkou chutí a vůní připomínající trávu, hrášek, Pak Choi a kukuřici, s velmi dlouhou a lehce slanou dochutí.",
            types = Set(TeaType.Green),
            vendor = Vendor.Meetea,
            url = raw,
            season = Some("Březen 2026"),
            cultivar = Some("Trung Du – vypěstováno ze semínek"),
            origin = Some("Thái Nguyên, Vietnam"),
            elevation = None,
            price = "N/A",
            brewingInstructions = "N/A",
            inStock = true
          )
        )
      }
    }
  ).provide(Client.default) @@ withLiveClock @@ tag("integration")
