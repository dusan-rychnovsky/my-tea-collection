package cz.dusanrychnovsky.myteacollection.scraper

import zio.*
import zio.http.*
import zio.json.*
import zio.json.ast.Json
import zio.test.*

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path as FilePath}
import scala.jdk.CollectionConverters.*

object BatchModeSpec extends ZIOSpecDefault:

  private def url(slug: String): String = s"https://meileaf.com/tea/$slug/"

  private def page(title: String): String =
    s"""<html><body>
       |  <ol itemtype="https://schema.org/BreadcrumbList">
       |    <li><span itemprop="name">Teas</span></li>
       |    <li><span itemprop="name">White Tea</span></li>
       |    <li><span itemprop="name">$title</span></li>
       |  </ol>
       |  <h1 class="product-info__title">$title</h1>
       |  <h2 class="product-info__subtitle">Sample name</h2>
       |  <dl class="product-detail"></dl>
       |</body></html>""".stripMargin

  private def respond(
    rawUrl: String,
    html: String,
    requests: Ref[List[String]]
  ): URIO[TestClient, Unit] =
    TestClient.addRoute(
      RoutePattern(Method.GET, URL.decode(rawUrl).toOption.get.path) ->
        handler(requests.update(_ :+ rawUrl).as(Response(body = Body.fromString(html))))
    )

  private def temporaryRoot: ZIO[Scope, Throwable, FilePath] =
    ZIO.acquireRelease(ZIO.attemptBlocking(Files.createTempDirectory("scraper batch "))) { root =>
      ZIO.attemptBlocking {
        val stream = Files.walk(root)
        val paths =
          try stream.iterator().asScala.toList
          finally stream.close()
        paths.sortBy(_.getNameCount).reverse.foreach(path => Files.delete(path))
      }.orDie
    }

  private def write(path: FilePath, content: String): Task[Unit] =
    ZIO.attemptBlocking(Files.writeString(path, content, UTF_8)).unit

  private def read(path: FilePath): Task[String] =
    ZIO.attemptBlocking(Files.readString(path, UTF_8))

  private def rootEntries(root: FilePath): Task[Set[String]] =
    ZIO.attemptBlocking {
      val stream = Files.list(root)
      try stream.iterator().asScala.map(_.getFileName.toString).toSet
      finally stream.close()
    }

  def spec = suite("Batch filesystem and HTTP integration")(
    test(
      "scrapes every missing ID in index order, writes JSON, and leaves existing folders untouched"
    ) {
      ZIO.scoped {
        val firstUrl    = url("first")
        val secondUrl   = url("second")
        val secondTitle = "B\u00EDl\u00E1 Tea"
        for
          root <- temporaryRoot
          _    <- ZIO.attemptBlocking(Files.createDirectory(root.resolve("1")))
          _    <- write(root.resolve("1").resolve("info.json"), "existing data")
          _    <- ZIO.attemptBlocking(Files.createDirectory(root.resolve("03")))
          index =
            s"01 Existing tea https://example.com/first\n02 Indexed first $firstUrl\n\n03 Another existing tea https://example.com/third\n04 $secondTitle $secondUrl\n"
          _              <- write(root.resolve("index.txt"), index)
          requests       <- Ref.make(List.empty[String])
          _              <- respond(firstUrl, page("First Tea"), requests)
          _              <- respond(secondUrl, page(secondTitle), requests)
          _              <- Main.program(Chunk("--batch", root.toString))
          first          <- read(root.resolve("02").resolve("info.json"))
          second         <- read(root.resolve("04").resolve("info.json"))
          existing       <- read(root.resolve("1").resolve("info.json"))
          names          <- rootEntries(root)
          output         <- TestConsole.output
          warnings       <- TestConsole.outputErr
          _              <- Main.program(Chunk(s"--batch=$root"))
          requested      <- requests.get
          allOutput      <- TestConsole.output
          untouchedIndex <- read(root.resolve("index.txt"))
        yield assertTrue(
          requested == List(firstUrl, secondUrl),
          names == Set("index.txt", "1", "02", "03", "04"),
          existing == "existing data",
          untouchedIndex == index,
          first.fromJson[Map[String, Json]].exists(fields =>
            fields.get("title").contains(Json.Str("First Tea")) &&
              fields.get("url").contains(Json.Str(firstUrl)) &&
              fields.get("types").contains(Json.Arr(Json.Str("White Tea"))) &&
              fields.get("inStock").contains(Json.Bool(true)) &&
              fields.get("tags").contains(Json.Arr()) &&
              fields.get("elevation").contains(Json.Str("N/A"))
          ),
          second.fromJson[Map[String, Json]].exists(_.get("title").contains(Json.Str(secondTitle))),
          first.endsWith("\n") && !first.contains("\r"),
          second.endsWith("\n") && !second.contains("\r"),
          output.last == "Batch finished: 2 scraped, 2 skipped, 0 failed.\n",
          allOutput.last == "Batch finished: 0 scraped, 4 skipped, 0 failed.\n",
          warnings.size == 1,
          warnings.mkString.contains("Indexed first"),
          warnings.mkString.contains("First Tea")
        )
      }
    },
    test(
      "continues after vendor, parsing and write failures, and failed teas can be retried"
    ) {
      ZIO.scoped {
        val brokenUrl    = url("broken")
        val firstGoodUrl = url("good-one")
        val collisionUrl = url("collision")
        val lastGoodUrl  = url("good-two")
        val failedIds    = List("01", "03", "06")
        for
          root <- temporaryRoot
          _ <- write(
            root.resolve("index.txt"),
            s"01 Broken $brokenUrl\n02 Good One $firstGoodUrl\n03 Unknown https://example.com/tea\n06 Collision $collisionUrl\n07 Good Two $lastGoodUrl\n"
          )
          _         <- write(root.resolve("06"), "do not replace")
          requests  <- Ref.make(List.empty[String])
          _         <- respond(brokenUrl, "<html></html>", requests)
          _         <- respond(firstGoodUrl, page("Good One"), requests)
          _         <- respond(collisionUrl, page("Collision"), requests)
          _         <- respond(lastGoodUrl, page("Good Two"), requests)
          err       <- Main.program(Chunk("--batch", root.toString)).flip
          names     <- rootEntries(root)
          first     <- read(root.resolve("02").resolve("info.json"))
          last      <- read(root.resolve("07").resolve("info.json"))
          collision <- read(root.resolve("06"))
          requested <- requests.get
          errors    <- TestConsole.outputErr
          output    <- TestConsole.output
          _         <- ZIO.attemptBlocking(Files.delete(root.resolve("06")))
          retryIndex = failedIds.map(id => s"$id Retry $id ${url(s"retry-$id")}").mkString("\n")
          _ <- write(
            root.resolve("index.txt"),
            s"$retryIndex\n02 Existing $firstGoodUrl\n07 Existing $lastGoodUrl\n"
          )
          _ <- ZIO.foreachDiscard(failedIds) { id =>
            respond(url(s"retry-$id"), page(s"Retry $id"), requests)
          }
          _            <- Main.program(Chunk("--batch", root.toString))
          retriedNames <- rootEntries(root)
          firstAfter   <- read(root.resolve("02").resolve("info.json"))
          lastAfter    <- read(root.resolve("07").resolve("info.json"))
        yield assertTrue(
          err == BatchError(failedIds),
          names == Set("index.txt", "02", "06", "07"),
          collision == "do not replace",
          requested == List(brokenUrl, firstGoodUrl, collisionUrl, lastGoodUrl),
          failedIds.forall(id => errors.mkString.contains(s"tea $id:")),
          errors.mkString.contains("product-info__title"),
          errors.mkString.contains("no vendor registered for host: example.com"),
          errors.mkString.contains("file failure"),
          output.last == "Batch finished: 2 scraped, 0 skipped, 3 failed.\n",
          retriedNames == Set("index.txt", "01", "02", "03", "06", "07"),
          firstAfter == first,
          lastAfter == last
        )
      }
    },
    test("rejects malformed or duplicate numerical IDs before scraping or creating folders") {
      ZIO.scoped {
        val cases = List(
          (s"\nnot-an-id Tea ${url("first")}\n", 2, "positive numerical id"),
          (s"00 Tea ${url("first")}\n", 1, "positive numerical id"),
          ("171\n", 1, "positive numerical id"),
          (s"01 First ${url("first")}\n1 Duplicate ${url("second")}\n", 2, "duplicate tea id")
        )
        for
          root <- temporaryRoot
          results <- ZIO.foreach(cases) { (content, expectedLine, reason) =>
            for
              _     <- write(root.resolve("index.txt"), content)
              err   <- Main.program(Chunk("--batch", root.toString)).flip
              names <- rootEntries(root)
            yield assertTrue(
              err match
                case IndexError(path, line, diagnostic) =>
                  path == root.resolve("index.txt") && line == expectedLine && diagnostic.contains(
                    reason
                  )
                case _ => false,
              names == Set("index.txt")
            )
          }
          output <- TestConsole.output
        yield results.reduce(_ && _) && assertTrue(output.isEmpty)
      }
    },
    test("canonicalizes HTTP(S) schemes without changing path or query casing") {
      ZIO.scoped {
        val cases = List(
          "HTTP"  -> "http",
          "HtTp"  -> "http",
          "HTTPS" -> "https",
          "hTtPs" -> "https"
        ).zipWithIndex.map { case ((inputScheme, expectedScheme), offset) =>
          val id   = f"${offset + 1}%02d"
          val host = if offset == 3 then "meileaf.com:8443" else "meileaf.com"
          val rest = s"$host/tea/Case-Sensitive-$id/?Token=AbC$id"
          (id, s"$inputScheme://$rest", s"$expectedScheme://$rest")
        }
        for
          root     <- temporaryRoot
          requests <- Ref.make(List.empty[String])
          _ <- ZIO.foreachDiscard(cases) { case (id, _, expectedUrl) =>
            respond(expectedUrl, page(s"Tea $id"), requests)
          }
          index = cases.map { case (id, rawUrl, _) => s"$id Tea $id $rawUrl" }.mkString("\n") + "\n"
          _ <- write(root.resolve("index.txt"), index)
          _ <- Main.program(Chunk("--batch", root.toString))
          results <- ZIO.foreach(cases) { case (id, _, expectedUrl) =>
            read(root.resolve(id).resolve("info.json")).map { json =>
              assertTrue(
                json.fromJson[Map[String, Json]].exists(
                  _.get("url").contains(Json.Str(expectedUrl))
                )
              )
            }
          }
          requested <- requests.get
        yield results.reduce(_ && _) && assertTrue(requested == cases.map(_._3))
      }
    },
    test("validates mandatory fields and URLs even for existing IDs before scraping") {
      ZIO.scoped {
        val goodUrl = url("existing-validation-good")
        val invalidRows = List(
          "02 Existing",
          s"02 ${url("missing-title")}",
          "02 Existing tea without-url",
          "02 Invalid https://[",
          "02 Invalid https:///tea",
          "02 Invalid ftp://example.com/tea"
        )
        for
          root     <- temporaryRoot
          _        <- ZIO.attemptBlocking(Files.createDirectory(root.resolve("2")))
          requests <- Ref.make(List.empty[String])
          _        <- respond(goodUrl, page("Good"), requests)
          results <- ZIO.foreach(invalidRows) { row =>
            for
              _     <- write(root.resolve("index.txt"), s"01 Good $goodUrl\n$row\n")
              err   <- Main.program(Chunk("--batch", root.toString)).flip
              names <- rootEntries(root)
            yield assertTrue(
              err match
                case IndexError(path, line, reason) =>
                  path == root.resolve("index.txt") && line == 2 && reason.nonEmpty
                case _ => false,
              names == Set("index.txt", "2")
            )
          }
          requested <- requests.get
          output    <- TestConsole.output
          errors    <- TestConsole.outputErr
        yield results.reduce(_ && _) && assertTrue(
          requested.isEmpty,
          output.isEmpty,
          errors.isEmpty
        )
      }
    },
    test("rejects a malformed pending URL before scraping earlier valid records") {
      ZIO.scoped {
        val goodUrl = url("pending-validation-good")
        for
          root      <- temporaryRoot
          requests  <- Ref.make(List.empty[String])
          _         <- respond(goodUrl, page("Good"), requests)
          _         <- write(root.resolve("index.txt"), s"01 Good $goodUrl\n02 Bad https://[\n")
          err       <- Main.program(Chunk("--batch", root.toString)).flip
          requested <- requests.get
          names     <- rootEntries(root)
          output    <- TestConsole.output
        yield assertTrue(
          err match
            case IndexError(path, line, reason) =>
              path == root.resolve("index.txt") && line == 2 && reason.contains("invalid URL")
            case _ => false,
          requested.isEmpty,
          names == Set("index.txt"),
          output.isEmpty
        )
      }
    },
    test("cleanup failures preserve publication errors and do not abort the batch") {
      ZIO.scoped {
        val collisionUrl = url("cleanup-collision")
        val goodUrl      = url("cleanup-good")
        for
          root <- temporaryRoot
          _ <- write(
            root.resolve("index.txt"),
            s"01 Collision $collisionUrl\n02 Good $goodUrl\n"
          )
          _               <- write(root.resolve("01"), "existing file")
          requests        <- Ref.make(List.empty[String])
          _               <- respond(collisionUrl, page("Collision"), requests)
          _               <- respond(goodUrl, page("Good"), requests)
          cleanupAttempts <- Ref.make(List.empty[FilePath])
          err <- scrapeBatch(
            root,
            staging =>
              cleanupAttempts.update(_ :+ staging) *>
                ZIO.fail(FileError(staging, "simulated cleanup failure"))
          ).flip
          staging   <- cleanupAttempts.get
          unchanged <- read(root.resolve("01"))
          good      <- read(root.resolve("02").resolve("info.json"))
          requested <- requests.get
          output    <- TestConsole.output
          errors    <- TestConsole.outputErr
          _         <- ZIO.attemptBlocking(Files.delete(root.resolve("01")))
          _         <- Main.program(Chunk("--batch", root.toString))
          retried   <- read(root.resolve("01").resolve("info.json"))
          goodAfter <- read(root.resolve("02").resolve("info.json"))
        yield assertTrue(
          err == BatchError(List("01")),
          unchanged == "existing file",
          requested == List(collisionUrl, goodUrl),
          staging.size == 1,
          errors.size == 2,
          errors.mkString.contains("staging cleanup failed"),
          errors.mkString.contains("simulated cleanup failure"),
          errors.mkString.contains(s"tea 01: file failure at '${root.resolve("01")}'"),
          output.last == "Batch finished: 1 scraped, 0 skipped, 1 failed.\n",
          retried.fromJson[Map[String, Json]].exists(
            _.get("title").contains(Json.Str("Collision"))
          ),
          goodAfter == good
        )
      }
    },
    test("reports a missing index as a typed file error") {
      ZIO.scoped {
        for
          root  <- temporaryRoot
          err   <- Main.program(Chunk("--batch", root.toString)).flip
          names <- rootEntries(root)
        yield assertTrue(
          err match
            case FileError(path, reason) => path == root.resolve("index.txt") && reason.nonEmpty
            case _                       => false,
          names.isEmpty
        )
      }
    },
    test("the app reports all failed teas and the final batch failure to stderr") {
      ZIO.scoped {
        val expected = BatchError(List("01", "02"))
        for
          root <- temporaryRoot
          _ <- write(
            root.resolve("index.txt"),
            "01 Unsupported https://example.com/tea\n02 Unsupported too https://example.net/tea\n"
          )
          err <- Main.run
            .provide(
              ZLayer.succeed(ZIOAppArgs(Chunk("--batch", root.toString))),
              Scope.default
            )
            .flip
          output <- TestConsole.output
          errors <- TestConsole.outputErr
        yield assertTrue(
          err == expected,
          output.last == "Batch finished: 0 scraped, 0 skipped, 2 failed.\n",
          errors.mkString.contains("tea 01:"),
          errors.mkString.contains("tea 02:"),
          errors.mkString.endsWith(s"${expected.message}\n")
        )
      }
    }
  ).provide(TestClient.layer)
