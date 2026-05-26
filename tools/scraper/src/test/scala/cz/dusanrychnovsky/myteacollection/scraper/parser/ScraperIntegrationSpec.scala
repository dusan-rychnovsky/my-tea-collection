package cz.dusanrychnovsky.myteacollection.scraper.parser

import cz.dusanrychnovsky.myteacollection.scraper.domain.*

import zio.*
import zio.http.*
import zio.test.*
import zio.test.TestAspect.*

object ScraperIntegrationSpec extends ZIOSpecDefault:

  private def scrapeUrl(raw: String)
    : ZIO[Client, HttpError | ParseError | UnsupportedVendorError, TeaInfo] =
    for
      url  <- ZIO.fromEither(URL.decode(raw)).orDie
      info <- scrape(url)
    yield info

  def spec = suite("Scraper integration")(
    test("parses Jade Star 9 from meileaf.com") {
      val raw      = "https://meileaf.com/tea/tea-jtic/"
      val expected = URL.decode(raw).toOption.get
      scrapeUrl(raw).map { info =>
        assertTrue(
          info == TeaInfo(
            title = Title("Jade Star 9"),
            name = Name("2008 Bai Mu Dan and Shou Mei"),
            description = Description("N/A"),
            types = Set(TeaType.WhiteTea),
            vendor = Vendor.MeiLeaf,
            url = expected,
            season = Some(Season.SeasonOfYear(SeasonName.Spring, 2008)),
            cultivar = Some("Da Bai"),
            origin = Some(Location("China", "Fujian", "Fuding")),
            elevation = Some(Elevation(900)),
            price = "N/A",
            brewingInstructions = "N/A",
            inStock = true
          )
        )
      }
    },
    test("parses Heritage Green 2026 from store.meetea.cz") {
      val raw      = "https://store.meetea.cz/zeleny-caj/heritage-green-2026/"
      val expected = URL.decode(raw).toOption.get
      scrapeUrl(raw).map { info =>
        assertTrue(
          info == TeaInfo(
            title = Title("Heritage Green 2026"),
            name = Name("Móc Câu Thái Nguyên 2026"),
            description = Description(
              "Robustní, ale elegantní zelený čaj s příjemně hořko-sladkou chutí a vůní připomínající trávu, hrášek, Pak Choi a kukuřici, s velmi dlouhou a lehce slanou dochutí."
            ),
            types = Set(TeaType.GreenTea),
            vendor = Vendor.Meetea,
            url = expected,
            season = Some(Season.MonthOfYear(Month.March, 2026)),
            cultivar = Some("Trung Du – vypěstováno ze semínek"),
            origin = Some(Location("Vietnam", "Thái Nguyên")),
            elevation = None,
            price = "N/A",
            brewingInstructions = "N/A",
            inStock = true
          )
        )
      }
    }
  ).provide(Client.default) @@ withLiveClock @@ tag("integration")
