package cz.dusanrychnovsky.myteacollection.scraper

import cz.dusanrychnovsky.myteacollection.scraper.domain.*
import cz.dusanrychnovsky.myteacollection.scraper.parse.*
import cz.dusanrychnovsky.myteacollection.scraper.render.*

import zio.*
import zio.http.{Client, Scheme, URL}

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path, StandardOpenOption}
import java.nio.file.attribute.BasicFileAttributes
import scala.jdk.CollectionConverters.*

final case class IndexError(path: Path, line: Int, reason: String) extends ScraperError:
  def message: String = s"$path:$line: $reason"

final case class FileError(path: Path, reason: String) extends ScraperError:
  def message: String = s"file failure at '$path': $reason"

final case class BatchError(failedIds: List[String]) extends ScraperError:
  def message: String =
    s"batch failed for ${failedIds.size} tea(s): ${failedIds.mkString(", ")}"

private case class IndexEntry(id: String, title: Title, url: URL)

private def parseIndexUrl(rawUrl: String, path: Path, line: Int): IO[IndexError, URL] =
  for
    decodedUrl <- ZIO
      .fromEither(URL.decode(rawUrl))
      .mapError(error => IndexError(path, line, s"invalid URL '$rawUrl': ${error.getMessage}"))
    // Known zio-http 3.0.1 bug: mixed-case HTTP(S) decodes as Scheme.Custom.
    // Explicitly normalize the scheme to preserve TLS and default-port handling:
    // https://github.com/zio/zio-http/blob/v3.0.1/zio-http/shared/src/main/scala/zio/http/Scheme.scala#L76-L80
    scheme <- ZIO
      .fromOption(
        decodedUrl.scheme
          .filter(_ => decodedUrl.isAbsolute)
          .map(_.encode)
          .collect {
            case value if value.equalsIgnoreCase("http")  => Scheme.HTTP
            case value if value.equalsIgnoreCase("https") => Scheme.HTTPS
          }
      )
      .orElseFail(IndexError(
        path,
        line,
        s"invalid URL '$rawUrl': expected an absolute HTTP(S) URL"
      ))
    url = decodedUrl.scheme(scheme)
    _ <- ZIO
      .fail(IndexError(path, line, s"invalid URL '$rawUrl': missing host"))
      .unless(url.host.exists(_.nonEmpty))
  yield url

private def parseIndex(content: String, path: Path): IO[IndexError, List[IndexEntry]] =
  val lines = content.linesIterator.zipWithIndex.toList
  ZIO.foldLeft(lines)((Set.empty[BigInt], List.empty[IndexEntry])) { (state, record) =>
    val (seen, entries) = state
    val (line, offset)  = record
    if line.trim.isEmpty then ZIO.succeed(state)
    else
      line.trim.split("\\s+").toList match
        case id :: rest if rest.size >= 2 && id.matches("[0-9]+") && BigInt(id) > 0 =>
          val numericId = BigInt(id)
          val title     = Title(rest.dropRight(1).mkString(" "))
          val rawUrl    = rest.last
          if seen.contains(numericId) then
            ZIO.fail(IndexError(path, offset + 1, s"duplicate tea id '$id'"))
          else
            parseIndexUrl(rawUrl, path, offset + 1).map { url =>
              (seen + numericId, IndexEntry(id, title, url) :: entries)
            }
        case _ =>
          ZIO.fail(IndexError(
            path,
            offset + 1,
            "expected <ID> <title> <URL>: a positive numerical id, non-empty title, and absolute HTTP(S) URL are mandatory"
          ))
  }.map(_._2.reverse)

