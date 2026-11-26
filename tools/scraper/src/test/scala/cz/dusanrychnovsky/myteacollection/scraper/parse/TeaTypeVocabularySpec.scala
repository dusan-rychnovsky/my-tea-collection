package cz.dusanrychnovsky.myteacollection.scraper.parse

import cz.dusanrychnovsky.myteacollection.scraper.domain.*

import zio.test.*

object TeaTypeVocabularySpec extends ZIOSpecDefault:

  def spec = suite("teaTypeVocabulary")(
    test("looks up an English label") {
      assertTrue(lookupTeaType("Green Tea").contains(TeaType.GreenTea))
    },
    test("looks up a Czech label") {
      assertTrue(lookupTeaType("Zelený čaj").contains(TeaType.GreenTea))
    },
    test("is case insensitive") {
      assertTrue(
        lookupTeaType("green tea").contains(TeaType.GreenTea),
        lookupTeaType("GREEN TEA").contains(TeaType.GreenTea),
        lookupTeaType("ZELENÝ ČAJ").contains(TeaType.GreenTea)
      )
    },
    test("matches whole names inside contextual labels") {
      assertTrue(
        lookupTeaType("Tmavý čaj (Shu Puer)").contains(TeaType.ShuPuerh),
        lookupTeaType("Tmavý čaj (SHU PUERH)").contains(TeaType.ShuPuerh),
        lookupTeaType("Premium [WHITE TEA] selection").contains(TeaType.WhiteTea),
        lookupTeaType("Čaj: ZELENÝ ČAJ (jarní)").contains(TeaType.GreenTea)
      )
    },
    test("accepts repeated names and aliases for the same type") {
      assertTrue(
        lookupTeaType("Green Tea / zelený čaj / GREEN TEA").contains(TeaType.GreenTea),
        lookupTeaType("Shu Puer / Shu Puerh").contains(TeaType.ShuPuerh)
      )
    },
    test("rejects labels naming distinct tea types") {
      for
        unrelated      <- resolveTeaType("Green Tea / Oolong").flip
        parentAndChild <- resolveTeaType("Dark Tea (Shu Puerh)").flip
      yield assertTrue(
        unrelated == ParseError("ambiguous tea type: Green Tea / Oolong"),
        parentAndChild == ParseError("ambiguous tea type: Dark Tea (Shu Puerh)")
      )
    },
    test("does not match inside larger words, including Unicode words") {
      for
        flavoured <- resolveTeaType("Flavoured tea").flip
        prefixed  <- resolveTeaType("čajoolong").flip
        suffixed  <- resolveTeaType("oolongčaj").flip
      yield assertTrue(
        flavoured == ParseError("unknown tea type: Flavoured tea"),
        prefixed == ParseError("unknown tea type: čajoolong"),
        suffixed == ParseError("unknown tea type: oolongčaj")
      )
    },
    test("returns ParseError for unknown labels") {
      resolveTeaType("Mystery Tea").flip.map { err =>
        assertTrue(err == ParseError("unknown tea type: Mystery Tea"))
      }
    }
  )
