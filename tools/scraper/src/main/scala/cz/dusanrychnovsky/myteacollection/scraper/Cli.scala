package cz.dusanrychnovsky.myteacollection.scraper

import cz.dusanrychnovsky.myteacollection.scraper.render.OutputFormat

import zio.*
import zio.cli.{Args, BuiltInOption, CliConfig, Command, CommandDirective, HelpDoc, Options}
import zio.http.URL

case class CliArgs(url: URL, format: OutputFormat)

enum CliResult:
  case Parsed(args: CliArgs)
  case Help(usage: String)

case class ArgError(reason: String, usage: String) extends ScraperError:
  def message: String = s"$reason\n\n$usage"

private val formatOption =
  Options
    .enumeration("format")("json" -> OutputFormat.Json, "text" -> OutputFormat.Text)
    .optional
    .map(_.getOrElse(OutputFormat.Json))
    .??("Output format (default: json).")

private val urlArgument =
  Args
    .text("url")
    .mapOrFail { raw =>
      if raw.startsWith("-") then Left(HelpDoc.p(s"unknown option '$raw'"))
      else URL.decode(raw).left.map(error => HelpDoc.p(s"invalid URL '$raw': ${error.getMessage}"))
    }
    .??("Product URL from a supported e-shop.")

private val command =
  Command("scraper", formatOption, urlArgument)
    .map { case (format, url) => CliArgs(url, format) }
    .withHelp("Download and render tea details from a supported e-shop.")

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
      case CommandDirective.UserDefined(Nil, cli) =>
        ZIO.succeed(CliResult.Parsed(cli))
      case CommandDirective.UserDefined(leftover, _) =>
        ZIO.fail(ArgError(s"unexpected arguments: ${leftover.mkString(" ")}", usage))
      case CommandDirective.BuiltIn(BuiltInOption.ShowHelp(_, _)) =>
        ZIO.succeed(CliResult.Help(usage))
      case CommandDirective.BuiltIn(_) =>
        ZIO.fail(ArgError("unsupported built-in option; use --help for usage", usage))
    }
