package cz.dusanrychnovsky.myteacollection.scraper.domain

opaque type Title = String

object Title:
  def apply(value: String): Title        = value
  extension (t: Title) def value: String = t
