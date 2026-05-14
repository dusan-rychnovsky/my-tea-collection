case class TeaInfo(
  title: String,
  name: String,
  description: String,
  types: Set[TeaType],
  vendor: Vendor,
  url: String,
  origin: Option[String] = None,
  cultivar: Option[String] = None,
  season: Option[String] = None,
  elevation: Option[String] = None,
  price: String,
  brewingInstructions: String,
  inStock: Boolean
)

def renderTeaInfo(info: TeaInfo): String =
  val typesValue =
    if info.types.isEmpty then None
    else Some(info.types.toList.map(_.displayName).sorted.mkString(", "))
  val lines =
    List(
      "title"               -> Some(info.title),
      "name"                -> Some(info.name),
      "description"         -> Some(info.description),
      "types"               -> typesValue,
      "vendor"              -> Some(info.vendor.displayName),
      "url"                 -> Some(info.url),
      "origin"              -> info.origin,
      "cultivar"            -> info.cultivar,
      "season"              -> info.season,
      "elevation"           -> info.elevation,
      "price"               -> Some(info.price),
      "brewingInstructions" -> Some(info.brewingInstructions),
      "inStock"             -> Some(info.inStock.toString)
    ).collect { case (label, Some(value)) => s"$label: $value" }
  lines.mkString("\n")
