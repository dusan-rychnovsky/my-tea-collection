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
    test("parses Esence 2014 from store.meetea.cz") {
      val raw      = "https://store.meetea.cz/shu-puer/esence-2014/"
      val expected = URL.decode(raw).toOption.get
      scrapeUrl(raw).map { info =>
        assertTrue(
          info == TeaInfo(
            title = Title("Esence 2014"),
            name = Name("2014 Haiwan Laochatou 9908 Fangcha"),
            description = Description(
              "Likérový, hřejivý shu puer s hustě sladkou chutí a vůní připomínající vanilkový pudink, višně a třtinový cukr, s dlouhou sametovou dochutí."
            ),
            types = Set(TeaType.ShuPuerh),
            vendor = Vendor.Meetea,
            url = expected,
            season = Some(Season.Year(2014)),
            cultivar = Some("Dayezhong - Velkolistý čajovník, staré stromy"),
            origin = Some(Location("Čína", "provincie Yunnan", "Menghai")),
            elevation = None,
            price = "N/A",
            brewingInstructions = "N/A",
            inStock = true
          )
        )
      }
    }
  ).provide(Client.default) @@ withLiveClock @@ tag("integration")
