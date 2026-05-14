import zio.test.*

object TeaInfoSpec extends ZIOSpecDefault:

  private val sampleInfo = TeaInfo(
    title = "Jade Star 9",
    name = "2008 Bai Mu Dan and Shou Mei",
    description = "N/A",
    types = Set(TeaType.White),
    vendor = Vendor.MeiLeaf,
    url = "https://example.com/x",
    season = Some("Spring 2008"),
    cultivar = Some("Da Bai"),
    origin = Some("Fuding, Fujian, China"),
    elevation = Some("900m approx"),
    price = "N/A",
    brewingInstructions = "N/A",
    inStock = true
  )

  def spec = suite("renderTeaInfo")(
    test("renders all fields in declared order") {
      val expected =
        """title: Jade Star 9
          |name: 2008 Bai Mu Dan and Shou Mei
          |description: N/A
          |types: White Tea
          |vendor: Mei Leaf
          |url: https://example.com/x
          |origin: Fuding, Fujian, China
          |cultivar: Da Bai
          |season: Spring 2008
          |elevation: 900m approx
          |price: N/A
          |brewingInstructions: N/A
          |inStock: true""".stripMargin
      assertTrue(renderTeaInfo(sampleInfo) == expected)
    },
    test("omits missing optional fields") {
      val rendered = renderTeaInfo(sampleInfo.copy(elevation = None))
      assertTrue(
        rendered.contains("origin: Fuding, Fujian, China"),
        !rendered.contains("elevation")
      )
    },
    test("renders multiple types comma-separated, sorted") {
      val rendered = renderTeaInfo(sampleInfo.copy(types = Set(TeaType.Green, TeaType.White)))
      assertTrue(rendered.contains("types: Green Tea, White Tea"))
    }
  )
