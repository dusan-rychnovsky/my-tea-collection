package cz.dusanrychnovsky.myteacollection.scraper.parser

import cz.dusanrychnovsky.myteacollection.scraper.ScraperError

import zio.*
import zio.http.*

final case class HttpError(cause: Throwable) extends ScraperError:
  def message: String = s"http failure: ${cause.getMessage}"

def fetch(url: URL): ZIO[Client, HttpError, String] =
  for
    response <- ZClient.batched(Request.get(url)).mapError(HttpError(_))
    body     <- response.body.asString.mapError(HttpError(_))
  yield body
