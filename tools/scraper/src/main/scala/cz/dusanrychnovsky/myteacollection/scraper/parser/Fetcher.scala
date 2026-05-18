package cz.dusanrychnovsky.myteacollection.scraper.parser

import zio.*
import zio.http.*

final case class HttpError(cause: Throwable)

def fetch(url: URL): ZIO[Client, HttpError, String] =
  for
    response <- ZClient.batched(Request.get(url)).mapError(HttpError(_))
    body     <- response.body.asString.mapError(HttpError(_))
  yield body
