package cz.dusanrychnovsky.myteacollection.scraper.parser

import cz.dusanrychnovsky.myteacollection.scraper.domain.*

def parseLocation(raw: String): Option[Location] =
  Location.fromList(raw.split(",").iterator.map(_.trim).toList.reverse)
