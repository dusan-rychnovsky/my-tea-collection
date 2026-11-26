package cz.dusanrychnovsky.myteacollection.scraper

import cz.dusanrychnovsky.myteacollection.scraper.render.OutputFormat

import zio.*
import zio.http.URL
import zio.test.*

object CliSpec extends ZIOSpecDefault:

  private val rawUrl    = "https://meileaf.com/tea/tea-jtic/"
  private val sampleUrl = URL.decode(rawUrl).toOption.get

  def spec = suite("parseArgs")(
    test("decodes the URL and defaults to JSON") {
      parseArgs(Chunk(rawUrl)).map { parsed =>
        assertTrue(parsed == CliResult.Parsed(CliArgs(sampleUrl, OutputFormat.Json)))
      }
    },
    test("accepts both option syntaxes before the URL") {
      ZIO.foreach(
        List(
          (Chunk("--format=json", rawUrl), OutputFormat.Json),
          (Chunk("--format", "json", rawUrl), OutputFormat.Json),
          (Chunk("--format=text", rawUrl), OutputFormat.Text),
          (Chunk("--format", "text", rawUrl), OutputFormat.Text)
        )
      ) { (input, expectedFormat) =>
        parseArgs(input).map { parsed =>
          assertTrue(parsed == CliResult.Parsed(CliArgs(sampleUrl, expectedFormat)))
        }
      }.map(results => results.reduce(_ && _))
    },
    test("rejects format options after the URL") {
      ZIO.foreach(
        List(Chunk(rawUrl, "--format=json"), Chunk(rawUrl, "--format", "text"))
      ) { input =>
        parseArgs(input).flip.map { err =>
          assertTrue(
            err.reason.contains("unexpected arguments"),
            err.reason.contains("--format")
          )
        }
      }.map(results => results.reduce(_ && _))
    },
    test("reports the missing URL when args is empty") {
      parseArgs(Chunk.empty).flip.map { err =>
        assertTrue(err.reason.contains("<url>"))
      }
    },
    test("reports the missing URL when only the format is supplied") {
      parseArgs(Chunk("--format=json")).flip.map { err =>
        assertTrue(err.reason.contains("<url>"))
      }
    },
    test("reports an invalid URL") {
      parseArgs(Chunk("--format=text", "not a url")).flip.map { err =>
        assertTrue(err.reason.contains("invalid URL 'not a url'"))
      }
    },
    test("rejects unsupported formats instead of falling back") {
      parseArgs(Chunk("--format=xml", rawUrl)).flip.map { err =>
        assertTrue(err.reason.contains("json"), err.reason.contains("text"))
      }
    },
    test("rejects a missing or empty format value") {
      ZIO.foreach(
        List(
          Chunk("--format"),
          Chunk("--format=", rawUrl),
          Chunk("--format", "", rawUrl),
          Chunk("--format", "--other", rawUrl)
        )
      ) { input =>
        parseArgs(input).flip.map { err =>
          assertTrue(err.reason.nonEmpty)
        }
      }.map(results => results.reduce(_ && _))
    },
    test("rejects duplicate format options") {
      ZIO.foreach(
        List(
          Chunk("--format=json", "--format", "text", rawUrl),
          Chunk("--format=json", "--format=text", rawUrl)
        )
      ) { input =>
        parseArgs(input).flip.map { err =>
          assertTrue(err.reason.nonEmpty)
        }
      }.map(results => results.reduce(_ && _))
    },
    test("rejects unknown options and extra positional arguments") {
      for
        unknown <- parseArgs(Chunk(rawUrl, "--other")).flip
        before  <- parseArgs(Chunk("--other", rawUrl)).flip
        extra   <- parseArgs(Chunk(rawUrl, "https://example.com/x")).flip
      yield assertTrue(
        unknown.reason.contains("--other"),
        before.reason.contains("--other"),
        extra.reason.contains("https://example.com/x")
      )
    },
    test("rejects case-variant flags rather than silently selecting JSON") {
      ZIO.foreach(
        List(Chunk("--FORMAT=text", rawUrl), Chunk(rawUrl, "--FORMAT=text"))
      ) { input =>
        parseArgs(input).flip.map { err =>
          assertTrue(err.reason.contains("--FORMAT"))
        }
      }.map(results => results.reduce(_ && _))
    },
    test("returns formatted usage for both help options without requiring a URL") {
      ZIO.foreach(List(Chunk("--help"), Chunk("-h"))) { input =>
        parseArgs(input).map { parsed =>
          assertTrue(parsed match
            case CliResult.Help(usage) =>
              usage.toLowerCase.contains("usage") &&
              usage.contains("scraper") &&
              usage.contains("--format") &&
              usage.contains("json") &&
              usage.contains("text") &&
              usage.contains("<url>")
            case _ => false
          )
        }
      }.map(results => results.reduce(_ && _))
    },
    test("includes the same generated usage in all argument errors without printing") {
      val invalidInputs = List(
        Chunk.empty,
        Chunk("--format=xml", rawUrl),
        Chunk("--format"),
        Chunk("--format=json", "--format=text", rawUrl),
        Chunk("--format=text", "not a url"),
        Chunk(rawUrl, "--other"),
        Chunk("--wizard")
      )
      for
        _         <- parseArgs(Chunk(rawUrl))
        help      <- parseArgs(Chunk("--help"))
        errors    <- ZIO.foreach(invalidInputs)(input => parseArgs(input).flip)
        output    <- TestConsole.output
        outputErr <- TestConsole.outputErr
      yield assertTrue(
        errors.forall(error =>
          help == CliResult.Help(error.usage) &&
            error.reason.nonEmpty &&
            error.message.contains(error.usage)
        ),
        output.isEmpty,
        outputErr.isEmpty
      )
    }
  )
