import zio.*
import zio.http.Client

object Main extends ZIOAppDefault:

  def program[R](
    url: String,
    download: String => ZIO[R, Throwable, String]
  ): ZIO[R, Throwable, Unit] =
    download(url).flatMap(Console.printLine(_))

  def run =
    for
      args <- getArgs
      url  <- ZIO
                .fromOption(args.headOption)
                .orElseFail(IllegalArgumentException("Usage: scraper <url>"))
      _    <- program(url, fetch).provide(Client.default)
    yield ()
