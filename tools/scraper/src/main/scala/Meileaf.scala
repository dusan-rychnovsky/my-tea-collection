import org.jsoup.Jsoup
import scala.jdk.CollectionConverters.*
import zio.*
import zio.http.*

final class ParseError(message: String) extends RuntimeException(message)

def parseMeileafTea(html: String): IO[ParseError, TeaInfo] =
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

      TeaInfo(
        title = text("h1.product-info__title"),
        name = text("h2.product-info__subtitle"),
        season = details.get("Season"),
        cultivar = details.get("Cultivar"),
        origin = details.get("Origin"),
        elevation = details.get("Elevation")
      )
    }
    .refineOrDie { case e: ParseError => e }

val meileafVendor: Vendor = Vendor(
  name = "meileaf",
  host = "meileaf.com",
  scrape = url => fetch(url.encode).flatMap(parseMeileafTea)
)
