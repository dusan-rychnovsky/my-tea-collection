import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, TextNode}
import scala.jdk.CollectionConverters.*
import zio.*
import zio.http.*

private val meeteaLabels: Map[String, String] = Map(
  "Původ"   -> "origin",
  "Odrůda"  -> "cultivar",
  "Sklizeň" -> "season"
)

private def cleanText(s: String): String =
  s.replace(' ', ' ').trim

def parseMeeteaTea(html: String): IO[ParseError, TeaInfo] =
  ZIO
    .attempt {
      val doc = Jsoup.parse(html)

      def title: String =
        Option(doc.selectFirst("div.p-detail meta[itemprop=name]"))
          .map(_.attr("content").trim)
          .filter(_.nonEmpty)
          .getOrElse(throw ParseError("missing meta[itemprop=name] in .p-detail"))

      def name: String =
        Option(doc.selectFirst("div.p-short-description p:first-of-type span"))
          .map(el => cleanText(el.text))
          .filter(_.nonEmpty)
          .getOrElse(throw ParseError("missing name span in .p-short-description"))

      val details: Map[String, String] =
        doc.select("div.p-short-description strong").asScala.flatMap { strong =>
          val label = strong.text.stripSuffix(":").trim
          meeteaLabels.get(label).flatMap { field =>
            Option(strong.nextSibling).collect {
              case t: TextNode if cleanText(t.text).nonEmpty =>
                field -> cleanText(t.text)
            }
          }
        }.toMap

      TeaInfo(
        title = title,
        name = name,
        season = details.get("season"),
        cultivar = details.get("cultivar"),
        origin = details.get("origin"),
        elevation = None
      )
    }
    .refineOrDie { case e: ParseError => e }

val meeteaVendor: Vendor = Vendor(
  name = "meetea",
  host = "store.meetea.cz",
  scrape = url => fetch(url.encode).flatMap(parseMeeteaTea)
)
