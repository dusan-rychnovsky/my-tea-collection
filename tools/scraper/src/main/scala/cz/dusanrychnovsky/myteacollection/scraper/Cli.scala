package cz.dusanrychnovsky.myteacollection.scraper

import cz.dusanrychnovsky.myteacollection.scraper.render.OutputFormat

import zio.*
import zio.cli.{Args, BuiltInOption, CliConfig, Command, CommandDirective, Exists, HelpDoc, Options}
import zio.http.URL

import java.nio.file.Path

enum CliArgs:
  case SingleUrl(url: URL, format: OutputFormat)
  case Batch(rootDir: Path)

enum CliResult:
  case Parsed(args: CliArgs)
  case Help(usage: String)

case class ArgError(reason: String, usage: String) extends ScraperError:
  def message: String = s"$reason\n\n$usage"

private val formatOption =
  Options
    .enumeration("format")("json" -> OutputFormat.Json, "text" -> OutputFormat.Text)
    .optional
    .??("Output format (default: json).")

private val batchOption =
  Options
    .directory("batch", Exists.Yes)
    .optional
    .??("Scrape missing teas from this directory's index.txt into JSON files.")

private val urlArgument =
  Args
    .text("url")
    .atMost(1)
    .mapOrFail { urls =>
      urls.headOption match
        case None => Right(None)
        case Some(raw) if raw.startsWith("-") =>
          Left(HelpDoc.p(s"unknown option '$raw'"))
        case Some(raw) =>
          URL.decode(raw)
            .map(Option(_))
            .left.map(error => HelpDoc.p(s"invalid URL '$raw': ${error.getMessage}"))
    }
    .??("Product URL from a supported e-shop.")

private val command =
  Command("scraper", formatOption ++ batchOption, urlArgument)
    .withHelp("Scrape one product URL or missing teas from a collection index.")

private val usage =
  (HelpDoc.h1("Usage") + command.synopsis.helpDoc + command.helpDoc).toPlaintext()

def parseArgs(args: Chunk[String]): IO[ArgError, CliResult] =
  command
    .parse("scraper" :: args.toList, CliConfig.default.copy(caseSensitive = true))
    .mapError(error =>
      ArgError(
        if error.error.isEmpty then s"invalid arguments: ${error.validationErrorType}"
        else error.error.toPlaintext(),
        usage
      )
    )
    .flatMap {
      case CommandDirective.UserDefined(Nil, ((format, rootDir), url)) =>
        (rootDir, url) match
          case (Some(root), None) if root.toString.isEmpty =>
            ZIO.fail(ArgError("missing root directory for --batch", usage))
          case (Some(root), None) if format.isEmpty =>
            ZIO.succeed(CliResult.Parsed(CliArgs.Batch(root)))
          case (Some(_), None) =>
            ZIO.fail(ArgError(
              "--format cannot be used with --batch; batch output is always JSON",
              usage
            ))
          case (Some(_), Some(_)) =>
            ZIO.fail(ArgError("choose either --batch <rootDir> or <url>, not both", usage))
          case (None, Some(productUrl)) =>
            ZIO.succeed(
              CliResult.Parsed(CliArgs.SingleUrl(productUrl, format.getOrElse(OutputFormat.Json)))
            )
          case (None, None) =>
            ZIO.fail(ArgError("missing <url> or --batch <rootDir>", usage))
      case CommandDirective.UserDefined(leftover, _) =>
        ZIO.fail(ArgError(s"unexpected arguments: ${leftover.mkString(" ")}", usage))
      case CommandDirective.BuiltIn(BuiltInOption.ShowHelp(_, _)) =>
        ZIO.succeed(CliResult.Help(usage))
      case CommandDirective.BuiltIn(_) =>
        ZIO.fail(ArgError("unsupported built-in option; use --help for usage", usage))
    }
