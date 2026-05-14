import zio.*
import zio.http.{Client, URL}

object Main extends ZIOAppDefault:

  def parseUrlArg(args: Chunk[String]): IO[Throwable, URL] =
    for
      raw <- ZIO
        .fromOption(args.headOption)
        .orElseFail(IllegalArgumentException("Usage: scraper <url>"))
      url <- ZIO.fromEither(URL.decode(raw))
    yield url

  def program(args: Chunk[String]): ZIO[Client, Throwable, Unit] =
    for
      url    <- parseUrlArg(args)
      vendor <- selectVendor(url)
      info   <- vendor.scrape(url)
      _      <- Console.printLine(renderTeaInfo(info))
    yield ()

  def run =
    for
      args <- getArgs
      _    <- program(args).provide(Client.default)
    yield ()
