import zio.*
import zio.http.Client

object Main extends ZIOAppDefault:

  def program[R](
    url: String,
    download: String => ZIO[R, Throwable, String]
  ): ZIO[R, Throwable, Unit] =
    for
      html <- download(url)
      info <- parseTea(html)
      _    <- Console.printLine(renderTeaInfo(info))
    yield ()

  def run =
    for
      args <- getArgs
      url  <- ZIO
                .fromOption(args.headOption)
                .orElseFail(IllegalArgumentException("Usage: scraper <url>"))
      _    <- program(url, fetch).provide(Client.default)
    yield ()
