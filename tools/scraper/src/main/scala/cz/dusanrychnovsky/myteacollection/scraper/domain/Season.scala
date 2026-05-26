package cz.dusanrychnovsky.myteacollection.scraper.domain

enum Season:
  case Year(year: Int)
  case SeasonOfYear(name: SeasonName, year: Int)
  case MonthOfYear(month: Month, year: Int)
