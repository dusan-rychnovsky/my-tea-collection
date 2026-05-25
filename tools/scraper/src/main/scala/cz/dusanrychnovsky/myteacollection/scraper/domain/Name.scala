package cz.dusanrychnovsky.myteacollection.scraper.domain

opaque type Name = String

object Name:
  def apply(value: String): Name        = value
  extension (n: Name) def value: String = n
