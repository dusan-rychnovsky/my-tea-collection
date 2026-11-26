package cz.dusanrychnovsky.myteacollection.scraper.render

import cz.dusanrychnovsky.myteacollection.scraper.domain.*

import zio.http.URL
import zio.json.*
import zio.json.ast.Json
import zio.test.*

object TeaInfoSpec extends ZIOSpecDefault:

  private val sampleUrl: URL =
    URL.decode("https://example.com/x").toOption.get

  private val sampleInfo = TeaInfo(
    title = Title("Jade Star 9"),
    name = Name("2008 Bai Mu Dan and Shou Mei"),
    description = Description("N/A"),
    types = Set(TeaType.WhiteTea),
    vendor = Vendor.MeiLeaf,
    url = sampleUrl,
    season = Some(Season.SeasonOfYear(SeasonName.Spring, 2008)),
    cultivar = Some("Da Bai"),
    origin = Some(Location("China", "Fujian", "Fuding")),
    elevation = Some(Elevation(900)),
    price = "N/A",
    brewingInstructions = "N/A",
    inStock = true
  )

  def spec = suite("renderTeaInfo")(
    suite("text")(
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
          |elevation: 900m
          |price: N/A
          |brewingInstructions: N/A
          |inStock: true""".stripMargin
        assertTrue(renderTeaInfo(sampleInfo, OutputFormat.Text) == expected)
      },
      test("omits missing optional fields") {
        val rendered = renderTeaInfo(
          sampleInfo.copy(origin = None, cultivar = None, season = None, elevation = None),
          OutputFormat.Text
        )
        assertTrue(
          !rendered.contains("origin"),
          !rendered.contains("cultivar"),
          !rendered.contains("season"),
          !rendered.contains("elevation")
        )
      },
      test("renders multiple types comma-separated, sorted") {
        val rendered = renderTeaInfo(
          sampleInfo.copy(types = Set(TeaType.GreenTea, TeaType.WhiteTea)),
          OutputFormat.Text
        )
        assertTrue(rendered.contains("types: Green Tea, White Tea"))
      },
      test("preserves omission of empty tea types") {
        assertTrue(
          !renderTeaInfo(sampleInfo.copy(types = Set.empty), OutputFormat.Text).contains("types:")
        )
      }
    ),
    suite("json")(
      test("renders the tea info.json shape with strings, sorted arrays, and a boolean") {
        val expected =
          """{
            |  "title": "Jade Star 9",
            |  "name": "2008 Bai Mu Dan and Shou Mei",
            |  "description": "N/A",
            |  "types": ["Green Tea", "White Tea"],
            |  "vendor": "Mei Leaf",
            |  "url": "https://example.com/x",
            |  "origin": "Fuding, Fujian, China",
            |  "cultivar": "Da Bai",
            |  "season": "Spring 2008",
            |  "elevation": "900m",
            |  "price": "N/A",
            |  "brewingInstructions": "N/A",
            |  "inStock": true,
            |  "tags": []
            |}""".stripMargin
        val decoded = renderTeaInfo(
          sampleInfo.copy(types = Set(TeaType.WhiteTea, TeaType.GreenTea)),
          OutputFormat.Json
        ).fromJson[Json]
        assertTrue(decoded.isRight, decoded == expected.fromJson[Json])
      },
      test("retains missing metadata as N/A strings and empty types as an array") {
        val decoded = renderTeaInfo(
          sampleInfo.copy(
            origin = None,
            cultivar = None,
            season = None,
            elevation = None,
            types = Set.empty
          ),
          OutputFormat.Json
        ).fromJson[Map[String, Json]]
        assertTrue(
          decoded.exists(fields =>
            List("origin", "cultivar", "season", "elevation").forall(key =>
              fields.get(key).contains(Json.Str("N/A"))
            ) && fields.get("types").contains(Json.Arr())
          )
        )
      },
      test("escapes special characters without losing Unicode or changing field types") {
        val special = "T\u00f3genky\u00f3 \"quoted\" \\ leaves\n\t\r\b\f\u0001"
        val decoded = renderTeaInfo(
          sampleInfo.copy(
            title = Title(special),
            name = Name(""),
            description = Description(special),
            cultivar = Some(special),
            brewingInstructions = special,
            price = "9.2",
            inStock = false
          ),
          OutputFormat.Json
        ).fromJson[Map[String, Json]]
        assertTrue(
          decoded.exists(fields =>
            List("title", "description", "cultivar", "brewingInstructions").forall(key =>
              fields.get(key).contains(Json.Str(special))
            ) &&
              fields.get("name").contains(Json.Str("")) &&
              fields.get("price").contains(Json.Str("9.2")) &&
              fields.get("inStock").contains(Json.Bool(false))
          )
        )
      },
      test("uses canonical year and month season strings and vendor names") {
        val year = renderTeaInfo(
          sampleInfo.copy(season = Some(Season.Year(2026)), vendor = Vendor.Meetea),
          OutputFormat.Json
        ).fromJson[Map[String, Json]]
        val month = renderTeaInfo(
          sampleInfo.copy(season = Some(Season.MonthOfYear(Month.March, 2026))),
          OutputFormat.Json
        ).fromJson[Map[String, Json]]
        assertTrue(
          year.exists(fields =>
            fields.get("season").contains(Json.Str("2026")) &&
              fields.get("vendor").contains(Json.Str("Meetea"))
          ),
          month.exists(_.get("season").contains(Json.Str("March 2026")))
        )
      }
    )
  )
