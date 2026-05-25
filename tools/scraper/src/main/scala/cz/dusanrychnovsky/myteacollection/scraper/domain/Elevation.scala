package cz.dusanrychnovsky.myteacollection.scraper.domain

opaque type Elevation = Int

object Elevation:
  def apply(value: Int): Elevation        = value
  extension (e: Elevation) def value: Int = e
