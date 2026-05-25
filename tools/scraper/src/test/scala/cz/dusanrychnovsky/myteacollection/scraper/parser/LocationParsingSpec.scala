package cz.dusanrychnovsky.myteacollection.scraper.parser

import cz.dusanrychnovsky.myteacollection.scraper.domain.*

import zio.test.*

object LocationParsingSpec extends ZIOSpecDefault:

  def spec = suite("parseLocation")(
    test("parses three comma-separated parts, storing them least-specific first") {
      assertTrue(
        parseLocation("Fuding, Fujian, China")
          .contains(Location("China", "Fujian", "Fuding"))
      )
    },
    test("parses a single part as a one-element Location") {
      assertTrue(parseLocation("China").contains(Location("China")))
    },
    test("trims whitespace around parts") {
      assertTrue(
        parseLocation(" Rohini ,  Darjeeling , India ")
          .contains(Location("India", "Darjeeling", "Rohini"))
      )
    },
    test("keeps empty parts so blank vendor fields surface in output") {
      assertTrue(
        parseLocation("Fuding, Fujian, ")
          .contains(Location("", "Fujian", "Fuding"))
      )
    },
    test("returns None when input has no content (just separator commas)") {
      assertTrue(parseLocation(",").isEmpty)
    }
  )
