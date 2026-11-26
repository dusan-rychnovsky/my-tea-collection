package cz.dusanrychnovsky.myteacollection.scraper

import cz.dusanrychnovsky.myteacollection.scraper.parse.*

import zio.*
import zio.http.Client
import zio.test.*

object MainSpec extends ZIOSpecDefault:

  private val rawUrl = "https://meileaf.com/tea/tea-jtic/"

  def spec = suite("Main")(
    suite("program")(
      test("fails with UnsupportedVendorError for an unknown host") {
        Main
          .program(Chunk("https://example.com/foo"))
          .provide(Client.default.orDie)
          .flip
          .map { err =>
            assertTrue(err == UnsupportedVendorError("example.com"))
          }
      },
      test("propagates CLI validation errors for empty args") {
        Main
          .program(Chunk.empty)
          .provide(Client.default.orDie)
          .flip
          .map { err =>
            assertTrue(err match
              case ArgError(reason, _) => reason.contains("<url>")
              case _                   => false
            )
          }
      },
      test("rejects invalid formats before scraping or printing") {
        for
          err <- Main
            .program(Chunk("--format=xml", rawUrl))
            .provide(Client.default.orDie)
            .flip
          output <- TestConsole.output
        yield assertTrue(
          err match
            case ArgError(reason, _) => reason.contains("json") && reason.contains("text")
            case _                   => false,
          output.isEmpty
        )
      },
      test("prints generated help without scraping") {
        for
          parsed <- parseArgs(Chunk("--help"))
          _      <- Main.program(Chunk("--help")).provide(Client.default.orDie)
          output <- TestConsole.output
        yield assertTrue(
          output.size == 1,
          parsed == CliResult.Help(output.mkString.stripSuffix("\n"))
        )
      },
      test("reports unsupported built-in commands explicitly") {
        for
          err    <- Main.program(Chunk("--wizard")).provide(Client.default.orDie).flip
          output <- TestConsole.output
        yield assertTrue(
          err.message.contains("unsupported built-in option"),
          output.isEmpty
        )
      }
    ),
    test("prints the parser's diagnostic and usage to stderr on invalid arguments") {
      for
        expected  <- parseArgs(Chunk.empty).flip
        err       <- Main.run.provide(ZLayer.succeed(ZIOAppArgs(Chunk.empty)), Scope.default).flip
        output    <- TestConsole.output
        outputErr <- TestConsole.outputErr
      yield assertTrue(
        err == expected,
        output.isEmpty,
        outputErr.mkString == s"${expected.message}\n"
      )
    }
  )
