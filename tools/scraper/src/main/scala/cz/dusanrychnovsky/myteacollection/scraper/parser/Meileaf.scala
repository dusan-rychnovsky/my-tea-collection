package cz.dusanrychnovsky.myteacollection.scraper.parser

import cz.dusanrychnovsky.myteacollection.scraper.domain.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.jdk.CollectionConverters.*
import zio.*
import zio.http.*

private val elevationDigits = """\d+""".r

private def parseElevation(raw: String): Option[Elevation] =
  elevationDigits.findFirstIn(raw).flatMap(_.toIntOption).map(Elevation(_))

private def parseDetails(doc: Document): Map[String, String] =
  doc.select("dl.product-detail dd").asScala.flatMap { dd =>
    for
      nameEl  <- Option(dd.selectFirst("meta[itemprop=name]"))
      valueEl <- Option(dd.selectFirst("span[itemprop=value]"))
    yield nameEl.attr("content") -> valueEl.text.trim
  }.toMap

private def parseBreadcrumbs(doc: Document): List[String] =
  doc
    .select("ol[itemtype$=BreadcrumbList] li span[itemprop=name]")
    .asScala
    .map(_.text.trim)
    .toList

def parseMeileafTea(html: String, url: URL): IO[ParseError, TeaInfo] =
  for
    doc   <- ZIO.attempt(Jsoup.parse(html)).orDie
    title <- parseElementText(doc, "h1.product-info__title")
    name  <- parseElementText(doc, "h2.product-info__subtitle")
    details         = parseDetails(doc)
    breadcrumbNames = parseBreadcrumbs(doc)
    teaTypeName <- ZIO
      .fromOption(breadcrumbNames.lift(1))
      .orElseFail(ParseError("missing tea type in breadcrumbs"))
    teaType <- resolveTeaType(teaTypeName)
  yield TeaInfo(
    title = Title(title),
    name = Name(name),
    description = Description("N/A"),
    types = Set(teaType),
    vendor = Vendor.MeiLeaf,
    url = url,
    season = details.get("Season").flatMap(parseSeason),
    cultivar = details.get("Cultivar"),
    origin = details.get("Origin").flatMap(parseLocation),
    elevation = details.get("Elevation").flatMap(parseElevation),
    price = "N/A",
    brewingInstructions = "N/A",
    inStock = true
  )
