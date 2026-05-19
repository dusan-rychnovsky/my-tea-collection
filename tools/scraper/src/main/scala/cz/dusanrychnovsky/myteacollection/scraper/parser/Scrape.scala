package cz.dusanrychnovsky.myteacollection.scraper.parser

import cz.dusanrychnovsky.myteacollection.scraper.ScraperError
import cz.dusanrychnovsky.myteacollection.scraper.domain.*

import zio.*
import zio.http.*

final case class UnsupportedVendorError(host: String) extends ScraperError:
  def message: String = s"no vendor registered for host: $host"

def scrape(url: URL): ZIO[Client, HttpError | ParseError | UnsupportedVendorError, TeaInfo] =
  url.host match
    case Some("meileaf.com")     => fetch(url).flatMap(parseMeileafTea(_, url))
    case Some("store.meetea.cz") => fetch(url).flatMap(parseMeeteaTea(_, url))
    case _ => ZIO.fail(UnsupportedVendorError(url.host.getOrElse("<unknown>")))
