import java.util.Locale

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

def lookupTeaType(label: String): Option[TeaType] =
  teaTypeVocabulary.get(label.toLowerCase(Locale.ROOT))
