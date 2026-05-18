package cz.dusanrychnovsky.myteacollection.scraper.parser

import zio.*
import zio.http.*

def fetch(url: String): ZIO[Client, Throwable, String] =
  for
    parsed   <- ZIO.fromEither(URL.decode(url))
    response <- ZClient.batched(Request.get(parsed))
    body     <- response.body.asString
  yield body
