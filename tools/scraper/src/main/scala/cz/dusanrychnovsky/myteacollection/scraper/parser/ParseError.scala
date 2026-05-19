package cz.dusanrychnovsky.myteacollection.scraper.parser

import cz.dusanrychnovsky.myteacollection.scraper.ScraperError

final case class ParseError(message: String) extends ScraperError
