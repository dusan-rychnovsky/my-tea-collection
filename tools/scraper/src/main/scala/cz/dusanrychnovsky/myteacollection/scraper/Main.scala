package cz.dusanrychnovsky.myteacollection.scraper

import cz.dusanrychnovsky.myteacollection.scraper.domain.*
import cz.dusanrychnovsky.myteacollection.scraper.parser.*

import zio.*
import zio.http.{Client, URL}

enum ArgError extends ScraperError:
  case MissingArg(usage: String)
  case BadUrl(raw: String, reason: String)

  def message: String = this match
    case ArgError.MissingArg(u)  => u
    case ArgError.BadUrl(raw, r) => s"invalid URL '$raw': $r"

object Main extends ZIOAppDefault:

  def parseUrlArg(args: Chunk[String]): IO[ArgError, URL] =
    for
      raw <- ZIO
        .fromOption(args.headOption)
        .orElseFail(ArgError.MissingArg("Usage: scraper <url>"))
      url <- ZIO.fromEither(URL.decode(raw)).mapError(e => ArgError.BadUrl(raw, e.getMessage))
    yield url

  def program(
    args: Chunk[String]
  ): ZIO[Client, ArgError | HttpError | ParseError | UnsupportedVendorError, Unit] =
    for
      url  <- parseUrlArg(args)
      info <- scrape(url)
      _    <- Console.printLine(renderTeaInfo(info)).orDie
    yield ()

  def run =
    for
      args <- getArgs
      _ <- program(args).provide(Client.default.orDie).tapError { err =>
        Console.printLineError(err.message).orDie
      }
    yield ()
