package cz.dusanrychnovsky.myteacollection.scraper.parser

import cz.dusanrychnovsky.myteacollection.scraper.domain.*

import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import scala.jdk.CollectionConverters.*
import zio.*
import zio.http.*

private def cleanText(s: String): String =
  s.replace(' ', ' ').trim

def parseMeeteaTea(html: String, url: URL): IO[ParseError, TeaInfo] =
  for
    doc <- ZIO.attempt(Jsoup.parse(html)).orDie
    title <- ZIO
      .fromOption(
        Option(doc.selectFirst("div.p-detail meta[itemprop=name]"))
          .map(_.attr("content").trim)
          .filter(_.nonEmpty)
      )
      .orElseFail(ParseError("missing meta[itemprop=name] in .p-detail"))
    name <- ZIO
      .fromOption(
        Option(doc.selectFirst("div.p-short-description p:first-of-type span"))
          .map(el => cleanText(el.text))
          .filter(_.nonEmpty)
      )
      .orElseFail(ParseError("missing name span in .p-short-description"))
    description <- ZIO
      .fromOption(
        Option(doc.selectFirst("div.p-short-description p:nth-of-type(2) span"))
          .map(el => cleanText(el.text))
          .filter(_.nonEmpty)
      )
      .orElseFail(ParseError("missing description span in .p-short-description"))
    labels = doc.select("div.p-short-description strong").asScala.flatMap { strong =>
      val label = strong.text.stripSuffix(":").trim
      Option(strong.nextSibling).collect {
        case t: TextNode if cleanText(t.text).nonEmpty =>
          label -> cleanText(t.text)
      }
    }.toMap
    teaTypeName <- ZIO
      .fromOption(labels.get("Druh podle zpracování"))
      .orElseFail(ParseError("missing tea type label (Druh podle zpracování)"))
    teaType <- ZIO
      .fromOption(lookupTeaType(teaTypeName))
      .orElseFail(ParseError(s"unknown tea type: $teaTypeName"))
  yield TeaInfo(
    title = title,
    name = name,
    description = description,
    types = Set(teaType),
    vendor = Vendor.Meetea,
    url = url.encode,
    origin = labels.get("Původ"),
    cultivar = labels.get("Odrůda"),
    season = labels.get("Sklizeň"),
    elevation = None,
    price = "N/A",
    brewingInstructions = "N/A",
    inStock = true
  )
