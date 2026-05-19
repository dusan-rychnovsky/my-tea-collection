package cz.dusanrychnovsky.myteacollection.scraper.parser

import cz.dusanrychnovsky.myteacollection.scraper.domain.*

import org.jsoup.Jsoup
import scala.jdk.CollectionConverters.*
import zio.*
import zio.http.*

def parseMeileafTea(html: String, url: URL): IO[ParseError, TeaInfo] =
  for
    doc <- ZIO.attempt(Jsoup.parse(html)).orDie
    title <- ZIO
      .fromOption(Option(doc.selectFirst("h1.product-info__title")).map(_.text.trim))
      .orElseFail(ParseError("missing element: h1.product-info__title"))
    name <- ZIO
      .fromOption(Option(doc.selectFirst("h2.product-info__subtitle")).map(_.text.trim))
      .orElseFail(ParseError("missing element: h2.product-info__subtitle"))
    details = doc.select("dl.product-detail dd").asScala.flatMap { dd =>
      for
        nameEl  <- Option(dd.selectFirst("meta[itemprop=name]"))
        valueEl <- Option(dd.selectFirst("span[itemprop=value]"))
      yield nameEl.attr("content") -> valueEl.text.trim
    }.toMap
    breadcrumbNames = doc
      .select("ol[itemtype$=BreadcrumbList] li span[itemprop=name]")
      .asScala
      .map(_.text.trim)
      .toList
    teaTypeName <- ZIO
      .fromOption(breadcrumbNames.lift(1))
      .orElseFail(ParseError("missing tea type in breadcrumbs"))
    teaType <- ZIO
      .fromOption(lookupTeaType(teaTypeName))
      .orElseFail(ParseError(s"unknown tea type: $teaTypeName"))
  yield TeaInfo(
    title = title,
    name = name,
    description = "N/A",
    types = Set(teaType),
    vendor = Vendor.MeiLeaf,
    url = url.encode,
    season = details.get("Season"),
    cultivar = details.get("Cultivar"),
    origin = details.get("Origin"),
    elevation = details.get("Elevation"),
    price = "N/A",
    brewingInstructions = "N/A",
    inStock = true
  )
