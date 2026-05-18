package cz.dusanrychnovsky.myteacollection.scraper.parser

import cz.dusanrychnovsky.myteacollection.scraper.domain.*

import zio.*
import zio.http.*

final class UnsupportedVendor(host: String)
  extends RuntimeException(s"no vendor registered for host: $host")

def scrape(url: URL): ZIO[Client, Throwable, TeaInfo] =
  url.host match
    case Some("meileaf.com")     => fetch(url.encode).flatMap(parseMeileafTea(_, url))
    case Some("store.meetea.cz") => fetch(url.encode).flatMap(parseMeeteaTea(_, url))
    case _                       => ZIO.fail(UnsupportedVendor(url.host.getOrElse("<unknown>")))
