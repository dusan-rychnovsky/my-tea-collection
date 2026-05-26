package cz.dusanrychnovsky.myteacollection.scraper.parser

import cz.dusanrychnovsky.myteacollection.scraper.domain.*

import zio.test.*

object SeasonParsingSpec extends ZIOSpecDefault:

  def spec = suite("parseSeason")(
    test("parses a bare year") {
      assertTrue(parseSeason("2025").contains(Season.Year(2025)))
    },
    test("parses an English season + year") {
      assertTrue(
        parseSeason("Spring 2008")
          .contains(Season.SeasonOfYear(SeasonName.Spring, 2008))
      )
    },
    test("parses all four English seasons") {
      assertTrue(
        parseSeason("spring 2020").contains(Season.SeasonOfYear(SeasonName.Spring, 2020)),
        parseSeason("Summer 2020").contains(Season.SeasonOfYear(SeasonName.Summer, 2020)),
        parseSeason("AUTUMN 2020").contains(Season.SeasonOfYear(SeasonName.Autumn, 2020)),
        parseSeason("Winter 2020").contains(Season.SeasonOfYear(SeasonName.Winter, 2020))
      )
    },
    test("parses an English month + year") {
      assertTrue(
        parseSeason("March 2026")
          .contains(Season.MonthOfYear(Month.March, 2026))
      )
    },
    test("parses a Czech month + year") {
      assertTrue(
        parseSeason("Březen 2026")
          .contains(Season.MonthOfYear(Month.March, 2026))
      )
    },
    test("is case-insensitive on the name token") {
      assertTrue(
        parseSeason("březen 2026").contains(Season.MonthOfYear(Month.March, 2026)),
        parseSeason("BŘEZEN 2026").contains(Season.MonthOfYear(Month.March, 2026))
      )
    },
    test("trims surrounding whitespace") {
      assertTrue(
        parseSeason("  Spring 2008  ")
          .contains(Season.SeasonOfYear(SeasonName.Spring, 2008))
      )
    },
    test("returns None when the name token is unrecognised") {
      assertTrue(parseSeason("Sprong 2008").isEmpty)
    },
    test("returns None when the year token is not numeric") {
      assertTrue(parseSeason("Spring twentyeight").isEmpty)
    },
    test("returns None for empty or multi-token unrecognised inputs") {
      assertTrue(
        parseSeason("").isEmpty,
        parseSeason("   ").isEmpty,
        parseSeason("Spring 2008 extra").isEmpty
      )
    }
  )
