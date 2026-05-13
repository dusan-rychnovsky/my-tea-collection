case class TeaInfo(
  title: String,
  name: String,
  season: String,
  cultivar: String,
  origin: String,
  elevation: String
)

def renderTeaInfo(info: TeaInfo): String =
  s"""title: ${info.title}
     |name: ${info.name}
     |season: ${info.season}
     |cultivar: ${info.cultivar}
     |origin: ${info.origin}
     |elevation: ${info.elevation}""".stripMargin
