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
    test("returns None for unknown labels") {
      assertTrue(lookupTeaType("Mystery Tea").isEmpty)
    }
  )
