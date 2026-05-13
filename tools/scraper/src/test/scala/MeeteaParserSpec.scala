import zio.*
import zio.test.*

object MeeteaParserSpec extends ZIOSpecDefault:

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
      |          <p><span>Robustní zelený čaj...</span></p>
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
      parseMeeteaTea(sampleHtml).map { info =>
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
    },
    test("ignores unknown <strong> labels (e.g. Druh podle zpracování)") {
      parseMeeteaTea(sampleHtml).map { info =>
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
          |    <div class="p-short-description"><p><span>x</span></p></div>
          |  </div>
          |</body></html>""".stripMargin
      parseMeeteaTea(html).flip.map { err =>
        assertTrue(err.getMessage.contains("itemprop=name"))
      }
    }
  )
