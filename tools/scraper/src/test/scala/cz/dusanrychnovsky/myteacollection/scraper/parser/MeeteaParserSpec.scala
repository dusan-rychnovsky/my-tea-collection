package cz.dusanrychnovsky.myteacollection.scraper.parser

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
            title = "Heritage Green 2026",
            name = "Móc Câu Thái Nguyên 2026",
            description =
              "Robustní, ale elegantní zelený čaj s příjemně hořko-sladkou chutí a vůní připomínající trávu, hrášek, Pak Choi a kukuřici, s velmi dlouhou a lehce slanou dochutí.",
            types = Set(TeaType.GreenTea),
            vendor = Vendor.Meetea,
            url = "https://store.meetea.cz/zeleny-caj/heritage-green-2026/",
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
    },
    test("ignores unknown <strong> labels (besides Druh podle zpracování)") {
      parseMeeteaTea(sampleHtml, sampleUrl).map { info =>
        assertTrue(
          info.season.contains("Březen 2026"),
          !info.season.exists(_.contains("Zelený"))
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
