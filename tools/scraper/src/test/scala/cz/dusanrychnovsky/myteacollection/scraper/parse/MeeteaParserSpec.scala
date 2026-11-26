package cz.dusanrychnovsky.myteacollection.scraper.parse

import cz.dusanrychnovsky.myteacollection.scraper.domain.*

import zio.*
import zio.http.URL
import zio.test.*

object MeeteaParserSpec extends ZIOSpecDefault:

  private val sampleUrl: URL =
    URL.decode("https://store.meetea.cz/zeleny-caj/heritage-green-2026/").toOption.get

  private val sampleHtml =
    """
      |<html>
      |  <body>
      |    <div class="p-detail" itemscope itemtype="https://schema.org/Product">
      |      <meta itemprop="name" content="Heritage Green 2026" />
      |      <div class="p-detail-inner">
      |        <h1>  Heritage Green 2026</h1>
      |        <div class="p-short-description">
      |          <p><span style="color: #db3e39;">Móc Câu Thái Nguyên 2026<br /></span></p>
      |          <p><span>Robustní, ale elegantní zelený čaj s příjemně hořko-sladkou chutí a vůní připomínající trávu, hrášek, Pak Choi a kukuřici, s velmi dlouhou a lehce slanou dochutí.</span></p>
      |          <hr />
      |          <p><span><strong>Původ:</strong> Thái Nguyên, Vietnam<br /><strong>Odrůda:</strong> Trung Du – vypěstováno ze semínek<br /></span><span><strong>Sklizeň:</strong>&nbsp;Březen 2026<br /><strong>Druh podle zpracování:</strong> Zelený čaj</span></p>
      |        </div>
      |      </div>
      |    </div>
      |  </body>
      |</html>
      |""".stripMargin

  def spec = suite("Meetea parser")(
    test("extracts tea info from meetea-shaped HTML") {
      parseMeeteaTea(sampleHtml, sampleUrl).map { info =>
        assertTrue(
          info == TeaInfo(
            title = Title("Heritage Green 2026"),
            name = Name("Móc Câu Thái Nguyên 2026"),
            description = Description(
              "Robustní, ale elegantní zelený čaj s příjemně hořko-sladkou chutí a vůní připomínající trávu, hrášek, Pak Choi a kukuřici, s velmi dlouhou a lehce slanou dochutí."
            ),
            types = Set(TeaType.GreenTea),
            vendor = Vendor.Meetea,
            url = sampleUrl,
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
    },
    test("parses the harvest year from Doba sklizně") {
      val html = sampleHtml
        .replace("Sklizeň:", "Doba sklizně:")
        .replace("Březen 2026", "2014")
      parseMeeteaTea(html, sampleUrl).map { info =>
        assertTrue(info.season.contains(Season.Year(2014)))
      }
    },
    test("prefers Sklizeň when both harvest labels are present") {
      val html = sampleHtml.replace(
        "<strong>Sklizeň:</strong>",
        "<strong>Doba sklizně:</strong> 2014<br /><strong>Sklizeň:</strong>"
      )
      parseMeeteaTea(html, sampleUrl).map { info =>
        assertTrue(info.season.contains(Season.MonthOfYear(Month.March, 2026)))
      }
    },
    test("leaves season empty when neither harvest label is present") {
      val html = sampleHtml.replace("<strong>Sklizeň:</strong>&nbsp;Březen 2026<br />", "")
      parseMeeteaTea(html, sampleUrl).map { info =>
        assertTrue(info.season.isEmpty)
      }
    },
    test("recognizes shu puer within a contextual tea type label") {
      val html = sampleHtml.replace("Zelený čaj", "Tmavý čaj (Shu Puer)")
      parseMeeteaTea(html, sampleUrl).map { info =>
        assertTrue(info.types == Set(TeaType.ShuPuerh))
      }
    },
    test("fails with ParseError when the tea type label is ambiguous") {
      val html = sampleHtml.replace("Zelený čaj", "Zelený čaj / Oolong")
      parseMeeteaTea(html, sampleUrl).flip.map { err =>
        assertTrue(err == ParseError("ambiguous tea type: Zelený čaj / Oolong"))
      }
    },
    test("ignores unknown <strong> labels (besides Druh podle zpracování)") {
      parseMeeteaTea(sampleHtml, sampleUrl).map { info =>
        assertTrue(
          info.season.contains(Season.MonthOfYear(Month.March, 2026))
        )
      }
    },
    test("fails with ParseError when title meta is missing") {
      val html =
        """<html><body>
          |  <div class="p-detail">
          |    <div class="p-short-description">
          |      <p><span>x</span></p>
          |      <p><span>desc</span></p>
          |      <p><strong>Druh podle zpracování:</strong> Zelený čaj</p>
          |    </div>
          |  </div>
          |</body></html>""".stripMargin
      parseMeeteaTea(html, sampleUrl).flip.map { err =>
        assertTrue(err.message.contains("itemprop=name"))
      }
    },
    test("fails with ParseError when tea type label is unknown") {
      val html =
        """
          |<html><body>
          |  <div class="p-detail">
          |    <meta itemprop="name" content="T" />
          |    <div class="p-short-description">
          |      <p><span>N</span></p>
          |      <p><span>desc</span></p>
          |      <p><strong>Druh podle zpracování:</strong> Mystery</p>
          |    </div>
          |  </div>
          |</body></html>
          |""".stripMargin
      parseMeeteaTea(html, sampleUrl).flip.map { err =>
        assertTrue(err.message.contains("Mystery"))
      }
    }
  )
