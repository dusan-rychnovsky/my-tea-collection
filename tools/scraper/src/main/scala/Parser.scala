import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import scala.jdk.CollectionConverters.*
import zio.*

final class ParseError(message: String) extends RuntimeException(message)

def parseTea(html: String): IO[ParseError, TeaInfo] =
  ZIO
    .attempt {
      val doc = Jsoup.parse(html)

      def text(selector: String): String =
        Option(doc.selectFirst(selector))
          .map(_.text.trim)
          .getOrElse(throw ParseError(s"missing element: $selector"))

      val details: Map[String, String] =
        doc.select("dl.product-detail dd").asScala.flatMap { dd =>
          for
            nameEl  <- Option(dd.selectFirst("meta[itemprop=name]"))
            valueEl <- Option(dd.selectFirst("span[itemprop=value]"))
          yield nameEl.attr("content") -> valueEl.text.trim
        }.toMap

      def detail(name: String): String =
        details.getOrElse(name, throw ParseError(s"missing detail: $name"))

      TeaInfo(
        title    = text("h1.product-info__title"),
        name     = text("h2.product-info__subtitle"),
        season   = detail("Season"),
        cultivar = detail("Cultivar"),
        origin   = detail("Origin"),
        elevation = detail("Elevation")
      )
    }
    .refineOrDie { case e: ParseError => e }
