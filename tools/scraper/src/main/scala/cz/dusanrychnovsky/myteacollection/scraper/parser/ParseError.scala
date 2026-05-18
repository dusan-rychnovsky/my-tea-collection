package cz.dusanrychnovsky.myteacollection.scraper.parser

final case class ParseError(message: String) extends RuntimeException(message)
