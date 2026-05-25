package cz.dusanrychnovsky.myteacollection.scraper.domain

opaque type Description = String

object Description:
  def apply(value: String): Description        = value
  extension (d: Description) def value: String = d
