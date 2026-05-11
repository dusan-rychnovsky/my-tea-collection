import zio.*

object Main extends ZIOAppDefault:
  val program: ZIO[Any, java.io.IOException, Unit] =
    Console.printLine("Hello world!")

  def run = program
