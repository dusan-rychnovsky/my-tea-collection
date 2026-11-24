package cz.dusanrychnovsky.myteacollection.scraper.parser

import cz.dusanrychnovsky.myteacollection.scraper.domain.*

import java.util.Locale
import java.util.regex.Pattern
import zio.*

val teaTypeVocabulary: Map[String, TeaType] = Map(
  "blend"       -> TeaType.Blend,
  "chen pi"     -> TeaType.ChenPi,
  "rose"        -> TeaType.Rose,
  "white tea"   -> TeaType.WhiteTea,
  "bai mu dan"  -> TeaType.BaiMuDan,
  "shou mei"    -> TeaType.ShouMei,
  "yellow tea"  -> TeaType.YellowTea,
  "green tea"   -> TeaType.GreenTea,
  "zelený čaj"  -> TeaType.GreenTea,
  "long jing"   -> TeaType.LongJing,
  "bi luo chun" -> TeaType.BiLuoChun,
  "mao feng"    -> TeaType.MaoFeng,
  "hou kui"     -> TeaType.HouKui,
  "sencha"      -> TeaType.Sencha,
  "genmaicha"   -> TeaType.Genmaicha,
  "oolong"      -> TeaType.Oolong,
  "yan cha"     -> TeaType.YanCha,
  "dan cong"    -> TeaType.DanCong,
  "bao zhong"   -> TeaType.BaoZhong,
  "dong ding"   -> TeaType.DongDing,
  "red tea"     -> TeaType.RedTea,
  "dian hong"   -> TeaType.DianHong,
  "dark tea"    -> TeaType.DarkTea,
  "sheng puerh" -> TeaType.ShengPuerh,
  "shu puer"    -> TeaType.ShuPuerh,
  "shu puerh"   -> TeaType.ShuPuerh,
  "liu bao"     -> TeaType.LiuBao,
  "fu zhuan"    -> TeaType.FuZhuan,
  "liu an"      -> TeaType.LiuAn,
  "tian jian"   -> TeaType.TianJian,
  "huang pian"  -> TeaType.HuangPian,
  "yabao"       -> TeaType.Yabao,
  "darjeeling"  -> TeaType.Darjeeling,
  "purple tea"  -> TeaType.PurpleTea
)

private val teaTypePatterns = teaTypeVocabulary.toList.map { case (name, teaType) =>
  s"(?U)\\b${Pattern.quote(name)}\\b".r -> teaType
}

def lookupTeaType(label: String): Either[ParseError, TeaType] =
  val normalizedLabel = label.toLowerCase(Locale.ROOT)
  val matchingTypes = teaTypePatterns.collect {
    case (pattern, teaType) if pattern.findFirstIn(normalizedLabel).nonEmpty => teaType
  }.toSet
  matchingTypes.toList match
    case Nil            => Left(ParseError(s"unknown tea type: $label"))
    case teaType :: Nil => Right(teaType)
    case _              => Left(ParseError(s"ambiguous tea type: $label"))

def resolveTeaType(label: String): IO[ParseError, TeaType] =
  ZIO.fromEither(lookupTeaType(label))
