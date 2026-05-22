package cz.dusanrychnovsky.myteacollection.scraper.parser

import cz.dusanrychnovsky.myteacollection.scraper.ScraperError

import org.jsoup.nodes.{Document, Element}
import zio.*

final case class ParseError(message: String) extends ScraperError

private[parser] def cleanText(s: String): String =
  s.replace(' ', ' ').trim

def parseElementText(doc: Document, selector: String): IO[ParseError, String] =
  parseElement(doc, selector, el => Option(el.text))

def parseAttributeText(doc: Document, selector: String, attribute: String): IO[ParseError, String] =
  parseElement(doc, selector, el => Option(el.attr(attribute)))

def parseElement(
  doc: Document,
  selector: String,
  extract: Element => Option[String]
): IO[ParseError, String] =
  ZIO
    .fromOption(
      Option(doc.selectFirst(selector))
        .flatMap(extract)
        .map(cleanText)
        .filter(_.nonEmpty)
    )
    .orElseFail(ParseError(s"missing or empty extract: [$extract] for element: [$selector]"))
