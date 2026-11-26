package cz.dusanrychnovsky.myteacollection.scraper.render

import cz.dusanrychnovsky.myteacollection.scraper.domain.*

import zio.json.*
import zio.json.ast.Json

def renderVendor(v: Vendor): String = v match
  case Vendor.MeiLeaf => "Mei Leaf"
  case Vendor.Meetea  => "Meetea"

def renderTeaType(t: TeaType): String = t match
  case TeaType.Blend      => "Blend"
  case TeaType.ChenPi     => "Chen Pi"
  case TeaType.Rose       => "Rose"
  case TeaType.WhiteTea   => "White Tea"
  case TeaType.BaiMuDan   => "Bai Mu Dan"
  case TeaType.ShouMei    => "Shou Mei"
  case TeaType.YellowTea  => "Yellow Tea"
  case TeaType.GreenTea   => "Green Tea"
  case TeaType.LongJing   => "Long Jing"
  case TeaType.BiLuoChun  => "Bi Luo Chun"
  case TeaType.MaoFeng    => "Mao Feng"
  case TeaType.HouKui     => "Hou Kui"
  case TeaType.Sencha     => "Sencha"
  case TeaType.Genmaicha  => "Genmaicha"
  case TeaType.Oolong     => "Oolong"
  case TeaType.YanCha     => "Yan Cha"
  case TeaType.DanCong    => "Dan Cong"
  case TeaType.BaoZhong   => "Bao Zhong"
  case TeaType.DongDing   => "Dong Ding"
  case TeaType.RedTea     => "Red Tea"
  case TeaType.DianHong   => "Dian Hong"
  case TeaType.DarkTea    => "Dark Tea"
  case TeaType.ShengPuerh => "Sheng Puerh"
  case TeaType.ShuPuerh   => "Shu Puerh"
  case TeaType.LiuBao     => "Liu Bao"
  case TeaType.FuZhuan    => "Fu Zhuan"
  case TeaType.LiuAn      => "Liu An"
  case TeaType.TianJian   => "Tian Jian"
  case TeaType.HuangPian  => "Huang Pian"
  case TeaType.Yabao      => "Yabao"
  case TeaType.Darjeeling => "Darjeeling"
  case TeaType.PurpleTea  => "Purple Tea"

def renderSeason(s: Season): String = s match
  case Season.Year(year)               => year.toString
  case Season.SeasonOfYear(name, year) => s"$name $year"
  case Season.MonthOfYear(month, year) => s"$month $year"

private def renderLocation(location: Location): String =
  location.value.reverse.mkString(", ")

private def renderElevation(elevation: Elevation): String =
  s"${elevation.value}m"

def renderTeaInfo(info: TeaInfo, format: OutputFormat): String = format match
  case OutputFormat.Json => renderTeaInfoJson(info)
  case OutputFormat.Text => renderTeaInfoText(info)

private def renderTeaInfoText(info: TeaInfo): String =
  val typesValue =
    if info.types.isEmpty then None
    else Some(info.types.toList.map(renderTeaType).sorted.mkString(", "))
  val lines =
    List(
      "title"               -> Some(info.title.value),
      "name"                -> Some(info.name.value),
      "description"         -> Some(info.description.value),
      "types"               -> typesValue,
      "vendor"              -> Some(renderVendor(info.vendor)),
      "url"                 -> Some(info.url.encode),
      "origin"              -> info.origin.map(renderLocation),
      "cultivar"            -> info.cultivar,
      "season"              -> info.season.map(renderSeason),
      "elevation"           -> info.elevation.map(renderElevation),
      "price"               -> Some(info.price),
      "brewingInstructions" -> Some(info.brewingInstructions),
      "inStock"             -> Some(info.inStock.toString)
    ).collect { case (label, Some(value)) => s"$label: $value" }
  lines.mkString("\n")

private def renderTeaInfoJson(info: TeaInfo): String =
  Json.Obj(
    "title"       -> Json.Str(info.title.value),
    "name"        -> Json.Str(info.name.value),
    "description" -> Json.Str(info.description.value),
    "types"       -> Json.Arr(info.types.toList.map(renderTeaType).sorted.map(Json.Str(_))*),
    "vendor"      -> Json.Str(renderVendor(info.vendor)),
    "url"         -> Json.Str(info.url.encode),
    "origin"      -> Json.Str(info.origin.map(renderLocation).getOrElse("N/A")),
    "cultivar"    -> Json.Str(info.cultivar.getOrElse("N/A")),
    "season"      -> Json.Str(info.season.map(renderSeason).getOrElse("N/A")),
    "elevation"   -> Json.Str(info.elevation.map(renderElevation).getOrElse("N/A")),
    "price"       -> Json.Str(info.price),
    "brewingInstructions" -> Json.Str(info.brewingInstructions),
    "inStock"             -> Json.Bool(info.inStock),
    "tags"                -> Json.Arr()
  ).toJsonPretty
