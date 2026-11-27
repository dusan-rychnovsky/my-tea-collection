package cz.dusanrychnovsky.myteacollection.scraper

import cz.dusanrychnovsky.myteacollection.scraper.parse.*
import cz.dusanrychnovsky.myteacollection.scraper.render.*

import zio.*
import zio.http.Client

object Main extends ZIOAppDefault:

  def program(
    args: Chunk[String]
  ): ZIO[
    Client,
    ArgError | HttpError | ParseError | UnsupportedVendorError | IndexError | FileError | BatchError,
    Unit
  ] =
    parseArgs(args).flatMap {
      case CliResult.Parsed(CliArgs.SingleUrl(url, format)) =>
        for
          info <- scrape(url)
          _    <- Console.printLine(renderTeaInfo(info, format)).orDie
        yield ()
      case CliResult.Parsed(CliArgs.Batch(rootDir)) =>
        scrapeBatch(rootDir)
      case CliResult.Help(usage) =>
        Console.printLine(usage).orDie
    }

  def run =
    for
      args <- getArgs
      _ <- program(args).provide(Client.default.orDie).tapError { err =>
        Console.printLineError(err.message).orDie
      }
    yield ()
