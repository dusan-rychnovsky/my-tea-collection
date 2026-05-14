import org.jsoup.Jsoup
import scala.jdk.CollectionConverters.*
import zio.*
import zio.http.*

final class ParseError(message: String) extends RuntimeException(message)

def parseMeileafTea(html: String, url: URL): IO[ParseError, TeaInfo] =
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

      val breadcrumbNames: List[String] =
        doc
          .select("ol[itemtype$=BreadcrumbList] li span[itemprop=name]")
          .asScala
          .map(_.text.trim)
          .toList

      val teaTypeName = breadcrumbNames
        .lift(1)
        .getOrElse(throw ParseError("missing tea type in breadcrumbs"))
      val teaType = TeaType.values
        .find(_.displayName == teaTypeName)
        .getOrElse(throw ParseError(s"unknown tea type: $teaTypeName"))

      TeaInfo(
        title = text("h1.product-info__title"),
        name = text("h2.product-info__subtitle"),
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
    }
    .refineOrDie { case e: ParseError => e }
