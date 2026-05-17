import zio.*
import zio.http.URL
import zio.test.*

object MeileafParserSpec extends ZIOSpecDefault:

  private val sampleUrl: URL =
    URL.decode("https://meileaf.com/tea/tea-jtic/").toOption.get

  private val sampleHtml =
    """
      |<html>
      |  <body>
      |    <ol itemscope itemtype="https://schema.org/BreadcrumbList">
      |      <li itemprop="itemListElement" itemscope itemtype="https://schema.org/ListItem">
      |        <a itemprop="item" href="/teas"><span itemprop="name">Tea &amp; Tisanes</span></a>
      |        <meta itemprop="position" content="1" />
      |      </li>
      |      <li itemprop="itemListElement" itemscope itemtype="https://schema.org/ListItem">
      |        <a itemprop="item" href="/teas/white/"><span itemprop="name">White Tea</span></a>
      |        <meta itemprop="position" content="2" />
      |      </li>
      |      <li itemprop="itemListElement" itemscope itemtype="https://schema.org/ListItem">
      |        <a itemprop="item" href="/p/tea-jtic/"><span itemprop="name">Jade Star 9</span></a>
      |        <meta itemprop="position" content="3" />
      |      </li>
      |    </ol>
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
      parseMeileafTea(sampleHtml, sampleUrl).map { info =>
        assertTrue(
          info == TeaInfo(
            title = "Jade Star 9",
            name = "2008 Bai Mu Dan and Shou Mei",
            description = "N/A",
            types = Set(TeaType.WhiteTea),
            vendor = Vendor.MeiLeaf,
            url = "https://meileaf.com/tea/tea-jtic/",
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
    test("leaves missing details as None") {
      val html =
        """<html><body>
          |  <ol itemscope itemtype="https://schema.org/BreadcrumbList">
          |    <li><span itemprop="name">Tea &amp; Tisanes</span></li>
          |    <li><span itemprop="name">White Tea</span></li>
          |    <li><span itemprop="name">T</span></li>
          |  </ol>
          |  <h1 class="product-info__title">T</h1>
          |  <h2 class="product-info__subtitle">N</h2>
          |  <dl class="product-detail"></dl>
          |</body></html>""".stripMargin
      parseMeileafTea(html, sampleUrl).map { info =>
        assertTrue(
          info.title == "T",
          info.name == "N",
          info.types == Set(TeaType.WhiteTea),
          info.season.isEmpty,
          info.cultivar.isEmpty,
          info.origin.isEmpty,
          info.elevation.isEmpty
        )
      }
    },
    test("fails with ParseError when title is missing") {
      val html =
        """<html><body>
          |  <ol itemscope itemtype="https://schema.org/BreadcrumbList">
          |    <li><span itemprop="name">Tea &amp; Tisanes</span></li>
          |    <li><span itemprop="name">White Tea</span></li>
          |    <li><span itemprop="name">X</span></li>
          |  </ol>
          |  <h2 class="product-info__subtitle">x</h2>
          |</body></html>""".stripMargin
      parseMeileafTea(html, sampleUrl).flip.map { err =>
        assertTrue(err.getMessage.contains("product-info__title"))
      }
    },
    test("fails with ParseError when tea type breadcrumb is unknown") {
      val html =
        """<html><body>
          |  <ol itemscope itemtype="https://schema.org/BreadcrumbList">
          |    <li><span itemprop="name">Tea &amp; Tisanes</span></li>
          |    <li><span itemprop="name">Mystery Tea</span></li>
          |    <li><span itemprop="name">X</span></li>
          |  </ol>
          |  <h1 class="product-info__title">T</h1>
          |  <h2 class="product-info__subtitle">N</h2>
          |  <dl class="product-detail"></dl>
          |</body></html>""".stripMargin
      parseMeileafTea(html, sampleUrl).flip.map { err =>
        assertTrue(err.getMessage.contains("Mystery Tea"))
      }
    }
  )
