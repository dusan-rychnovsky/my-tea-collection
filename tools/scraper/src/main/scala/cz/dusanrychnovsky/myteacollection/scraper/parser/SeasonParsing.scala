package cz.dusanrychnovsky.myteacollection.scraper.parser

import cz.dusanrychnovsky.myteacollection.scraper.domain.*

import java.util.Locale

private val monthVocabulary: Map[String, Month] = Map(
  "leden"    -> Month.January,
  "únor"     -> Month.February,
  "březen"   -> Month.March,
  "duben"    -> Month.April,
  "květen"   -> Month.May,
  "červen"   -> Month.June,
  "červenec" -> Month.July,
  "srpen"    -> Month.August,
  "září"     -> Month.September,
  "říjen"    -> Month.October,
  "listopad" -> Month.November,
  "prosinec" -> Month.December
)

private def lookupSeasonName(raw: String): Option[SeasonName] =
  SeasonName.values.find(_.toString.equalsIgnoreCase(raw))

private def lookupMonth(raw: String): Option[Month] =
  Month.values
    .find(_.toString.equalsIgnoreCase(raw))
    .orElse(monthVocabulary.get(raw.toLowerCase(Locale.ROOT)))

def parseSeason(raw: String): Option[Season] =
  raw.trim.split("\\s+").toList match
    case List(yearStr) =>
      yearStr.toIntOption.map(Season.Year(_))
    case List(nameStr, yearStr) =>
      yearStr.toIntOption.flatMap { year =>
        lookupSeasonName(nameStr)
          .map(Season.SeasonOfYear(_, year))
          .orElse(lookupMonth(nameStr).map(Season.MonthOfYear(_, year)))
      }
    case _ => None
