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

def renderTeaType(t: TeaType): String = t match
  case TeaType.Blend       => "Blend"
  case TeaType.ChenPi      => "Chen Pi"
  case TeaType.Rose        => "Rose"
  case TeaType.WhiteTea    => "White Tea"
  case TeaType.BaiMuDan    => "Bai Mu Dan"
  case TeaType.ShouMei     => "Shou Mei"
  case TeaType.YellowTea   => "Yellow Tea"
  case TeaType.GreenTea    => "Green Tea"
  case TeaType.LongJing    => "Long Jing"
  case TeaType.BiLuoChun   => "Bi Luo Chun"
  case TeaType.MaoFeng     => "Mao Feng"
  case TeaType.HouKui      => "Hou Kui"
  case TeaType.Sencha      => "Sencha"
  case TeaType.Genmaicha   => "Genmaicha"
  case TeaType.Oolong      => "Oolong"
  case TeaType.YanCha      => "Yan Cha"
  case TeaType.DanCong     => "Dan Cong"
  case TeaType.BaoZhong    => "Bao Zhong"
  case TeaType.DongDing    => "Dong Ding"
  case TeaType.RedTea      => "Red Tea"
  case TeaType.DianHong    => "Dian Hong"
  case TeaType.DarkTea     => "Dark Tea"
  case TeaType.ShengPuerh  => "Sheng Puerh"
  case TeaType.ShuPuerh    => "Shu Puerh"
  case TeaType.LiuBao      => "Liu Bao"
  case TeaType.FuZhuan     => "Fu Zhuan"
  case TeaType.LiuAn       => "Liu An"
  case TeaType.TianJian    => "Tian Jian"
  case TeaType.HuangPian   => "Huang Pian"
  case TeaType.Yabao       => "Yabao"
  case TeaType.Darjeeling  => "Darjeeling"
  case TeaType.PurpleTea   => "Purple Tea"

def renderTeaInfo(info: TeaInfo): String =
  val typesValue =
    if info.types.isEmpty then None
    else Some(info.types.toList.map(renderTeaType).sorted.mkString(", "))
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
