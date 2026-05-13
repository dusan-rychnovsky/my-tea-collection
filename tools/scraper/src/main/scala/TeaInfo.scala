case class TeaInfo(
  title: String,
  name: String,
  season: Option[String]    = None,
  cultivar: Option[String]  = None,
  origin: Option[String]    = None,
  elevation: Option[String] = None
)

def renderTeaInfo(info: TeaInfo): String =
  val lines =
    List(
      "title" -> Some(info.title),
      "name"  -> Some(info.name),
      "season"    -> info.season,
      "cultivar"  -> info.cultivar,
      "origin"    -> info.origin,
      "elevation" -> info.elevation
    ).collect { case (label, Some(value)) => s"$label: $value" }
  lines.mkString("\n")
