import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import scala.jdk.CollectionConverters.*
import zio.*
import zio.http.*

private val meeteaTeaTypes: Map[String, TeaType] = Map(
  "Zelený čaj" -> TeaType.Green
)

private def cleanText(s: String): String =
  s.replace(' ', ' ').trim

def parseMeeteaTea(html: String, url: URL): IO[ParseError, TeaInfo] =
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

      def description: String =
        Option(doc.selectFirst("div.p-short-description p:nth-of-type(2) span"))
          .map(el => cleanText(el.text))
          .filter(_.nonEmpty)
          .getOrElse(throw ParseError("missing description span in .p-short-description"))

      val labels: Map[String, String] =
        doc.select("div.p-short-description strong").asScala.flatMap { strong =>
          val label = strong.text.stripSuffix(":").trim
          Option(strong.nextSibling).collect {
            case t: TextNode if cleanText(t.text).nonEmpty =>
              label -> cleanText(t.text)
          }
        }.toMap

      val teaTypeName = labels.getOrElse(
        "Druh podle zpracování",
        throw ParseError("missing tea type label (Druh podle zpracování)")
      )
      val teaType = meeteaTeaTypes
        .getOrElse(teaTypeName, throw ParseError(s"unknown tea type: $teaTypeName"))

      TeaInfo(
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
    }
    .refineOrDie { case e: ParseError => e }
