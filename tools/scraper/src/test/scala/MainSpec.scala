import zio.*
import zio.test.*

object MainSpec extends ZIOSpecDefault:
  def spec = suite("Main")(
    test("prints Hello world!") {
      for
        _      <- Main.program
        output <- TestConsole.output
      yield assertTrue(output == Vector("Hello world!\n"))
    }
  )
