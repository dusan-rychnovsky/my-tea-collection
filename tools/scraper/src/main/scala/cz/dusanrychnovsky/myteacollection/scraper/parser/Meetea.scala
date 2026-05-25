package cz.dusanrychnovsky.myteacollection.scraper.parser

import cz.dusanrychnovsky.myteacollection.scraper.domain.*
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, TextNode}

import scala.jdk.CollectionConverters.*
import zio.*
import zio.http.*

private def parseLabels(doc: Document): Map[String, String] =
  doc.select("div.p-short-description strong").asScala.flatMap { strong =>
    val label = strong.text.stripSuffix(":").trim
    Option(strong.nextSibling).collect {
      case t: TextNode if cleanText(t.text).nonEmpty =>
        label -> cleanText(t.text)
    }
  }.toMap

private def getLabel(name: String, labels: Map[String, String]): IO[ParseError, String] =
  ZIO
    .fromOption(labels.get(name))
    .orElseFail(ParseError(s"missing label: $name"))

private def getTeaType(labels: Map[String, String]): IO[ParseError, TeaType] =
  for
    teaTypeVal <- getLabel("Druh podle zpracování", labels)
    teaType    <- resolveTeaType(teaTypeVal)
  yield teaType

def parseMeeteaTea(html: String, url: URL): IO[ParseError, TeaInfo] =
  for
    doc         <- ZIO.attempt(Jsoup.parse(html)).orDie
    title       <- parseAttributeText(doc, "div.p-detail meta[itemprop=name]", "content")
    name        <- parseElementText(doc, "div.p-short-description p:first-of-type span")
    description <- parseElementText(doc, "div.p-short-description p:nth-of-type(2) span")
    labels = parseLabels(doc)
    teaType <- getTeaType(labels)
  yield TeaInfo(
    title = Title(title),
    name = Name(name),
    description = Description(description),
    types = Set(teaType),
    vendor = Vendor.Meetea,
    url = url,
    origin = labels.get("Původ"),
    cultivar = labels.get("Odrůda"),
    season = labels.get("Sklizeň"),
    elevation = None,
    price = "N/A",
    brewingInstructions = "N/A",
    inStock = true
  )