private def existingTeaIds(rootDir: Path): IO[FileError, Set[BigInt]] =
  ZIO.scoped {
    ZIO
      .acquireRelease(ZIO.attemptBlocking(Files.newDirectoryStream(rootDir)))(stream =>
        ZIO.attemptBlocking(stream.close()).orDie
      )
      .flatMap { stream =>
        ZIO.attemptBlocking {
          stream.iterator().asScala
            .filter(path => path.getFileName.toString.matches("[0-9]+"))
            .filter(path => Files.readAttributes(path, classOf[BasicFileAttributes]).isDirectory)
            .map(path => BigInt(path.getFileName.toString))
            .toSet
        }
      }
  }.mapError(error => FileError(rootDir, error.getMessage))

private def removeStaging(staging: Path): IO[FileError, Unit] =
  ZIO
    .attemptBlocking {
      Files.deleteIfExists(staging.resolve("info.json"))
      Files.deleteIfExists(staging)
    }
    .mapError(error => FileError(staging, error.getMessage))
    .unit

private def publishTea(
  rootDir: Path,
  id: String,
  info: TeaInfo,
  cleanup: Path => IO[FileError, Unit]
): IO[FileError, Unit] =
  val destination = rootDir.resolve(id)
  ZIO.acquireReleaseExitWith(
    ZIO
      .attemptBlocking(Files.createTempDirectory(rootDir, ".scraper-"))
      .mapError(error => FileError(destination, error.getMessage))
  ) { (staging, exit) =>
    exit match
      case Exit.Success(_) => ZIO.unit
      case Exit.Failure(_) =>
        // Report cleanup separately so it cannot replace the publication failure.
        cleanup(staging).catchAll { error =>
          Console.printLineError(s"tea $id: staging cleanup failed: ${error.message}").orDie
        }
  } { staging =>
    ZIO
      .attemptBlocking {
        Files.writeString(
          staging.resolve("info.json"),
          renderTeaInfo(info, OutputFormat.Json) + "\n",
          UTF_8,
          StandardOpenOption.CREATE_NEW
        )
        Files.move(staging, destination)
        ()
      }
      .mapError(error => FileError(destination, error.getMessage))
  }

private def scrapeEntry(
  rootDir: Path,
  entry: IndexEntry,
  cleanup: Path => IO[FileError, Unit]
): ZIO[Client, HttpError | ParseError | UnsupportedVendorError | FileError, Unit] =
  for
    info <- scrape(entry.url)
    _ <- Console
      .printLineError(
        s"warning: tea ${entry.id}: index title '${entry.title.value}' differs from scraped title '${info.title.value}'"
      )
      .orDie
      .when(entry.title != info.title)
    _ <- publishTea(rootDir, entry.id, info, cleanup)
    _ <- Console.printLine(s"Scraped tea ${entry.id}: ${info.title.value}").orDie
  yield ()

def scrapeBatch(rootDir: Path): ZIO[Client, IndexError | FileError | BatchError, Unit] =
  scrapeBatch(rootDir, removeStaging)

private[scraper] def scrapeBatch(
  rootDir: Path,
  cleanup: Path => IO[FileError, Unit]
): ZIO[Client, IndexError | FileError | BatchError, Unit] =
  val indexPath = rootDir.resolve("index.txt")
  for
    content <- ZIO
      .attemptBlocking(Files.readString(indexPath, UTF_8))
      .mapError(error => FileError(indexPath, error.getMessage))
    entries  <- parseIndex(content, indexPath)
    existing <- existingTeaIds(rootDir)
    pending = entries.filterNot(entry => existing.contains(BigInt(entry.id)))
    results <- ZIO.foreach(pending) { entry =>
      scrapeEntry(rootDir, entry, cleanup)
        .tapError(error => Console.printLineError(s"tea ${entry.id}: ${error.message}").orDie)
        .either
    }
    failedIds = pending.zip(results).collect { case (entry, Left(_)) => entry.id }
    _ <- Console
      .printLine(
        s"Batch finished: ${results.count(_.isRight)} scraped, ${entries.size - pending.size} skipped, ${failedIds.size} failed."
      )
      .orDie
    _ <- ZIO.fail(BatchError(failedIds)).when(failedIds.nonEmpty)
  yield ()
