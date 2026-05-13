import zio.*
import zio.http.{Client, URL}

object Main extends ZIOAppDefault:

  def program[R](
    url: URL,
    scrape: URL => ZIO[R, Throwable, TeaInfo]
  ): ZIO[R, Throwable, Unit] =
    for
      info <- scrape(url)
      _    <- Console.printLine(renderTeaInfo(info))
    yield ()

  def run =
    for
      args   <- getArgs
      raw    <- ZIO
                  .fromOption(args.headOption)
                  .orElseFail(IllegalArgumentException("Usage: scraper <url>"))
      url    <- ZIO.fromEither(URL.decode(raw))
      vendor <- selectVendor(url)
      _      <- program(url, vendor.scrape).provide(Client.default)
    yield ()
