import zio.*
import zio.test.*

object MeileafParserSpec extends ZIOSpecDefault:

  private val sampleHtml =
    """
      |<html>
      |  <body>
      |    <h1 class="product-info__title" itemprop="name">Jade Star 9</h1>
      |    <h2 class="product-info__subtitle" itemprop="alternateName">
      |      2008 Bai Mu Dan and Shou Mei
      |    </h2>
      |    <dl class="product-detail">
      |      <dt class="product-detail__title">SEASON</dt>
      |      <dd itemprop="additionalProperty" itemscope itemtype="https://schema.org/PropertyValue"
      |          class="product-detail__info">
      |        <meta itemprop="name" content="Season">
      |        <span itemprop="value" class="squiggle">Spring 2008</span>
      |      </dd>
      |      <dt class="product-detail__title">CULTIVAR</dt>
      |      <dd itemprop="additionalProperty" itemscope itemtype="https://schema.org/PropertyValue"
      |          class="product-detail__info">
      |        <meta itemprop="name" content="Cultivar">
      |        <span itemprop="value" class="squiggle">Da Bai</span>
      |      </dd>
      |      <dt class="product-detail__title">ORIGIN</dt>
      |      <dd itemprop="additionalProperty" itemscope itemtype="https://schema.org/PropertyValue"
      |          class="product-detail__info">
      |        <meta itemprop="name" content="Origin">
      |        <span itemprop="value" class="squiggle">Fuding, Fujian, China</span>
      |      </dd>
      |      <dt class="product-detail__title">ELEVATION</dt>
      |      <dd itemprop="additionalProperty" itemscope itemtype="https://schema.org/PropertyValue"
      |          class="product-detail__info">
      |        <meta itemprop="name" content="Elevation">
      |        <span itemprop="value" class="squiggle">900m approx</span>
      |      </dd>
      |    </dl>
      |  </body>
      |</html>
      |""".stripMargin

  def spec = suite("Meileaf parser")(
    test("extracts tea info from meileaf-shaped HTML") {
      parseMeileafTea(sampleHtml).map { info =>
        assertTrue(
          info == TeaInfo(
            title = "Jade Star 9",
            name = "2008 Bai Mu Dan and Shou Mei",
            season = Some("Spring 2008"),
            cultivar = Some("Da Bai"),
            origin = Some("Fuding, Fujian, China"),
            elevation = Some("900m approx")
          )
        )
      }
    },
    test("leaves missing details as None") {
      val html =
        """<html><body>
          |  <h1 class="product-info__title">T</h1>
          |  <h2 class="product-info__subtitle">N</h2>
          |  <dl class="product-detail"></dl>
          |</body></html>""".stripMargin
      parseMeileafTea(html).map { info =>
        assertTrue(
          info.title == "T",
          info.name == "N",
          info.season.isEmpty,
          info.cultivar.isEmpty,
          info.origin.isEmpty,
          info.elevation.isEmpty
        )
      }
    },
    test("fails with ParseError when title is missing") {
      val html = "<html><body><h2 class=\"product-info__subtitle\">x</h2></body></html>"
      parseMeileafTea(html).flip.map { err =>
        assertTrue(err.getMessage.contains("product-info__title"))
      }
    }
  )
