package cz.dusanrychnovsky.myteacollection.scraper.domain

import zio.http.URL

case class TeaInfo(
  title: Title,
  name: Name,
  description: Description,
  types: Set[TeaType],
  vendor: Vendor,
  url: URL,
  origin: Option[Location] = None,
  cultivar: Option[String] = None,
  season: Option[Season] = None,
  elevation: Option[Elevation] = None,
  price: String,
  brewingInstructions: String,
  inStock: Boolean
)
